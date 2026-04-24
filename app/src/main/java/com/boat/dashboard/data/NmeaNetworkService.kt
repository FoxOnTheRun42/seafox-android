package com.seafox.nmea_dashboard.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import android.util.Log
import android.util.Base64
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.BindException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Locale
import kotlin.math.abs
import java.util.concurrent.atomic.AtomicLong

data class NmeaUpdate(
    val numericValues: Map<String, Float>,
    val textValues: Map<String, String> = emptyMap(),
)

class NmeaNetworkService {
    @Volatile private var nmeaRouterHost: String = ""
    @Volatile private var nmeaRouterProtocol: NmeaRouterProtocol = NmeaRouterProtocol.TCP
    private var tcpListenJob: Job? = null
    private var tcpSocket: Socket? = null

    val updates: MutableSharedFlow<NmeaUpdate> get() = Companion.updates
    val aisUpdates: MutableSharedFlow<NmeaUpdate> get() = Companion.aisUpdates
    private val logTag = Companion.logTag
    private val compatListenerPorts = Companion.compatListenerPorts

    private companion object {
        private const val logTag = "NmeaNetworkService"
        private val packetLogCounter = AtomicLong(0L)
        private val n2kDebugLogCounter = AtomicLong(0L)
        private val socketScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val listenJobs = linkedMapOf<Int, Job>()
        private val listenSockets = linkedMapOf<Int, DatagramSocket>()
        private val updates = MutableSharedFlow<NmeaUpdate>(
            extraBufferCapacity = 1024,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        private val aisUpdates = MutableSharedFlow<NmeaUpdate>(
            extraBufferCapacity = 512,
            onBufferOverflow = BufferOverflow.SUSPEND
        )
        private val compatListenerPorts = linkedSetOf(DEFAULT_NMEA_ROUTER_PORT)
    }

    private data class Ais0183FragmentState(
        val expectedParts: Int,
        val startedAtMs: Long,
        var lastUpdatedMs: Long,
        val channel: String,
        val sequenceId: String,
        val explicitSequence: Boolean,
        val payloadParts: Array<String?>,
        var fillBits: Int,
    )

    private val ais0183Fragments = mutableMapOf<String, Ais0183FragmentState>()
    private val ais0183FragmentsLock = Any()
    private val ais0183FragmentTtlMs = 4000L
    private var ais0183FragmentSequence = 0L

    fun start(port: Int = DEFAULT_NMEA_ROUTER_PORT) {
        val normalizedPort = port.coerceIn(1, 65535)
        if (nmeaRouterProtocol == NmeaRouterProtocol.TCP && tcpListenJob?.isActive == true) return

        val udpRunning = when {
            nmeaRouterProtocol == NmeaRouterProtocol.UDP && listenJobs.isNotEmpty() -> {
                val ports = resolveListenerPorts(normalizedPort)
                listenJobs.keys == ports.toSet()
            }
            else -> false
        }
        if (udpRunning) return

        stop()

        when (nmeaRouterProtocol) {
            NmeaRouterProtocol.UDP -> {
                val ports = resolveListenerPorts(normalizedPort)
                Log.d(logTag, "NMEA UDP service start requested for ports=$ports")
                ports.forEach { listenPort ->
                    val job = socketScope.launch {
                        listenOnPort(listenPort)
                    }
                    listenJobs[listenPort] = job
                }
            }
            NmeaRouterProtocol.TCP -> {
                val host = nmeaRouterHost.ifBlank { DEFAULT_NMEA_ROUTER_HOST }
                Log.d(logTag, "NMEA TCP service start requested for $host:$normalizedPort")
                tcpListenJob = socketScope.launch {
                    listenOnTcp(host, normalizedPort)
                }
            }
        }
    }

    fun restart(port: Int) {
        Log.d(logTag, "NMEA service restart requested for port=$port")
        stop()
        start(port)
    }

    fun stop() {
        Log.d(logTag, "NMEA service stop requested")
        listenJobs.values.forEach { it.cancel() }
        listenJobs.clear()
        listenSockets.values.forEach { it.close() }
        listenSockets.clear()
        tcpListenJob?.cancel()
        tcpListenJob = null
        tcpSocket?.close()
        tcpSocket = null
    }

    fun setRouterHost(host: String) {
        val normalizedHost = normalizeHost(host)
        nmeaRouterHost = if (normalizedHost.isBlank() || normalizedHost == DEFAULT_NMEA_ROUTER_HOST) {
            ""
        } else {
            normalizedHost
        }
    }

    fun setRouterProtocol(protocol: NmeaRouterProtocol) {
        nmeaRouterProtocol = protocol
    }

    fun sendCommandJson(payload: String, port: Int, host: String = "255.255.255.255") {
        socketScope.launch {
            sendUdp(payload, host, port)
        }
    }

    fun sendUdpText(payload: String, host: String, port: Int) {
        socketScope.launch {
            sendUdp(payload, host, port)
        }
    }

    suspend fun sendUdpTextWithResult(payload: String, host: String, port: Int): String? {
        val normalizedHost = normalizeHost(host)
        if (normalizedHost.isBlank()) {
            return "Kein gültiger NMEA-Router Host konfiguriert."
        }
        if (port !in 1..65535) {
            return "Ungültiger UDP-Port: $port."
        }
        return try {
            sendUdp(payload, normalizedHost, port, suppressErrors = false)
            null
        } catch (e: Exception) {
            when (e) {
                is java.net.UnknownHostException -> "Unbekannter Host: $normalizedHost."
                is java.net.SocketException -> "UDP-Socket-Fehler: ${e.message ?: "nicht spezifiziert"}."
                is SecurityException -> "Kein Zugriff für UDP-Sendungen: ${e.message ?: "nicht spezifiziert"}."
                else -> "MMSI konnte nicht übertragen werden: ${e.message ?: e::class.java.simpleName}."
            }.also {
                Log.w(logTag, "sendUdpTextWithResult failed host=$normalizedHost port=$port error=$it")
            }
        }
    }

    fun sendHttpJson(
        method: String,
        url: String,
        payload: String? = null,
        headers: Map<String, String> = mapOf("Content-Type" to "application/json")
    ) {
        socketScope.launch {
            sendHttp(method, url, payload, headers)
        }
    }

    private fun resolveListenerPorts(port: Int): List<Int> {
        val resolved = linkedSetOf<Int>()
        if (port in 1..65535) {
            resolved.add(port)
        }
        if (DEFAULT_NMEA_ROUTER_PORT in 1..65535) {
            resolved.add(DEFAULT_NMEA_ROUTER_PORT)
        }
        resolved.addAll(compatListenerPorts.filter { it in 1..65535 })
        return resolved.toList()
    }

    private suspend fun listenOnPort(port: Int) {
        var socket: DatagramSocket? = null
        try {
            Log.d(logTag, "Starting NMEA UDP listener on port $port")
            socket = DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(InetAddress.getByName("0.0.0.0"), port))
            }
            socket.soTimeout = 3000
            val buffer = ByteArray(16384)
            val packet = DatagramPacket(buffer, buffer.size)
            listenSockets[port] = socket
            var hostFilterEnabled = true
            var hostFilterTarget = ""
            var hostFilterDisabledLogged = false
            while (true) {
                try {
                    socket.receive(packet)
                    val expectedHost = nmeaRouterHost
                    if (expectedHost != hostFilterTarget) {
                        hostFilterEnabled = expectedHost.isNotBlank()
                        hostFilterTarget = expectedHost
                        hostFilterDisabledLogged = false
                    }
                    if (hostFilterEnabled && expectedHost.isNotBlank()) {
                        val sourceAddress = packet.address
                        val sourceHost = sourceAddress.hostAddress
                        val sourceName = sourceAddress.hostName
                        if (sourceHost != expectedHost && sourceName != expectedHost) {
                            if (!hostFilterDisabledLogged) {
                                hostFilterEnabled = false
                                hostFilterDisabledLogged = true
                                Log.w(
                                    logTag,
                                    "UDP host filter relaxed due to source mismatch. expectedHost=$expectedHost source=$sourceAddress"
                                )
                            }
                            continue
                        }
                    }
                    val text = String(packet.data, 0, packet.length).trim()
                    val values = try {
                        parseIncoming(text)
                    } catch (e: Exception) {
                        Log.w(
                            logTag,
                            "NMEA parse failed on port $port: ${e.message} packetLength=${text.length}",
                        )
                        continue
                    }
                    if (values.numericValues.isNotEmpty() || values.textValues.isNotEmpty()) {
                        val isAisUpdate = hasAisContent(values.numericValues, values.textValues)
                        if (values.textValues["nmea_sentence"] in setOf("VDM", "VDO")) {
                            Log.d(
                                logTag,
                                "AIS raw: ${values.textValues["nmea0183_raw_line"] ?: "<missing>"}",
                            )
                        }
                        if (packetLogCounter.incrementAndGet() % 10L == 1L) {
                            val sample = values.numericValues.entries
                                .sortedBy { it.key }
                                .take(6)
                                .joinToString(", ") { "${it.key}=${it.value}" }
                            Log.d(logTag, "NMEA update #${packetLogCounter.get()} keys=${values.numericValues.size + values.textValues.size} sample=[$sample]")
                        }
                        if (isAisUpdate) {
                            aisUpdates.emit(values)
                        } else {
                            updates.emit(values)
                        }
                    }
                } catch (_: SocketTimeoutException) {
                    // keep listening
                }
            }
        } catch (e: BindException) {
            Log.e(logTag, "Could not bind UDP listener on port $port: ${e.message}")
        } catch (e: Exception) {
            Log.e(logTag, "NMEA UDP listener failed on port $port: ${e.message}")
            // keep app running if UDP bind fails
        } finally {
            withContext(Dispatchers.IO) {
                socket?.close()
                listenSockets.remove(port)
            }
        }
    }

    private suspend fun listenOnTcp(host: String, port: Int) {
        while (true) {
            var socket: Socket? = null
            try {
                socket = Socket(host, port).also { tcpSocket = it }
                Log.d(logTag, "TCP connected to $host:$port")
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (true) {
                    val line = reader.readLine() ?: break
                    val trimmed = line.trim()
                    if (trimmed.isBlank()) continue
                    val values = try {
                        parseIncoming(trimmed)
                    } catch (e: Exception) {
                        Log.w(
                            logTag,
                            "NMEA parse failed on TCP: ${e.message} textLength=${trimmed.length}",
                        )
                        continue
                    }
                    if (values.numericValues.isNotEmpty() || values.textValues.isNotEmpty()) {
                        val isAisUpdate = hasAisContent(values.numericValues, values.textValues)
                        if (values.textValues["nmea_sentence"] in setOf("VDM", "VDO")) {
                            val aisLine = values.textValues["nmea0183_raw_line"]
                            if (!aisLine.isNullOrBlank()) {
                                Log.d(logTag, "AIS raw: $aisLine")
                            }
                        }
                        if (packetLogCounter.incrementAndGet() % 10L == 1L) {
                            val sample = values.numericValues.entries
                                .sortedBy { it.key }
                                .take(6)
                                .joinToString(", ") { "${it.key}=${it.value}" }
                            Log.d(logTag, "NMEA TCP update #${packetLogCounter.get()} keys=${values.numericValues.size + values.textValues.size} sample=[$sample]")
                        }
                        if (isAisUpdate) {
                            aisUpdates.emit(values)
                        } else {
                            updates.emit(values)
                        }
                    }
                }
            } catch (e: Exception) {
                if (!isTcpReconnectExpected()) {
                    Log.w(logTag, "NMEA TCP listener failed: ${e.message}")
                }
            } finally {
                socket?.close()
                if (tcpSocket === socket) {
                    tcpSocket = null
                }
            }
            if (isTcpReconnectExpected()) {
                kotlinx.coroutines.delay(1000L)
            } else {
                break
            }
        }
    }

    private fun hasAisContent(
        numericValues: Map<String, Float>,
        textValues: Map<String, String>,
    ): Boolean {
        val sentence = textValues["nmea0183_sentence"]
            ?: textValues["nmea_sentence"]
            ?: ""
        val normalizedSentence = sentence.uppercase(Locale.ROOT).trim()
        if (normalizedSentence == "VDM" || normalizedSentence == "VDO") return true

        val category = textValues["nmea0183_category"]?.uppercase(Locale.ROOT)?.trim()
        if (category == "AIS") return true

        if (textValues.any { (key, _) -> key.lowercase(Locale.ROOT).startsWith("ais_") }) return true
        if (numericValues.any { (key, _) -> key.lowercase(Locale.ROOT).startsWith("ais_") }) return true

        return false
    }

    private fun normalizeHost(host: String): String {
        val trimmed = host.trim().trimEnd('\u0000').trim()
        if (trimmed.isBlank()) return ""

        val withoutProtocol = when {
            trimmed.startsWith("udp://", ignoreCase = true) -> trimmed.removePrefix("udp://")
            trimmed.startsWith("tcp://", ignoreCase = true) -> trimmed.removePrefix("tcp://")
            else -> trimmed
        }
        return withoutProtocol
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore(':')
            .trim()
    }

    private fun isTcpReconnectExpected(): Boolean {
        val job = tcpListenJob
        return job?.isActive == true
    }

    private fun parseIncoming(raw: String): NmeaUpdate {
        try {
            if (raw.isBlank()) return NmeaUpdate(emptyMap(), emptyMap())
            val numeric = LinkedHashMap<String, Float>()
            val text = LinkedHashMap<String, String>()
            var seenN2kPayload = false
            var seenNmea0183Sentence = false
            var seenNmea0183Raw = false

            raw.split('\n', '\r')
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                when {
                    line.firstOrNull() == '{' -> parseJson(line, numeric, text)
                    line.firstOrNull() == '[' -> parseJsonArray(line, numeric, text)
                    isN2kLine(line) -> {
                        val normalizedLine = normalizeN2kLine(line)
                        parseN2k(normalizedLine, numeric, text)
                        text["n2k_raw_line"] = normalizedLine
                        seenN2kPayload = true
                    }
                    line.startsWith("$") || line.startsWith("!") -> {
                        if (!parseSpecialLineAsN2k(line, numeric, text)) {
                            parseNmea0183(line, numeric, text)
                            seenNmea0183Sentence = seenNmea0183Sentence || text["nmea_sentence"].let { !it.isNullOrBlank() }
                            seenNmea0183Raw = seenNmea0183Raw || text["nmea0183_raw_line"].let { !it.isNullOrBlank() }
                        } else {
                            seenN2kPayload = true
                            text["nmea_type"] = "NMEA2000"
                        }
                    }
                    else -> parseFlat(line, numeric, text)
                }
            }

        if (text["nmea_type"] == null) {
            val hasNmea0183Key = text["nmea0183_sentence"] != null ||
                text["nmea_sentence"] != null ||
                text.keys.any { it.startsWith("nmea0183_") } ||
                seenNmea0183Raw
            val hasNmea2000Field = text["n2k_detected_pgn"] != null ||
                text["n2k_pgn"] != null ||
                text.keys.any { it.endsWith(".pgn") || it.endsWith("/pgn") || it.endsWith("_pgn") }
            val hasNmea0183Field = hasNmea0183Key || seenNmea0183Sentence

            val isLikelyNmea0183Sentence = (raw.startsWith("$") || raw.startsWith("!")) && hasNmea0183Field

            when {
                isLikelyNmea0183Sentence && !hasNmea2000Field -> {
                    text["nmea_type"] = "NMEA0183"
                }
                hasNmea2000Field || seenN2kPayload || isN2kLine(raw) -> {
                    text["nmea_type"] = if (raw.startsWith("N2K", ignoreCase = true)) "N2K" else "NMEA2000"
                }
                hasNmea0183Field -> {
                    text["nmea_type"] = "NMEA0183"
                }
                raw.firstOrNull() == '{' || raw.firstOrNull() == '[' -> {
                    text["nmea_type"] = "JSON"
                }
                else -> text["nmea_type"] = "FLAT"
            }
        }
        return NmeaUpdate(numericValues = numeric, textValues = text)
        } catch (e: Exception) {
            Log.w(logTag, "parseIncoming failed for raw payload: ${e.message}")
            return NmeaUpdate(emptyMap(), emptyMap())
        }
    }

    private fun isN2kLine(line: String): Boolean {
        val normalized = line.trim().trimStart('$', '!')
        return normalized.regionMatches(0, "N2K,", 0, 4, ignoreCase = true) ||
            normalized.regionMatches(0, "NMEA2000,", 0, 9, ignoreCase = true)
    }

    private fun normalizeN2kLine(line: String): String {
        val normalized = line.trim().trimStart('$', '!')
        return if (normalized.regionMatches(0, "NMEA2000,", 0, 9, ignoreCase = true)) {
            "N2K," + normalized.substring(9)
        } else {
            normalized
        }
    }

    private fun parseSpecialLineAsN2k(
        line: String,
        numeric: MutableMap<String, Float>,
        text: MutableMap<String, String>,
    ): Boolean {
        val body = line.trim().trimStart('$', '!').substringBefore('*').trim()
        if (body.isBlank()) return false
        val tokens = body.split(',').map { it.trim() }.filter { it.isNotBlank() }
        if (tokens.isEmpty()) return false

        if (!tokens[0].uppercase(Locale.ROOT).startsWith("PCDIN")) return false

        return parseActisensePcdin(tokens, line, numeric, text)
    }

    private fun parseActisensePcdin(
        tokens: List<String>,
        sourceLine: String,
        numeric: MutableMap<String, Float>,
        text: MutableMap<String, String>,
    ): Boolean {
        if (tokens.size < 4) return false

        val cleaned = tokens.drop(1).map { it.trim() }.filter { it.isNotBlank() }

        val candidatePgnIndexes = cleaned.indices.map { index ->
            index to cleaned[index].toIntOrNull()
        }.filter {
            val pgn = it.second ?: return@filter false
            pgn in 1..65535
        }

        for ((pgnIndex, pgnValue) in candidatePgnIndexes) {
            val pgn = pgnValue ?: continue
            val sourceIndexCandidates = setOf(
                pgnIndex - 1,
                pgnIndex + 1,
                pgnIndex + 2,
                pgnIndex + 3,
            ).filter { candidate ->
                candidate in cleaned.indices && cleaned[candidate].toIntOrNull() in 0..255 && candidate != pgnIndex
            }

            for (sourceIndex in sourceIndexCandidates) {
                val source = cleaned[sourceIndex].toIntOrNull() ?: continue
                val payloadStart = maxOf(pgnIndex, sourceIndex) + 1
                if (payloadStart >= cleaned.size) continue

                val payloadHex = parseActisensePayloadHex(cleaned, payloadStart) ?: continue
                val candidateLine = "NMEA2000,$pgn,$source,$payloadHex"

                text.remove("n2k_payload_len")
                text.remove("n2k_payload_hex")
                text.remove("n2k_detected_pgn")
                parseN2k(candidateLine, numeric, text)

                if (text["n2k_detected_pgn"] != null && text["n2k_payload_len"] != null) {
                    text["nmea_type"] = "NMEA2000"
                    text["nmea0183_raw_line"] = sourceLine
                    return true
                }
            }
        }

        return false
    }

    private fun parseActisensePayloadHex(
        tokens: List<String>,
        startIndex: Int,
    ): String? {
        if (startIndex !in tokens.indices) return null
        var index = startIndex

        val maybeLength = tokens[index].toIntOrNull()
        if (maybeLength != null && maybeLength in 0..255 && (index + 1) < tokens.size) {
            index += 1
        }

        if (index !in tokens.indices) return null

        val first = sanitizeN2kPayloadToken(tokens[index])
        if (first.isBlank()) return null
        val firstIsLongHex = first.length > 2 &&
            first.length % 2 == 0 &&
            first.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' || it.lowercaseChar() in 'A'..'F' }

        if (firstIsLongHex && isLikelyN2kHexPayload(first)) {
            return first.uppercase(Locale.ROOT)
        }

        val payload = StringBuilder()
        var consumedAny = false
        while (index < tokens.size) {
            val token = sanitizeN2kPayloadToken(tokens[index])
            if (token.isBlank()) break

            val tokenValue = token.toIntOrNull(16)
                ?: token.toIntOrNull()

            if (tokenValue == null || tokenValue !in 0..255) break
            payload.append(tokenValue.toString(16).padStart(2, '0'))
            consumedAny = true
            index += 1
        }

        if (!consumedAny) return null
        val result = payload.toString().uppercase(Locale.ROOT)
        return if (result.isNotBlank() && isLikelyN2kHexPayload(result)) result else null
    }

    private fun parseNmea0183(raw: String, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        val body = raw.substringBefore('*', raw)
            .trim()
            .trimStart('$', '!')
        if (body.length < 4) return
        text["nmea0183_raw_line"] = raw

        val parts = body.split(",")
        if (parts.isEmpty()) return

        val sentence = parts[0].uppercase(Locale.ROOT)
        val type = if (sentence.length >= 3) sentence.takeLast(3) else sentence
        text["nmea_sentence"] = type
        text["nmea0183_sentence_full"] = sentence
        text["nmea0183_sentence"] = type

        val recognizedCategory = when (type) {
            "VDM", "VDO" -> "AIS"
            "MWV", "VWT", "MWD", "WCV" -> "WIND"
            "RMC", "GGA", "VTG", "RMB", "BWC", "HDT", "HDG", "GLL" -> "GPS"
            "DPT", "DBT", "DBS" -> "DEPTH"
            "RPM", "RSA", "RSD" -> "ENGINE"
            "APB", "BOD", "HDM", "HSC" -> "HEADING"
            else -> "UNRESOLVED"
        }
        text["nmea0183_category"] = recognizedCategory
        text["nmea0183_auto_classified"] = (recognizedCategory != "UNRESOLVED").toString()

        when (type) {
            "MWV" -> parseNmea0183Wind(parts, numeric, text)
            "HDT" -> parseNmea0183Heading(parts, numeric, text, magnetic = false)
            "HDG" -> parseNmea0183Heading(parts, numeric, text, magnetic = true)
            "DPT" -> parseNmea0183Depth(parts, numeric, text)
            "RMC" -> parseNmea0183Rmc(parts, numeric, text)
            "VTG" -> parseNmea0183Vtg(parts, numeric, text)
            "GGA" -> parseNmea0183Gga(parts, numeric, text)
            "VDM", "VDO" -> parseNmea0183Ais(parts, numeric, text)
            else -> {
                text["nmea_raw_prefix"] = sentence
                text["nmea0183_fields"] = parts.joinToString(",")
            }
        }
    }

    private fun parseNmea0183Ais(
        parts: List<String>,
        numeric: MutableMap<String, Float>,
        text: MutableMap<String, String>,
    ) {
        val rawSentenceType = "${parts.firstOrNull().orEmpty().uppercase(Locale.ROOT)}"
        val totalParts = parts.getOrNull(1)?.toIntOrNull() ?: return
        val partIndex = parts.getOrNull(2)?.toIntOrNull() ?: return
        if (totalParts <= 0 || partIndex < 1 || partIndex > totalParts) return

        val rawSequenceId = parts.getOrNull(3)?.trim()
        val explicitSequence = rawSequenceId?.takeIf { it.isNotBlank() && it != "0" }
        val channel = parts.getOrNull(4)?.trim()?.ifBlank { "?" } ?: "?"
        val payload = parts.getOrNull(5)?.trim() ?: return
        val fillBits = parts.getOrNull(6)?.trim()?.toIntOrNull() ?: 0
        if (fillBits !in 0..5) return
        val debugSequenceId = rawSequenceId?.takeIf { it.isNotBlank() } ?: "auto"
        Log.d(logTag, "AIS fragment sentence=$rawSentenceType total=$totalParts part=$partIndex/$totalParts seq=$debugSequenceId")

        if (totalParts == 1 && partIndex == 1) {
            text["ais_fragmented"] = "false"
            text["ais_fragment_sequence"] = debugSequenceId
            text["ais_fragment_payload_parts"] = "1/1"
            parseNmea0183AisPayload(payload, fillBits, numeric, text)
            return
        }

        val nowMs = System.currentTimeMillis()
        val partIndexZeroBased = partIndex - 1
        val completeResult: Pair<String, Int>? = synchronized(ais0183FragmentsLock) {
            cleanupExpiredAis0183Fragments(nowMs)
            val (stateKey, state) = if (explicitSequence != null) {
                val stateKey = "${channel}_${explicitSequence}_$totalParts"
                val previousState = ais0183Fragments[stateKey]
                val resolved = if (
                    previousState == null ||
                    previousState.expectedParts != totalParts ||
                    previousState.channel != channel ||
                    (previousState.payloadParts.getOrNull(partIndexZeroBased) != null &&
                        previousState.payloadParts[partIndexZeroBased] != payload)
                ) {
                    ais0183Fragments.remove(stateKey)
                    Ais0183FragmentState(
                        expectedParts = totalParts,
                        startedAtMs = nowMs,
                        lastUpdatedMs = nowMs,
                        channel = channel,
                        sequenceId = explicitSequence,
                        explicitSequence = true,
                        payloadParts = arrayOfNulls(totalParts),
                        fillBits = fillBits,
                    ).also { ais0183Fragments[stateKey] = it }
                } else {
                    previousState
                }
                stateKey to resolved
            } else {
                val candidates = ais0183Fragments.entries.filter { (_, candidateState) ->
                    !candidateState.explicitSequence &&
                        candidateState.expectedParts == totalParts &&
                        candidateState.channel == channel &&
                        partIndexZeroBased < candidateState.payloadParts.size
                }
                val directMatch = candidates.firstOrNull { (_, candidateState) ->
                    candidateState.payloadParts[partIndexZeroBased] == payload
                }
                val firstMissing = candidates
                    .filter { (_, candidateState) -> candidateState.payloadParts[partIndexZeroBased] == null }
                    .minByOrNull { (_, candidateState) -> candidateState.lastUpdatedMs }
                val selected = directMatch ?: firstMissing
                if (selected != null) {
                    selected.key to selected.value
                } else {
                    val sequence = "auto_${ais0183FragmentSequence++}"
                    val stateKey = "${channel}_${sequence}_$totalParts"
                    val created = Ais0183FragmentState(
                        expectedParts = totalParts,
                        startedAtMs = nowMs,
                        lastUpdatedMs = nowMs,
                        channel = channel,
                        sequenceId = sequence,
                        explicitSequence = false,
                        payloadParts = arrayOfNulls(totalParts),
                        fillBits = fillBits,
                    )
                    ais0183Fragments[stateKey] = created
                    stateKey to created
                }
            }

            state.lastUpdatedMs = nowMs
            if (partIndexZeroBased !in state.payloadParts.indices) {
                ais0183Fragments.remove(stateKey)
                return@synchronized null
            }

            if (
                state.payloadParts[partIndexZeroBased] != null &&
                state.payloadParts[partIndexZeroBased] != payload
            ) {
                if (!state.explicitSequence) {
                    ais0183Fragments.remove(stateKey)
                    return@synchronized null
                }
                state.payloadParts.fill(null)
            }

            state.payloadParts[partIndexZeroBased] = payload
            if (partIndex == totalParts) {
                state.fillBits = fillBits
            }
                if (state.payloadParts.any { it == null }) {
                    text["ais_fragment_sequence"] = state.sequenceId
                    text["ais_fragment_payload_parts"] = "${state.payloadParts.count { it != null }}/$totalParts"
                    Log.d(
                        logTag,
                        "AIS fragment state key=${state.sequenceId} " +
                            "got=${state.payloadParts.count { it != null }}/$totalParts",
                    )
                    null
                } else {
                    val combined = state.payloadParts.joinToString("")
                    Log.d(
                        logTag,
                        "AIS fragment complete key=${state.sequenceId} total=$totalParts fillBits=$fillBits",
                    )
                    ais0183Fragments.remove(stateKey)
                    combined to state.fillBits
                }
            }

        if (completeResult != null) {
            val (completePayload, completeFillBits) = completeResult
            text["ais_fragmented"] = "true"
            parseNmea0183AisPayload(completePayload, completeFillBits, numeric, text)
        }
    }

    private fun parseNmea0183AisPayload(
        payload: String,
        fillBits: Int,
        numeric: MutableMap<String, Float>,
        text: MutableMap<String, String>,
    ) {
        val bitStream = decodeAis6BitPayload(payload) ?: return
        if (bitStream.length <= fillBits) return
        val usableBits = if (fillBits > 0) bitStream.dropLast(fillBits) else bitStream
        val messageType = readAisBitsUnsigned(usableBits, 0, 6)?.toInt() ?: return
        if (messageType !in 1..27) return

        text["ais_message_type"] = messageType.toString()
        parseAis0183Mmsi(usableBits, numeric, text)

        when (messageType) {
            1, 2, 3 -> parseAis0183Type1To3(usableBits, numeric, text)
            5 -> parseAis0183Type5(usableBits, numeric, text)
            18, 19 -> parseAis0183Type18(usableBits, numeric, text)
            else -> text["ais_message_type_supported"] = "false"
        }
    }

    private fun parseAis0183Mmsi(bits: String, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        val mmsi = readAisBitsUnsigned(bits, 8, 30)?.toFloat()
        if (mmsi != null && mmsi in 1f..999_999_999f) {
            numeric["ais_mmsi"] = mmsi
            numeric["ais_target_mmsi"] = mmsi
            text["ais_mmsi"] = mmsi.toInt().toString()
        }
    }

    private fun parseAis0183Type1To3(
        bits: String,
        numeric: MutableMap<String, Float>,
        text: MutableMap<String, String>,
    ) {
        val navStatus = readAisBitsUnsigned(bits, 38, 4)?.toFloat()?.takeIf { it in 0f..15f }
        if (navStatus != null) {
            numeric["ais_nav_status"] = navStatus
            numeric["nav_status"] = navStatus
            text["ais_navigation_status"] = navStatus.toInt().toString()
        }
        parseAis0183Position(bits, 50, 61, 89, 116, 128, numeric, text)
    }

    private fun parseAis0183Type5(bits: String, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        val partNumber = readAisBitsUnsigned(bits, 38, 2)?.toInt() ?: return
        text["ais_type5_part"] = partNumber.toString()
        if (partNumber == 0) {
            parseAis0183Type5StaticName(bits, text)
        }
    }

    private fun parseAis0183Type18(
        bits: String,
        numeric: MutableMap<String, Float>,
        text: MutableMap<String, String>,
    ) {
        parseAis0183Position(bits, 46, 56, 84, 112, 123, numeric, text)
    }

    private fun parseAis0183Type5StaticName(bits: String, text: MutableMap<String, String>) {
        val nameStartBit = 112
        val nameChars = 20
        val endBit = nameStartBit + (nameChars * 6)
        if (bits.length < endBit) return
        val rawName = decodeAis6BitText(bits, nameStartBit, nameChars)
        if (!rawName.isBlank()) {
            text["ais_name"] = rawName.trim()
        }
    }

    private fun parseAis0183Position(
        bits: String,
        speedStart: Int,
        lonStart: Int,
        latStart: Int,
        courseStart: Int,
        headingStart: Int,
        numeric: MutableMap<String, Float>,
        text: MutableMap<String, String>,
    ) {
        val speedRaw = readAisBitsUnsigned(bits, speedStart, 10)?.toFloat()
        if (speedRaw != null && speedRaw < 1023f) {
            val speedKn = speedRaw / 10f
            if (speedKn in 0f..102f) {
                numeric["ais_speed"] = speedKn
                numeric["ais_target_speed"] = speedKn
                numeric["ais_speed_kn"] = speedKn
                numeric["ais_speed_knots"] = speedKn
                numeric["ais_sog"] = speedKn
            }
        }

        val longitudeRaw = readAisBitsSigned(bits, lonStart, 28)
        val latitudeRaw = readAisBitsSigned(bits, latStart, 27)
        if (longitudeRaw != null && latitudeRaw != null) {
            val longitude = longitudeRaw / 600_000f
            val latitude = latitudeRaw / 600_000f
            if (longitude in -180f..180f && latitude in -90f..90f) {
                numeric["ais_target_longitude"] = longitude
                numeric["ais_longitude"] = longitude
                numeric["ais_lon"] = longitude
                numeric["target_longitude"] = longitude
                numeric["ais_target_latitude"] = latitude
                numeric["ais_latitude"] = latitude
                numeric["ais_lat"] = latitude
                numeric["target_latitude"] = latitude
            }
        }

        val courseRaw = readAisBitsUnsigned(bits, courseStart, 12)?.toFloat()
        if (courseRaw != null && courseRaw < 3600f) {
            val course = courseRaw / 10f
            if (course in 0f..360f) {
                numeric["ais_course"] = course
                numeric["ais_target_course"] = course
                numeric["course"] = course
                numeric["ais_heading"] = course
                numeric["ais_target_heading"] = course
                text["ais_course"] = course.toString()
            }
        }

        val headingRaw = readAisBitsUnsigned(bits, headingStart, 9)?.toFloat()
        if (headingRaw != null && headingRaw in 0f..360f) {
            numeric["ais_heading"] = headingRaw
            numeric["ais_target_heading"] = headingRaw
            text["ais_heading"] = headingRaw.toString()
        }
    }

    private fun decodeAis6BitPayload(payload: String): String? {
        val bits = StringBuilder(payload.length * 6)
        for (char in payload) {
            val value = decodeAis6BitChar(char)
            if (value !in 0..63) return null
            bits.append(value.toString(2).padStart(6, '0'))
        }
        return bits.toString()
    }

    private fun decodeAis6BitChar(value: Char): Int {
        val charCode = value.code
        return when {
            charCode in 48..87 -> charCode - 48
            charCode in 88..119 -> charCode - 56
            else -> -1
        }
    }

    private fun readAisBitsUnsigned(bits: String, start: Int, length: Int): Float? {
        if (start < 0 || length <= 0) return null
        val end = start + length
        if (end > bits.length) return null
        var value = 0L
        for (index in start until end) {
            value = (value shl 1) + if (bits[index] == '1') 1L else 0L
        }
        return value.toFloat()
    }

    private fun readAisBitsSigned(bits: String, start: Int, length: Int): Float? {
        val unsigned = readAisBitsUnsigned(bits, start, length) ?: return null
        val maxValue = 1L shl length
        var value = unsigned.toLong()
        val signBit = 1L shl (length - 1)
        if ((value and signBit) != 0L) {
            value -= maxValue
        }
        return value.toFloat()
    }

    private fun decodeAis6BitText(bits: String, startBit: Int, chars: Int): String {
        val endBit = startBit + (chars * 6)
        if (startBit < 0 || chars <= 0 || endBit > bits.length) return ""
        val charMap = "@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_ !\"#$%&'()*+,-./0123456789:;<=>?"
        val out = StringBuilder()
        for (index in 0 until chars) {
            val charBitsStart = startBit + (index * 6)
            val charBits = bits.substring(charBitsStart, charBitsStart + 6)
            val value = readAisBitsUnsigned(charBits, 0, 6)?.toInt() ?: continue
            if (value !in 0..63) continue
            out.append(charMap[value])
        }
        return out.toString().trim { it == '@' || it == ' ' }
    }

    private fun cleanupExpiredAis0183Fragments(nowMs: Long) {
        val expiredKeys = ais0183Fragments.entries
            .filter { (_, state) -> nowMs - state.startedAtMs > ais0183FragmentTtlMs }
            .map { it.key }
        if (expiredKeys.isEmpty()) return
        expiredKeys.forEach { ais0183Fragments.remove(it) }
    }

    private fun parseNmea0183Gga(
        parts: List<String>,
        numeric: MutableMap<String, Float>,
        text: MutableMap<String, String>,
    ) {
        if (parts.size < 10) return

        val utcRaw = parts.getOrNull(1)?.trim()
        if (!utcRaw.isNullOrBlank()) {
            text["gps_utc_time"] = formatNmea0183Time(utcRaw)
        }

        val latitude = parseNmea0183Coord(
            parts.getOrNull(2),
            parts.getOrNull(3),
            isLat = true
        )
        if (latitude != null && latitude in -90f..90f) {
            numeric["navigation.position.latitude"] = latitude
            numeric["position.latitude"] = latitude
            numeric["latitude"] = latitude
            numeric["lat"] = latitude
        }

        val longitude = parseNmea0183Coord(
            parts.getOrNull(4),
            parts.getOrNull(5),
            isLat = false
        )
        if (longitude != null && longitude in -180f..180f) {
            numeric["navigation.position.longitude"] = longitude
            numeric["position.longitude"] = longitude
            numeric["longitude"] = longitude
            numeric["lon"] = longitude
            numeric["lng"] = longitude
        }

        parts.getOrNull(6)?.toFloatOrNull()?.let { fixQuality ->
            numeric["gps_fix_quality"] = fixQuality
            text["gps_fix_quality"] = when (fixQuality.toInt()) {
                0 -> "kein_fix"
                1 -> "gps"
                2 -> "dgps"
                3 -> "pps"
                4 -> "rtk"
                5 -> "rtk_float"
                else -> fixQuality.toInt().toString()
            }
        }

        parts.getOrNull(7)?.toFloatOrNull()?.let { satellites ->
            numeric["gps_satellites"] = satellites
        }

        parts.getOrNull(8)?.toFloatOrNull()?.let { hdop ->
            numeric["gps_hdop"] = hdop
        }

        parts.getOrNull(9)?.toFloatOrNull()?.let { altitudeM ->
            if (altitudeM.isFinite() && altitudeM > -10000f && altitudeM < 10000f) {
                numeric["gps_altitude_m"] = altitudeM
                numeric["altitude_m"] = altitudeM
                text["gps_altitude_unit"] = parts.getOrNull(10)?.trim() ?: "M"
            }
        }
    }

    private fun formatNmea0183Time(raw: String): String {
        val clean = raw.trim().substringBefore('*')
        if (clean.length < 6) return clean
        val hh = clean.substring(0, 2)
        val mm = clean.substring(2, 4)
        val ss = clean.substring(4).padEnd(6, '0').substring(0, 2)
        return "$hh:$mm:$ss"
    }

    private fun parseNmea0183Wind(parts: List<String>, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        if (parts.size < 6) return
        val angle = parts.getOrNull(1)?.toFloatOrNull() ?: return
        val reference = parts.getOrNull(2)?.trim()?.uppercase(Locale.ROOT) ?: "R"
        val speedKn = parseNmea0183SpeedToKn(parts.getOrNull(3), parts.getOrNull(4))
            ?: return
        val status = parts.getOrNull(5)?.trim()?.uppercase(Locale.ROOT) ?: "A"
        if (status != "A") return

        numeric["wind_speed_apparent"] = speedKn
        numeric["apparent_wind_speed"] = speedKn
        numeric["wind_speed"] = speedKn
        numeric["wind_angle_apparent"] = angle
        numeric["apparent_wind_angle"] = angle
        numeric["awa"] = angle
        numeric["wind_angle"] = angle
        numeric["wind_direction"] = angle
        numeric["aws"] = speedKn
        text["nmea0183_wind_ref"] = reference
    }

    private fun parseNmea0183Heading(
        parts: List<String>,
        numeric: MutableMap<String, Float>,
        text: MutableMap<String, String>,
        magnetic: Boolean,
    ) {
        val heading = parts.getOrNull(1)?.toFloatOrNull() ?: return
        if (heading !in 0f..360f) return
        numeric["heading"] = heading
        numeric["autopilot_heading"] = heading
        numeric["compass_heading"] = heading
        if (magnetic) text["nmea0183_heading"] = "magnetic" else text["nmea0183_heading"] = "true"
    }

    private fun parseNmea0183Rmc(parts: List<String>, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        if (parts.size < 10) return
        val utcRaw = parts.getOrNull(1)?.trim()
        if (!utcRaw.isNullOrBlank()) {
            val formattedUtc = formatNmea0183Time(utcRaw)
            text["gps_utc_time"] = formattedUtc
            text["nmea0183_time"] = formattedUtc
            text["gps_time"] = formattedUtc
        }
        val status = parts.getOrNull(2)?.trim()?.uppercase(Locale.ROOT) ?: return
        text["nmea0183_rmc_status"] = status
        if (status != "A") return

        val latitude = parseNmea0183Coord(parts.getOrNull(3), parts.getOrNull(4), isLat = true)
        val longitude = parseNmea0183Coord(parts.getOrNull(5), parts.getOrNull(6), isLat = false)
        if (latitude != null && longitude != null) {
            numeric["navigation.position.latitude"] = latitude
            numeric["position.latitude"] = latitude
            numeric["latitude"] = latitude
            numeric["lat"] = latitude
            numeric["navigation.position.longitude"] = longitude
            numeric["position.longitude"] = longitude
            numeric["longitude"] = longitude
            numeric["lon"] = longitude
            numeric["lng"] = longitude
        }

        val sog = parts.getOrNull(7)?.toFloatOrNull()
        if (sog != null) {
            numeric["navigation.speed_over_ground"] = sog
            numeric["speed_over_ground"] = sog
            numeric["sog"] = sog
            numeric["aispeed"] = sog
        }
        val cog = parts.getOrNull(8)?.toFloatOrNull()
        if (cog != null) {
            numeric["navigation.course_over_ground_true"] = cog
            numeric["course_over_ground"] = cog
            numeric["cog"] = cog
        }
    }

    private fun parseNmea0183Vtg(parts: List<String>, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        if (parts.size < 9) return
        val speedKn = parts.getOrNull(7)?.toFloatOrNull()
        if (speedKn != null) {
            numeric["navigation.speed_over_ground"] = speedKn
            numeric["speed_over_ground"] = speedKn
            numeric["sog"] = speedKn
            numeric["aispeed"] = speedKn
        }

        val cog = parts.getOrNull(1)?.toFloatOrNull()
        if (cog != null) {
            numeric["navigation.course_over_ground_true"] = cog
            numeric["course_over_ground"] = cog
            numeric["cog"] = cog
        }
    }

    private fun parseNmea0183Depth(parts: List<String>, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        if (parts.size < 2) return
        val depthMeters = parts.getOrNull(1)?.substringBefore('*')?.toFloatOrNull() ?: return
        if (!depthMeters.isFinite() || depthMeters < 0f) return
        numeric["water_depth_m"] = depthMeters
        numeric["depth_m"] = depthMeters
        numeric["water_depth"] = depthMeters
        text["nmea0183_depth"] = "DPT"
    }

    private fun parseNmea0183SpeedToKn(value: String?, unit: String?): Float? {
        val raw = value?.toFloatOrNull() ?: return null
        return when (unit?.trim()?.uppercase(Locale.ROOT)) {
            "N" -> raw
            "K" -> raw / 1.852f
            "M" -> raw * 1.943844f
            else -> raw
        }
    }

    private fun parseNmea0183Coord(
        value: String?,
        direction: String?,
        isLat: Boolean,
    ): Float? {
        val raw = value?.toFloatOrNull() ?: return null
        val dir = direction?.trim()?.uppercase(Locale.ROOT) ?: return null
        val absRaw = kotlin.math.abs(raw)
        val degrees = if (isLat) (absRaw / 100).toInt() else (absRaw / 100).toInt()
        val minutes = absRaw - (degrees * 100)
        if (minutes !in 0f..60f) return null

        val valueDeg = degrees.toFloat() + (minutes / 60f)
        return when (dir) {
            "N", "E" -> valueDeg
            "S", "W" -> -valueDeg
            else -> null
        }
    }

    private suspend fun sendUdp(payload: String, host: String, port: Int, suppressErrors: Boolean = true) {
        withContext(Dispatchers.IO) {
            try {
                DatagramSocket().use { socket ->
                    socket.broadcast = host == "255.255.255.255"
                    val bytes = payload.toByteArray()
                    val packet = DatagramPacket(
                        bytes,
                        bytes.size,
                        InetAddress.getByName(host),
                        port
                    )
                    socket.send(packet)
                }
            } catch (e: Exception) {
                if (!suppressErrors) {
                    throw e
                }
                // ignore send failures to keep UI responsive
            }
        }
    }

    private suspend fun sendHttp(
        method: String,
        url: String,
        payload: String?,
        headers: Map<String, String>
    ) {
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 3000
                    readTimeout = 3000
                    doInput = true
                    if (payload != null) {
                        doOutput = true
                    }
                    headers.forEach { (key, value) ->
                        setRequestProperty(key, value)
                    }
                }
                if (payload != null) {
                    connection.outputStream.use { stream ->
                        stream.write(payload.toByteArray())
                    }
                }
                connection.responseCode
            } catch (_: Exception) {
                // ignore send failures to keep UI responsive
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun parseFlat(raw: String, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        raw
            .split(';', ',')
            .forEach { item ->
                if (!item.contains('=') && !item.contains(':')) return@forEach
                val pair = if (item.contains('=')) item.split('=', limit = 2) else item.split(':', limit = 2)
                if (pair.size != 2) return@forEach

                val key = pair[0].trim().lowercase(Locale.ROOT)
                val valueText = pair[1].trim()
                val numericValue = valueText.toFloatOrNull()
                if (numericValue != null) {
                    numeric[key] = numericValue
                    return@forEach
                }
                text[key] = valueText
            }
    }

    private fun parseN2k(raw: String, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        val frame = parseN2kFrame(raw) ?: return
        val sourceKey = parseN2kSourceKey(frame.source, frame.payloadHex)
        sourceKey?.let { text["n2k_source"] = it }
        val pgn = frame.pgn
        text["n2k_pgn"] = pgn.toString()
        text["n2k_detected_pgn"] = pgn.toString()
        text["n2k_payload_len"] = frame.payload.size.toString()
        text["n2k_payload_hex"] = frame.payloadHex
        text["n2k_raw_payload"] = raw
        text["n2k_debug_preview"] = raw.take(120)
        if (n2kDebugLogCounter.incrementAndGet() % 1L == 0L) {
            Log.d(
                logTag,
                "N2K debug: raw=${raw.take(180)} pgn=$pgn source=${sourceKey ?: "unbekannt"} payloadLen=${frame.payload.size}",
            )
        }
        val payloadHex = frame.payloadHex
        if (payloadHex.isBlank()) return
        val payload = frame.payload

        when (pgn) {
            130306 -> parseWind130306(payload, numeric, text)
            127250 -> parseHeading127250(payload, numeric, text)
            127245 -> parseRudder127245(payload, numeric, text)
            129026 -> parseCogSog129026(payload, numeric)
            129025, 129029 -> parsePositionRapid129025(payload, numeric)
            127489 -> parseEngine489(payload, numeric)
            127506 -> parseFluidLevel127506(payload, numeric, text)
            127508 -> parseBattery127508(payload, numeric, text)
            127237 -> parseAutopilot127237(payload, numeric, text)
            129038 -> parseAisPosition129038(payload, numeric, text)
            129039 -> parseAisPosition129039(payload, numeric, text)
            128267 -> parseDepth128267(payload, numeric, text)
            else -> {
                // Unknown PGN; keep text-only visibility for debugging.
                text["n2k_payload_len"] = payload.size.toString()
                text["n2k_payload_hex"] = payloadHex
            }
        }
    }

    private data class ParsedN2kFrame(
        val pgn: Int,
        val source: String,
        val payload: ByteArray,
        val payloadHex: String,
    )

    private fun parseN2kFrame(raw: String): ParsedN2kFrame? {
        val tokens = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.size < 3) return null

        val n2kTokens = if (
            tokens[0].equals("N2K", ignoreCase = true) ||
            tokens[0].equals("NMEA2000", ignoreCase = true)
        ) {
            tokens.drop(1)
        } else {
            tokens
        }

        if (n2kTokens.size < 3) return null

        val candidateIndexes = listOf(
            intArrayOf(0, 1, 2),
            intArrayOf(0, 1, 3),
            intArrayOf(1, 2, 3),
            intArrayOf(1, 2, 4),
            intArrayOf(2, 1, 3),
            intArrayOf(2, 1, 4),
            intArrayOf(2, 3, 4),
            intArrayOf(2, 3, 5),
            intArrayOf(3, 2, 4),
            intArrayOf(3, 2, 5),
            intArrayOf(3, 4, 5),
            intArrayOf(4, 2, 5),
            intArrayOf(4, 3, 5),
        )

        for (candidate in candidateIndexes) {
            val pgnIndex = candidate[0]
            val sourceIndex = candidate[1]
            val payloadIndex = candidate[2]
            if (pgnIndex >= n2kTokens.size || sourceIndex >= n2kTokens.size || payloadIndex >= n2kTokens.size) {
                continue
            }

            val pgn = n2kTokens[pgnIndex].toIntOrNull() ?: continue
            if (pgn <= 0 || pgn > 0xFFFF) continue

            val payloadHex = sanitizeN2kPayloadToken(n2kTokens[payloadIndex])
            if (!isLikelyN2kHexPayload(payloadHex)) continue

            val source = n2kTokens[sourceIndex]
            val payload = hexToBytes(payloadHex) ?: continue
            return ParsedN2kFrame(
                pgn = pgn,
                source = source,
                payload = payload,
                payloadHex = payloadHex,
            )
        }

        val payloadCandidates = n2kTokens.withIndex().filter { (_, token) ->
            val payloadHex = sanitizeN2kPayloadToken(token)
            isLikelyN2kHexPayload(payloadHex)
        }

        for (payloadEntry in payloadCandidates) {
            val payloadIndex = payloadEntry.index
            val payloadHex = sanitizeN2kPayloadToken(payloadEntry.value)
            val payload = hexToBytes(payloadHex) ?: continue

            val pgnSearchRange = (0 until payloadIndex).toList() + ((payloadIndex + 1 until n2kTokens.size).toList())
            for (pgnIndex in pgnSearchRange) {
                val pgn = n2kTokens[pgnIndex].toIntOrNull() ?: continue
                if (pgn <= 0 || pgn > 0xFFFF) continue

                val sourceIndex = when {
                    pgnIndex + 1 in n2kTokens.indices && pgnIndex + 1 != payloadIndex -> pgnIndex + 1
                    pgnIndex - 1 in n2kTokens.indices && pgnIndex - 1 != payloadIndex -> pgnIndex - 1
                    else -> {
                        when (payloadIndex - 1) {
                            in n2kTokens.indices -> payloadIndex - 1
                            else -> continue
                        }
                    }
                }

                val source = n2kTokens[sourceIndex]
                return ParsedN2kFrame(
                    pgn = pgn,
                    source = source,
                    payload = payload,
                    payloadHex = payloadHex,
                )
            }
        }
        return null
    }

    private fun sanitizeN2kPayloadToken(raw: String): String {
        val sanitized = raw
            .trim()
            .removePrefix("0x")
            .removePrefix("0X")
            .replace(Regex("[^0-9A-Fa-f]"), "")
        return if ((sanitized.length % 2) == 1) "0$sanitized" else sanitized
    }

    private fun isLikelyN2kHexPayload(rawHex: String): Boolean {
        if (rawHex.isBlank()) return false
        if ((rawHex.length % 2) != 0) return false
        return rawHex.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }
    }

    private fun parseN2kSourceKey(rawSource: String?, payload: String): String? {
        val source = rawSource?.trim()?.trim('"') ?: return null
        if (source.isBlank()) return null

        if (source.contains("_") || source.contains("-") || source.contains(".") || source.contains(":")) {
            return source.lowercase(Locale.ROOT)
        }

        if (!payload.matches(Regex("^[0-9A-Fa-f]+$"))) {
            return source.lowercase(Locale.ROOT)
        }

        val numericSource = source.toIntOrNull()
            ?: return if (source.lowercase(Locale.ROOT).matches(Regex("^[a-z0-9]+$"))) source.lowercase(Locale.ROOT) else null

        return if (numericSource in 0..255) "src-$numericSource" else null
    }

    private fun parseWind130306(payload: ByteArray, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        if (payload.size < 6) return
        val windSpeedMps = readUInt16Le(payload, 1) * 0.01f
        val windDirRad = readUInt16Le(payload, 3) * 0.0001f
        val windDirDeg = radToDeg(windDirRad)
        val windSpeedKn = windSpeedMps * 1.943844f
        val reference = payload[5].toInt() and 0xFF

        val isSupportedWindReference = reference == 0x02 || reference in listOf(0x00, 0x01, 0x03, 0x04)
        if (!isSupportedWindReference) return

        numeric["wind_speed_apparent"] = windSpeedKn
        numeric["apparent_wind_speed"] = windSpeedKn
        numeric["aws"] = windSpeedKn
        numeric["wind_angle_apparent"] = windDirDeg
        numeric["apparent_wind_angle"] = windDirDeg
        numeric["awa"] = windDirDeg
        numeric["wind_speed"] = windSpeedKn
        numeric["wind_angle"] = windDirDeg
        numeric["wind_direction"] = windDirDeg
        text["n2k_wind_reference"] = reference.toString()
    }

    private fun parseHeading127250(payload: ByteArray, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        if (payload.size < 3) return
        val headingRad = readUInt16Le(payload, 1) * 0.0001f
        val headingDeg = radToDeg(headingRad)
        numeric["heading"] = headingDeg
        numeric["autopilot_heading"] = headingDeg
        numeric["compass_heading"] = headingDeg
        text["n2k_heading_ref"] = payload.getOrNull(7)?.toInt()?.and(0x03)?.toString() ?: "0"
    }

    private fun parseRudder127245(payload: ByteArray, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        if (payload.size < 3) return
        val rawRadians = readInt16Le(payload, 1)
        if (rawRadians.isNaN()) return
        if (rawRadians == -32768f) return
        val rudderDeg = radToDeg(rawRadians * 0.0001f)
        val normalized = normalizeSignedDegrees(rudderDeg)
        numeric["rudder"] = normalized
        numeric["rudder_angle"] = normalized
        numeric["rudder_position"] = normalized
        numeric["rudder_angle_deg"] = normalized
        text["n2k_rudder_source"] = "127245"
    }

    private fun parseCogSog129026(payload: ByteArray, numeric: MutableMap<String, Float>) {
        if (payload.size < 6) return
        val cogRad = readUInt16Le(payload, 2) * 0.0001f
        val sogMs = readUInt16Le(payload, 4) * 0.01f
        val sogKn = sogMs * 1.943844f
        val cogDeg = radToDeg(cogRad)

        numeric["navigation.course_over_ground_true"] = cogDeg
        numeric["course_over_ground"] = cogDeg
        numeric["cog"] = cogDeg
        numeric["navigation.speed_over_ground"] = sogKn
        numeric["speed_over_ground"] = sogKn
        numeric["sog"] = sogKn
        numeric["aispeed"] = sogKn
    }

    private fun parsePositionRapid129025(payload: ByteArray, numeric: MutableMap<String, Float>) {
        if (payload.size < 8) return
        val latRaw = readInt32Le(payload, 0)
        val lonRaw = readInt32Le(payload, 4)
        val latitude = latRaw / 10_000_000.0f
        val longitude = lonRaw / 10_000_000.0f

        numeric["navigation.position.latitude"] = latitude
        numeric["position.latitude"] = latitude
        numeric["latitude"] = latitude
        numeric["lat"] = latitude
        numeric["navigation.position.longitude"] = longitude
        numeric["position.longitude"] = longitude
        numeric["longitude"] = longitude
        numeric["lon"] = longitude
        numeric["lng"] = longitude
    }

    private fun parseEngine489(payload: ByteArray, numeric: MutableMap<String, Float>) {
        if (payload.size < 3) return
        val rpmRaw = readUInt16Le(payload, 1)
        if (rpmRaw == 65535f) return
        val rpm = rpmRaw * 0.25f
        if (rpm >= 0f) {
            numeric["engine_rpm"] = rpm
            numeric["rpm"] = rpm
            numeric["engine_rpm1"] = rpm
            numeric["engine_speed"] = rpm
        }
    }

    private fun parseFluidLevel127506(payload: ByteArray, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        if (payload.size < 5) return
        val fluidType = payload.getOrNull(1)?.toInt() ?: return
        val levelRaw = readUInt16Le(payload, 3)
        if (levelRaw.isNaN()) return
        val levelPercent = when {
            levelRaw <= 0f -> 0f
            levelRaw <= 100f -> levelRaw
            levelRaw <= 1000f -> levelRaw / 10f
            levelRaw <= 10000f -> levelRaw / 100f
            else -> (levelRaw % 10000f) / 100f
        }
        if (levelPercent !in 0f..100f) return
        val level = (levelPercent / 100f)

        when (fluidType) {
            1 -> {
                numeric["water_tank"] = level
                numeric["water"] = level
                numeric["tank_level"] = level
                numeric["fresh_water"] = level
                text["n2k_tank_type"] = "water"
            }
            2 -> {
                numeric["grey_water_tank"] = level
                numeric["grey_water"] = level
                numeric["gray_water"] = level
                text["n2k_tank_type"] = "grey"
            }
            3 -> {
                numeric["black_water_tank"] = level
                numeric["black_water"] = level
                text["n2k_tank_type"] = "black"
            }
            9 -> {
                numeric["water_tank"] = level
                numeric["tank_level"] = level
                text["n2k_tank_type"] = "grey_or_gray"
            }
            else -> {
                numeric["tank_level"] = level
                text["n2k_tank_type"] = fluidType.toString()
            }
        }
    }

    private fun parseBattery127508(payload: ByteArray, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        if (payload.size < 7) return

        val soc = readUInt16Le(payload, 1)
        if (!soc.isNaN() && soc >= 0f && soc <= 10000f) {
            val socPercent = when {
                soc <= 100f -> soc
                soc <= 1000f -> soc / 10f
                else -> soc / 100f
            }
            numeric["battery_soc"] = socPercent.coerceIn(0f, 100f)
            numeric["battery_level"] = socPercent
            numeric["soc"] = socPercent
        }

        val batteryVoltageRaw = readUInt16Le(payload, 3)
        if (!batteryVoltageRaw.isNaN() && batteryVoltageRaw > 0f) {
            val voltage = when {
                batteryVoltageRaw <= 1_000f -> batteryVoltageRaw * 0.1f
                batteryVoltageRaw <= 32_767f -> batteryVoltageRaw * 0.01f
                else -> batteryVoltageRaw * 0.001f
            }
            if (voltage in 0.5f..80f) {
                numeric["battery_voltage"] = voltage
                numeric["batt_voltage"] = voltage
                numeric["voltage"] = voltage
            }
        }

        val batteryCurrentRaw = readInt16Le(payload, 5)
        if (!batteryCurrentRaw.isNaN()) {
            val current = when {
                absInt16(batteryCurrentRaw.toInt()) <= 0x7FFE -> {
                    val signed = batteryCurrentRaw
                    when {
                        absInt16(signed.toInt()) <= 8000f -> signed * 0.1f
                        else -> signed
                    }
                }
                else -> 0f
            }
            if (abs(current) <= 10_000f) {
                numeric["battery_current"] = current
                numeric["batt_current"] = current
                numeric["current"] = current
                numeric["battery_power_w"] = when {
                    numeric["battery_voltage"] != null -> (numeric["battery_voltage"] ?: 0f) * current
                    else -> 0f
                }
            }
        }

        text["n2k_battery_source"] = "127508"
    }

    private fun parseAutopilot127237(payload: ByteArray, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        if (payload.size < 8) return
        val modeByte = payload.getOrNull(2)?.toInt()?.and(0xFF) ?: return
        val modeCandidate = when (modeByte) {
            0 -> 0f
            1 -> 0f
            2 -> 2f
            3 -> 1f
            4 -> 2f
            else -> return
        }
        numeric["autopilot_mode"] = modeCandidate
        text["n2k_autopilot_mode"] = modeCandidate.toInt().toString()

        val headingRaw = readUInt16Le(payload, 4)
        if (!headingRaw.isNaN()) {
            val headingDeg = radToDeg(headingRaw * 0.0001f)
            numeric["autopilot_heading_target"] = headingDeg
        }
    }

    private fun parseDepth128267(payload: ByteArray, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        if (payload.size < 3) return

        val depthRaw = listOf(1, 2, 3).firstOrNull { index ->
            if (index + 1 >= payload.size) return@firstOrNull false
            val candidate = readUInt16Le(payload, index)
            candidate < 0xFFFEf && candidate >= 0f
        }?.let { readUInt16Le(payload, it) } ?: return

        val depthM = depthRaw * 0.01f
        if (!depthM.isFinite() || depthM < 0f) return
        val normalizedDepth = depthM.coerceIn(0f, 10000f)
        numeric["water_depth_m"] = normalizedDepth
        numeric["depth_m"] = normalizedDepth
        numeric["water_depth"] = normalizedDepth
        text["n2k_depth_source"] = "128267"
    }

    private fun parseAisPosition129038(payload: ByteArray, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        parseAisPositionPayload(payload, numeric, text, sourcePgn = 129038)
    }

    private fun parseAisPosition129039(payload: ByteArray, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        parseAisPositionPayload(payload, numeric, text, sourcePgn = 129039)
    }

    private data class ParsedAisN2kPayload(
        val mmsi: Float?,
        val longitude: Float?,
        val latitude: Float?,
        val speedKn: Float?,
        val courseDeg: Float?,
        val headingDeg: Float?,
    )

    private fun parseAisPositionPayload(
        payload: ByteArray,
        numeric: MutableMap<String, Float>,
        text: MutableMap<String, String>,
        sourcePgn: Int,
    ) {
        if (payload.size < 12) return

        val parsed = parseAisN2kPayload(payload)
            ?: parseAisLegacyPayload(payload)
            ?: return

        parsed.mmsi?.let { mmsi ->
            numeric["ais_mmsi"] = mmsi
            numeric["ais_target_mmsi"] = mmsi
            text["ais_mmsi"] = mmsi.toInt().toString()
        }

        parsed.longitude?.let { lon ->
            numeric["ais_target_longitude"] = lon
            numeric["target_longitude"] = lon
            numeric["ais_longitude"] = lon
        }
        parsed.latitude?.let { lat ->
            numeric["ais_latitude"] = lat
            numeric["ais_target_latitude"] = lat
            numeric["target_latitude"] = lat
        }

        parsed.speedKn?.let { speedKn ->
            numeric["ais_speed"] = speedKn
            numeric["ais_target_speed"] = speedKn
            numeric["ais_speed_kn"] = speedKn
            numeric["ais_speed_knots"] = speedKn
            numeric["ais_sog"] = speedKn
        }
        parsed.courseDeg?.let { courseDeg ->
            numeric["ais_course"] = courseDeg
            numeric["ais_target_course"] = courseDeg
            numeric["ais_heading"] = courseDeg
            numeric["ais_target_heading"] = courseDeg
        }
        parsed.headingDeg?.let { headingDeg ->
            numeric["ais_heading"] = headingDeg
            numeric["ais_target_heading"] = headingDeg
        }

        text["n2k_pgn"] = sourcePgn.toString()
        text["n2k_payload_len"] = payload.size.toString()
    }

    private fun parseAisN2kPayload(payload: ByteArray): ParsedAisN2kPayload? {
        val bitOrders = listOf(true, false)
        val mmsiOffsets = intArrayOf(8, 0, 16, 24, 10, 12, 40)
        val lonBaseOffsets = intArrayOf(58, 59, 60, 61, 62, 63, 64, 65, 66)
        var bestScore = -1
        var best: ParsedAisN2kPayload? = null

        for (msbFirst in bitOrders) {
            for (mmsiOffset in mmsiOffsets) {
                val mmsiRaw = readBitsUnsigned(payload, mmsiOffset, 30, msbFirst) ?: continue
                if (mmsiRaw !in 1L..99_999_999L) continue

                for (lonOffset in lonBaseOffsets) {
                    val speedRaw = readBitsUnsigned(payload, lonOffset - 11, 10, msbFirst)?.takeIf { it != 1023L }
                    val lonRaw = readBitsSigned(payload, lonOffset, 28, msbFirst) ?: continue
                    val latRaw = readBitsSigned(payload, lonOffset + 28, 27, msbFirst) ?: continue
                    val courseRaw = readBitsUnsigned(payload, lonOffset + 55, 12, msbFirst)?.takeIf { it != 3600L }
                    val headingRaw = readBitsUnsigned(payload, lonOffset + 67, 9, msbFirst)?.takeIf { it != 511L }

                    val longitude = decodeAisN2kCoordinate(lonRaw, maxAbsDegrees = 180f)
                    val latitude = decodeAisN2kCoordinate(latRaw, maxAbsDegrees = 90f)
                    if (longitude == null || latitude == null) continue

                    val speedKn = speedRaw?.takeIf { it in 0L..2550L }?.toFloat()?.let { it * 0.1f }
                    val courseDeg = courseRaw?.takeIf { it in 0L..3599L }?.toFloat()?.let { it * 0.1f }
                    val headingDeg = headingRaw?.takeIf { it in 0L..359L }?.toFloat()

                    val score = listOf(speedKn, courseDeg, headingDeg).count { it != null }
                    if (score <= bestScore) continue

                    bestScore = score
                    best = ParsedAisN2kPayload(
                        mmsi = mmsiRaw.toFloat(),
                        longitude = longitude,
                        latitude = latitude,
                        speedKn = speedKn,
                        courseDeg = courseDeg,
                        headingDeg = headingDeg,
                    )
                }
            }
        }

        return best
    }

    private fun parseAisLegacyPayload(payload: ByteArray): ParsedAisN2kPayload? {
        val mmsi = readUInt32Le(payload, 1)
        if (!mmsi.isFinite() || mmsi !in 1f..99_999_999f) return null

        var longitude: Float? = null
        var latitude: Float? = null
        val coordinatePairs = listOf(
            Pair(11, 15),
            Pair(10, 14),
            Pair(12, 16),
            Pair(13, 17),
            Pair(15, 19),
            Pair(16, 20),
            Pair(17, 21),
        )
        for ((lonOffset, latOffset) in coordinatePairs) {
            if (lonOffset + 3 >= payload.size || latOffset + 3 >= payload.size) continue
            val lon = parseAisCoord(payload, lonOffset, maxAbsDegrees = 180f)
            val lat = parseAisCoord(payload, latOffset, maxAbsDegrees = 90f)
            if (lon != null && lat != null) {
                longitude = lon
                latitude = lat
                break
            }
        }

        val legacy = mutableMapOf<String, Float>()
        parseAisSpeedAndCourse(payload, legacy)
        return ParsedAisN2kPayload(
            mmsi = mmsi,
            longitude = longitude,
            latitude = latitude,
            speedKn = legacy["ais_speed"],
            courseDeg = legacy["ais_course"],
            headingDeg = legacy["ais_heading"],
        )
    }

    private fun parseAisSpeedAndCourse(payload: ByteArray, numeric: MutableMap<String, Float>) {
        val speedOffsets = listOf(8, 16, 18, 20, 22, 24, 26)
        for (offset in speedOffsets) {
            if (offset + 1 >= payload.size) continue
            val speedRaw = readUInt16Le(payload, offset)
            if (speedRaw >= 65534f) continue
            val speedKn = decodeAisSpeedKn(speedRaw)
            if (speedKn == null) continue

            numeric["ais_speed"] = speedKn
            numeric["ais_target_speed"] = speedKn
            numeric["ais_speed_kn"] = speedKn
            numeric["ais_speed_knots"] = speedKn
            numeric["ais_sog"] = speedKn
            break
        }

        val courseOffsets = listOf(17, 19, 21, 23, 25, 27)
        for (offset in courseOffsets) {
            if (offset + 1 >= payload.size) continue
            val courseRaw = readUInt16Le(payload, offset)
            if (courseRaw >= 65535f) continue
            val courseDeg = decodeAisAngleDeg(courseRaw)
            if (courseDeg == null) continue

            numeric["ais_course"] = courseDeg
            numeric["ais_target_course"] = courseDeg
            numeric["ais_heading"] = courseDeg
            numeric["ais_target_heading"] = courseDeg
            break
        }
    }

    private fun parseAisCoord(payload: ByteArray, index: Int, maxAbsDegrees: Float): Float? {
        val raw = readInt32Le(payload, index)
        if (raw.isNaN()) return null
        if (raw == 0x7FFF_FFFF.toFloat()) return null
        return decodeAisN2kCoordinate(raw, maxAbsDegrees)
    }

    private fun decodeAisN2kCoordinate(raw: Float, maxAbsDegrees: Float): Float? {
        if (!raw.isFinite()) return null
        val degreesFromScale60 = raw / 600000f
        if (degreesFromScale60.isFinite() && abs(degreesFromScale60) <= maxAbsDegrees) {
            return degreesFromScale60
        }

        val degreesFromScale1e7 = raw / 10_000_000f
        return if (degreesFromScale1e7.isFinite() && abs(degreesFromScale1e7) <= maxAbsDegrees) {
            degreesFromScale1e7
        } else {
            null
        }
    }

    private fun readBitsUnsigned(payload: ByteArray, startBit: Int, bitCount: Int, msbFirst: Boolean): Long? {
        if (startBit < 0 || bitCount <= 0 || startBit + bitCount > payload.size * 8) return null
        var result = 0L
        for (index in 0 until bitCount) {
            val bitIndex = startBit + index
            val byteIndex = bitIndex / 8
            val shift = if (msbFirst) 7 - (bitIndex % 8) else bitIndex % 8
            val bit = (payload[byteIndex].toInt() shr shift) and 1
            result = (result shl 1) or bit.toLong()
        }
        return result
    }

    private fun readBitsSigned(payload: ByteArray, startBit: Int, bitCount: Int, msbFirst: Boolean): Float? {
        val unsigned = readBitsUnsigned(payload, startBit, bitCount, msbFirst) ?: return null
        if (bitCount >= 63) return null
        val signBit = 1L shl (bitCount - 1)
        val signed = if ((unsigned and signBit) != 0L) {
            unsigned - (1L shl bitCount)
        } else {
            unsigned
        }
        return signed.toFloat()
    }

    private fun decodeAisSpeedKn(speedRaw: Float): Float? {
        if (speedRaw < 0f || !speedRaw.isFinite()) return null
        val candidates = listOf(
            speedRaw * 0.019438f,
            speedRaw * 0.1f,
            speedRaw,
        )
        return candidates.firstOrNull { it in 0f..80f && it.isFinite() }
    }

    private fun decodeAisAngleDeg(angleRaw: Float): Float? {
        if (!angleRaw.isFinite() || angleRaw >= 65535f) return null
        val direct = if (angleRaw <= 360f) angleRaw else null
        val radBased = radToDeg(angleRaw * 0.0001f)
        return when {
            direct != null -> direct
            radBased.isFinite() -> radBased
            else -> null
        }
    }

    private fun radToDeg(rad: Float): Float {
        val degrees = Math.toDegrees(rad.toDouble()).toFloat()
        val wrapped = degrees % 360f
        return if (wrapped < 0f) wrapped + 360f else wrapped
    }

    private fun normalizeSignedDegrees(degrees: Float): Float {
        if (degrees.isNaN()) return degrees
        var value = degrees % 360f
        if (value > 180f) value -= 360f
        if (value < -180f) value += 360f
        return value
    }

    private fun readUInt16Le(payload: ByteArray, index: Int): Float {
        if (index + 1 >= payload.size) return 0f
        return (payload[index].toInt() and 0xFF or ((payload[index + 1].toInt() and 0xFF) shl 8)).toFloat()
    }

    private fun readUInt32Le(payload: ByteArray, index: Int): Float {
        if (index + 3 >= payload.size) return Float.NaN
        val raw = (payload[index].toInt() and 0xFF) or
            ((payload[index + 1].toInt() and 0xFF) shl 8) or
            ((payload[index + 2].toInt() and 0xFF) shl 16) or
            ((payload[index + 3].toInt() and 0xFF) shl 24)
        return raw.toFloat()
    }

    private fun readInt16Le(payload: ByteArray, index: Int): Float {
        if (index + 1 >= payload.size) return Float.NaN
        val raw = payload[index].toInt() and 0xFF or ((payload[index + 1].toInt() and 0xFF) shl 8)
        val signed = if ((raw and 0x8000) != 0) raw - 0x1_0000 else raw
        return signed.toFloat()
    }

    private fun absInt16(value: Int): Float {
        return if (value < 0) -value.toFloat() else value.toFloat()
    }

    private fun readInt32Le(payload: ByteArray, index: Int): Float {
        if (index + 3 >= payload.size) return 0f
        val raw = payload[index].toInt() and 0xFF or
            ((payload[index + 1].toInt() and 0xFF) shl 8) or
            ((payload[index + 2].toInt() and 0xFF) shl 16) or
            ((payload[index + 3].toInt() and 0xFF) shl 24)
        return raw.toFloat().let {
            if (it > Int.MAX_VALUE / 2f) it - 0x1_0000_0000L.toFloat() else it
        }
    }

    private fun hexToBytes(rawHex: String): ByteArray? {
        val hex = rawHex.trim().uppercase(Locale.ROOT).replace("\\s+".toRegex(), "")
        if ((hex.length and 1) == 1) return null
        if (hex.isEmpty()) return ByteArray(0)
        return ByteArray(hex.length / 2) { idx ->
            val start = idx * 2
            val nibblePair = hex.substring(start, start + 2)
            nibblePair.toInt(16).toByte()
        }
    }

    private fun parseJson(raw: String, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        try {
            text["n2k_raw_line"] = raw
            val json = JSONObject(raw)
            val fieldContainers = listOf("fields", "field", "data_fields", "n2kfields", "n2k_fields")
                .flatMap { key ->
                    listOf(
                        json.optJSONObject(key),
                        json.optJSONObjectIgnoreCase(key),
                    ).filterNotNull()
                }
                .distinctBy { it.toString() }

            fieldContainers.forEach { container ->
                flattenJson(container, "", numeric, text)
            }

            if (!parseN2kFromJson(json, numeric, text)) {
                val fieldsObject = json.optJSONObject("fields")
                    ?: json.optJSONObjectIgnoreCase("fields")
                if (fieldsObject != null) {
                    flattenJson(fieldsObject, "ais", numeric, text)
                }
            }
            flattenJson(json, "", numeric, text)
        } catch (_: JSONException) {
            return
        }
    }

    private fun parseJsonArray(
        raw: String,
        numeric: MutableMap<String, Float>,
        text: MutableMap<String, String>,
    ) {
        try {
            val jsonArray = JSONArray(raw)
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.opt(index)
                if (item is JSONObject) {
                    parseJson(item.toString(), numeric, text)
                }
            }
        } catch (_: JSONException) {
            return
        }
    }

    private fun parseN2kFromJson(
        json: JSONObject,
        numeric: MutableMap<String, Float>,
        text: MutableMap<String, String>,
    ): Boolean {
        val candidates = listOfNotNull(
            json,
            json.optJSONObjectIgnoreCase("n2k"),
            json.optJSONObjectIgnoreCase("nmea2000"),
            json.optJSONObjectIgnoreCase("message"),
            json.optJSONObjectIgnoreCase("frame"),
        )
        var parsed = false

        candidates.forEach { candidate ->
            candidate ?: return@forEach
            val parsedHere = parseN2kFromJsonObject(candidate, numeric, text)
            if (parsedHere) parsed = true
        }

        return parsed
    }

    private fun parseN2kFromJsonObject(
        json: JSONObject,
        numeric: MutableMap<String, Float>,
        text: MutableMap<String, String>,
    ): Boolean {
        val pgn = parseJsonInt(
            json,
            listOf(
                "pgn",
                "n2k_pgn",
                "header.pgn",
                "message.pgn",
                "nmea2000.pgn",
                "pg",
                "nmea2000Pgn",
            ),
        )

        if (pgn == null || pgn !in 0..0xFFFF) return false

        val payloadObject = parseJsonText(json, listOf("payload", "payload_hex", "payloadHex", "payloadlen", "payload_len", "data", "datalen", "data_hex", "raw"))
        val sourceKey = parseJsonText(
            json,
            listOf(
                "n2k_source",
                "source",
                "source_address",
                "sourceaddress",
                "src",
                "src_id",
                "src-id",
                "srcaddress",
                "sourceId",
                "header.source",
                "header.src",
                "from",
            ),
        )

        if (sourceKey != null && sourceKey.isNotBlank()) {
            text["n2k_source"] = parseN2kSourceKey(sourceKey, payloadObject ?: "") ?: sourceKey.trim().lowercase()
        }

        text["n2k_pgn"] = pgn.toString()
        text["n2k_detected_pgn"] = pgn.toString()
        if (sourceKey != null) {
            text["n2k_detected_source"] = sourceKey.trim().lowercase()
        }

        val payloadBytes = parsePayloadBytes(json, payloadObject)
        if (payloadBytes == null || payloadBytes.isEmpty()) {
            text["n2k_payload_len"] = "0"
            return true
        }

        text["n2k_payload_len"] = payloadBytes.size.toString()
        text["n2k_payload_hex"] = payloadBytes.joinToString("") { byte -> "%02X".format(byte) }

        when (pgn) {
            130306 -> parseWind130306(payloadBytes, numeric, text)
            127250 -> parseHeading127250(payloadBytes, numeric, text)
            127245 -> parseRudder127245(payloadBytes, numeric, text)
            129026 -> parseCogSog129026(payloadBytes, numeric)
            129025, 129029 -> parsePositionRapid129025(payloadBytes, numeric)
            127489 -> parseEngine489(payloadBytes, numeric)
            127506 -> parseFluidLevel127506(payloadBytes, numeric, text)
            127508 -> parseBattery127508(payloadBytes, numeric, text)
            127237 -> parseAutopilot127237(payloadBytes, numeric, text)
            129038 -> parseAisPosition129038(payloadBytes, numeric, text)
            129039 -> parseAisPosition129039(payloadBytes, numeric, text)
            128267 -> parseDepth128267(payloadBytes, numeric, text)
            else -> {
                // Unknown PGN; keep payload visibility for diagnostics.
            }
        }
        return true
    }

    private fun parsePayloadBytes(json: JSONObject, payloadText: String?): ByteArray? {
        payloadText?.let { text ->
            val cleaned = text.trim()
            if (cleaned.isEmpty()) return ByteArray(0)

            val listLikeText = cleaned.trim().trimStart('[').trimEnd(']')
            if (looksLikeBase64(listLikeText)) {
                return runCatching {
                    Base64.decode(listLikeText, Base64.DEFAULT)
                }.getOrNull()
            }

            // Hex-string payload: "01AB..."
            val asHex = cleaned.replace(Regex("[^0-9A-Fa-f]"), "")
            if (asHex.isNotEmpty() && (asHex.length % 2 == 0)) {
                return runCatching { hexToBytes(asHex) }.getOrNull()
            }

            // Decimal-array encoded as JSON string "1,2,3" or "1;2;3"
            val byteCandidates = listLikeText.split(',', ';')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { it.toIntOrNull() }
                .filter { it in 0..255 }
            if (byteCandidates.isNotEmpty()) {
                return byteCandidates.map { it.toByte() }.toByteArray()
            }
        }

        val payloadArray = json.optJSONArray("data")
            ?: json.optJSONArray("payload")
            ?: json.optJSONArray("payload_hex")
            ?: return null

        if (payloadArray.length() == 0) return null

        val bytes = ByteArray(payloadArray.length())
        for (index in 0 until payloadArray.length()) {
            val value = payloadArray.opt(index)
            val byteValue = when (value) {
                is Int -> value
                is Double -> value.toInt()
                is Long -> value.toInt()
                is Float -> value.toInt()
                is String -> value.toIntOrNull()
                else -> null
            } ?: return null
            bytes[index] = byteValue.toByte()
        }
        return bytes
    }

    private fun parseJsonInt(json: JSONObject, keys: List<String>): Int? {
        parseJsonText(json, keys)?.let { value ->
            value.toFloatOrNull()?.toInt()?.takeIf { it >= 0 }?.let { return it }
            value.toIntOrNull()?.let { return it }
        }
        return null
    }

    private fun parseJsonText(json: JSONObject, keys: List<String>): String? {
        for (key in keys) {
            val raw = json.getByPathIgnoreCase(key)
            if (raw != null) {
                val asText = raw.toString().trim()
                if (asText.isNotBlank()) return asText
            }
        }

        return null
    }

    private fun looksLikeBase64(raw: String): Boolean {
        val normalized = raw.trim()
        if (normalized.isEmpty()) return false
        if (!normalized.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }) return false
        return normalized.length % 4 == 0
    }

    private fun JSONObject.optJSONObjectIgnoreCase(key: String): JSONObject? {
        val requested = key.lowercase(Locale.ROOT)
        val actualKey = keys().asSequence().firstOrNull { it.lowercase(Locale.ROOT) == requested }
        return actualKey?.let { optJSONObject(it) }
    }

    private fun JSONObject.getByPathIgnoreCase(path: String): Any? {
        var current: Any? = this
        val parts = path.split('.')
        for (part in parts) {
            current = when (current) {
                is JSONObject -> {
                    val key = current.keys().asSequence().firstOrNull { it.equals(part, ignoreCase = true) }
                        ?: return null
                    current.opt(key)
                }

                else -> return null
            }
        }
        return current
    }

    private fun flattenJson(obj: JSONObject, prefix: String, numeric: MutableMap<String, Float>, text: MutableMap<String, String>) {
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.opt(key)
            val path = if (prefix.isEmpty()) key else "${prefix}.${key}"

            when (value) {
                is Number -> numeric[path.lowercase(Locale.ROOT)] = value.toString().toFloat()
                is JSONObject -> {
                    val pathValue = value.optString("path").trim()
                    if (pathValue.isNotEmpty() && value.has("value")) {
                        val flattenedPath = pathValue.lowercase(Locale.ROOT)
                        emitJsonValue(flattenedPath, value.opt("value"), numeric, text)
                    } else {
                        flattenJson(value, path, numeric, text)
                    }
                }
                is JSONObject -> flattenJson(value, path, numeric, text)
                is JSONArray -> {
                    for (i in 0 until value.length()) {
                        val item = value.opt(i)
                        when (item) {
                            is Number -> numeric["$path[$i]".lowercase(Locale.ROOT)] = item.toString().toFloat()
                            is JSONObject -> {
                                val itemPath = item.optString("path").trim()
                                if (itemPath.isNotEmpty() && item.has("value")) {
                                    emitJsonValue(itemPath.lowercase(Locale.ROOT), item.opt("value"), numeric, text)
                                } else {
                                    flattenJson(item, "$path[$i]", numeric, text)
                                }
                            }
                        }
                    }
                }
                else -> {
                    val asFloat = value?.toString()?.toFloatOrNull()
                    if (asFloat != null) {
                        numeric[path.lowercase(Locale.ROOT)] = asFloat
                    } else if (value != null) {
                        text[path.lowercase(Locale.ROOT)] = value.toString()
                    }
                }
            }
        }
    }

    private fun emitJsonValue(
        path: String,
        value: Any?,
        numeric: MutableMap<String, Float>,
        text: MutableMap<String, String>,
    ) {
        when (value) {
            is Number -> numeric[path] = value.toString().toFloat()
            is JSONObject, is JSONArray -> {
                if (value is JSONObject) {
                    flattenJson(value, path, numeric, text)
                } else if (value is JSONArray) {
                    for (i in 0 until value.length()) {
                        val item = value.opt(i)
                        when (item) {
                            is Number -> numeric["$path[$i]".lowercase(Locale.ROOT)] = item.toString().toFloat()
                            is JSONObject -> flattenJson(item, "$path[$i]", numeric, text)
                            is String -> text["$path[$i]".lowercase(Locale.ROOT)] = item
                        }
                    }
                }
            }

            else -> {
                val asFloat = value?.toString()?.toFloatOrNull()
                if (asFloat != null) {
                    numeric[path] = asFloat
                } else if (value != null) {
                    text[path] = value.toString()
                }
            }
        }
    }
}
