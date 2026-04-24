package com.seafox.nmea_dashboard.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

private const val TAG_DALY_BMS = "DalyBmsBleManager"
private const val SCAN_WINDOW_MS = 6_000L
private const val RECONNECT_DELAY_MS = 5_000L
private const val POWER_FALLBACK_SCALAR = 1f
private const val MIN_CONNECT_RETRY_DELAY_MS = 1_000L
private const val FALLBACK_SCAN_WINDOW_MS = 6_000L
private const val MAX_DALY_DEBUG_EVENTS = 18
private const val DALY_VALIDATION_TIMEOUT_MS = 6_000L
private const val DALY_INVALID_FRAMES_BEFORE_REJECT = 4
private val DALY_NAME_HINTS = listOf(
    "daly",
    "bms",
    "balance",
    "orion",
)
private val DALY_BLOCKED_NAME_HINTS = listOf(
    "smart solar",
    "smartsolar",
    "solar",
    "mppt",
)
private val DALY_SERVICE_HINTS = listOf(
    "fff0",
    "fff1",
    "ffd0",
    "ffd1",
    "fee0",
)

private val DALY_SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
private val DALY_NOTIFY_CHAR_UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
private val DALY_CONTROL_CHAR_UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
private val CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

private data class DalyParsedFrame(
    val telemetry: Map<String, Float>,
    val text: Map<String, String>,
    val rawFrame: String,
)

private data class ParseMeta(
    val frameText: String,
    val parseAttempted: Boolean = true,
)

class DalyBmsBleManager(applicationContext: Context) {
    private val appContext: Context = applicationContext.applicationContext
    private val bluetoothManager: BluetoothManager? = appContext.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var activeGatt: BluetoothGatt? = null
    private var activeDeviceAddress: String? = null
    private var scanCallback: ScanCallback? = null
    private var isRunning = false
    private var isConnecting = false
    private var scanTimeoutJob: Job? = null
    private var reconnectJob: Job? = null
    private var didTryFallbackScan = false
    private var fallbackScanCandidate: BluetoothDevice? = null
    private var bondRetryAttemptedForCurrentDevice = false
    private var frameWatchdogJob: Job? = null
    private var pollingFallbackJob: Job? = null
    private var dalyValidationJob: Job? = null
    private var lastFrameReceivedAtMs: Long = 0L
    private var dalyValidationFrameCount = 0
    private var isDalyValidated = false

    private val _telemetry = MutableStateFlow<Map<String, Float>>(emptyMap())
    val telemetry: StateFlow<Map<String, Float>> = _telemetry.asStateFlow()
    private val _telemetryText = MutableStateFlow<Map<String, String>>(emptyMap())
    val telemetryText: StateFlow<Map<String, String>> = _telemetryText.asStateFlow()
    private val _connectionStatus = MutableStateFlow("Nicht verbunden")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    private val _debugEvents = MutableStateFlow<List<String>>(emptyList())
    val debugEvents: StateFlow<List<String>> = _debugEvents.asStateFlow()

    private val textTimestampFormat = SimpleDateFormat("HH:mm:ss", Locale.ROOT).apply {
        timeZone = TimeZone.getDefault()
    }
    private val debugEventTimestampFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT).apply {
        timeZone = TimeZone.getDefault()
    }
    private var lastDebugLine = ""
    private var lastDebugLineMs = 0L

    init {
        _telemetryText.value = mapOf("daly_connection_status" to "Nicht verbunden")
    }

    fun start() {
        if (!hasPermissions()) {
            updateConnectionStatus("Berechtigungen fehlen")
            return
        }
        if (!hasBluetoothAdapter()) {
            updateConnectionStatus("Bluetooth ist nicht verfügbar")
            return
        }
        appendDebugEvent("Start angefordert")
        if (isRunning) {
            stop()
        }
        isRunning = true
        didTryFallbackScan = false
        fallbackScanCandidate = null
        bondRetryAttemptedForCurrentDevice = false
        updateConnectionStatus("Suche DALY-BMS...")
        startScan()
    }

    fun stop() {
        isRunning = false
        isConnecting = false
        bondRetryAttemptedForCurrentDevice = false
        stopReadFallback()
        stopDalyValidation()
        frameWatchdogJob?.cancel()
        frameWatchdogJob = null
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        stopScan()
        disconnectGatt()
        appendDebugEvent("Stop angefordert")
        updateConnectionStatus("Nicht verbunden")
    }

    private fun hasBluetoothAdapter(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    private fun hasPermissions(): Boolean {
        return requiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(appContext, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }
    }

    private fun startScan() {
        stopScan()
        if (!isRunning) return
        if (!hasBluetoothAdapter()) {
            updateConnectionStatus("Bluetooth ist deaktiviert")
            return
        }
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            updateConnectionStatus("Bluetooth-Scanner nicht verfügbar")
            return
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (!isRunning || isConnecting) return
                if (isPotentialDalyCandidate(result)) {
                    connectToDevice(result.device)
                } else if (didTryFallbackScan && fallbackScanCandidate == null && canUseAsFallbackDalyCandidate(result)) {
                    fallbackScanCandidate = result.device
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach {
                    if (!isRunning || isConnecting) return@forEach
                    if (isPotentialDalyCandidate(it)) {
                        connectToDevice(it.device)
                    } else if (didTryFallbackScan && fallbackScanCandidate == null && canUseAsFallbackDalyCandidate(it)) {
                        fallbackScanCandidate = it.device
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.w(TAG_DALY_BMS, "BLE Scan failed: $errorCode")
                if (isRunning) {
                    scheduleReconnect(RECONNECT_DELAY_MS, "Scan fehlgeschlagen")
                }
            }
        }

        scanCallback = callback
        try {
            val filters = if (didTryFallbackScan) {
                emptyList()
            } else {
                listOf(
                    ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid(DALY_SERVICE_UUID))
                        .build()
                )
            }
            fallbackScanCandidate = null
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .build()
            scanner.startScan(filters, settings, callback)
            scheduleScanTimeout()
        } catch (ex: SecurityException) {
            Log.w(TAG_DALY_BMS, "BLE scan nicht erlaubt: ${ex.message}")
            updateConnectionStatus("Scan nicht erlaubt")
        }
    }

    private fun scheduleScanTimeout() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = scope.launch {
            delay(if (didTryFallbackScan) FALLBACK_SCAN_WINDOW_MS else SCAN_WINDOW_MS)
            if (!isRunning || activeGatt != null) return@launch
            if (!didTryFallbackScan) {
                didTryFallbackScan = true
                fallbackScanCandidate = null
                updateConnectionStatus("Kein DALY im gefilterten Scan gefunden – Breitensuche")
                startScan()
                return@launch
            }
            stopScan()
            val fallbackDevice = fallbackScanCandidate
            if (fallbackDevice != null) {
                connectToDevice(fallbackDevice)
                return@launch
            }
            updateConnectionStatus("Kein DALY Gerät gefunden")
            scheduleReconnect(RECONNECT_DELAY_MS, "Keine Geräte gefunden")
        }
    }

    private fun isPotentialDalyCandidate(result: ScanResult): Boolean {
        val name = getScanResultName(result)
        if (isBlockedDalyCandidate(name)) {
            appendDebugEvent("Scan übersprungen (Blocklist): ${result.device.address} (${name.ifBlank { "ohne Name" }})")
            return false
        }
        if (hasDalyNameHint(name)) return true

        val advertisedServices = result.scanRecord?.serviceUuids ?: return false
        return advertisedServices.any { uuid ->
            hasDalyServiceHint(uuid.uuid.toString())
        } || advertisedServices.any { it.uuid == DALY_SERVICE_UUID }
    }

    private fun canUseAsFallbackDalyCandidate(result: ScanResult): Boolean {
        val name = getScanResultName(result)
        if (isBlockedDalyCandidate(name)) return false
        if (hasDalyNameHint(name)) return true
        val advertisedServices = result.scanRecord?.serviceUuids ?: return false
        return advertisedServices.any { hasDalyServiceHint(it.uuid.toString()) || it.uuid == DALY_SERVICE_UUID }
    }

    private fun hasDalyNameHint(name: String): Boolean {
        return DALY_NAME_HINTS.any { name.contains(it) }
    }

    private fun isBlockedDalyCandidate(name: String): Boolean {
        return DALY_BLOCKED_NAME_HINTS.any { blocked ->
            name.contains(blocked)
        }
    }

    private fun getScanResultName(result: ScanResult): String {
        return (result.scanRecord?.deviceName ?: safeDeviceName(result.device) ?: "").lowercase(Locale.ROOT)
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice): String? {
        return try {
            device.name
        } catch (_: SecurityException) {
            null
        }
    }

    private fun hasDalyServiceHint(uuid: String): Boolean {
        val value = uuid.lowercase(Locale.ROOT)
        return DALY_SERVICE_HINTS.any { value.contains(it) }
    }

    private fun scheduleReconnect(delayMs: Long, reason: String) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            if (delayMs > MIN_CONNECT_RETRY_DELAY_MS) {
                delay(delayMs)
            }
            if (!isRunning) return@launch
            updateConnectionStatus(reason.ifBlank { "Verbinde..." })
            startScan()
        }
    }

    private fun stopScan() {
        stopReadFallback()
        stopDalyValidation()
        frameWatchdogJob?.cancel()
        frameWatchdogJob = null
        try {
            val scanner = bluetoothAdapter?.bluetoothLeScanner
            scanCallback?.let { callback ->
                scanner?.stopScan(callback)
            }
        } catch (_: SecurityException) {
            // ignore
        }
        scanCallback = null
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (!isRunning || isConnecting) return
        val currentGatt = activeGatt
        val candidateName = (safeDeviceName(device) ?: "").lowercase(Locale.ROOT)
        if (isBlockedDalyCandidate(candidateName)) {
            appendDebugEvent("Verbindungsversuch blockiert wegen Namensfilter: ${device.address} (${candidateName.ifBlank { "ohne Name" }})")
            return
        }
        if (currentGatt != null && currentGatt.device.address == device.address) return
        bondRetryAttemptedForCurrentDevice = false
        stopReadFallback()
        stopDalyValidation()
        isDalyValidated = false
        dalyValidationFrameCount = 0
        frameWatchdogJob?.cancel()
        frameWatchdogJob = null
        lastFrameReceivedAtMs = 0L
        isConnecting = true
        stopScan()
        activeDeviceAddress = device.address
        disconnectGatt()
        updateConnectionStatus("Verbinde: ${safeDeviceName(device) ?: device.address}")
        val gatt = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                @Suppress("DEPRECATION")
                device.connectGatt(appContext, false, gattCallback)
            }
        } catch (ex: SecurityException) {
            Log.w(TAG_DALY_BMS, "Verbindung verweigert: ${ex.message}")
            isConnecting = false
            updateConnectionStatus("Verbindung fehlgeschlagen")
            scheduleReconnect(RECONNECT_DELAY_MS, "Verbindung fehlgeschlagen")
            null
        }
        if (gatt == null) {
            isConnecting = false
            return
        }
        activeGatt = gatt
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        isConnecting = false
                        val stateMessage = describeGattStatus(status)
                        if (isAuthenticationError(status) && activeGatt == gatt) {
                            val shouldRetry = triggerBondFlow(gatt.device, status)
                            if (shouldRetry) {
                                updateConnectionStatus("Kopplung prüfen: $stateMessage")
                                disconnectGatt()
                                scheduleReconnect(RECONNECT_DELAY_MS, "Kopplung prüfen")
                                return
                            }
                        }
                        updateConnectionStatus("Verbindung mit Gerät fehlgeschlagen: $stateMessage")
                        scheduleReconnect(RECONNECT_DELAY_MS, "Verbindung fehlgeschlagen")
                        disconnectGatt()
                        return
                    }
                    isConnecting = false
                    updateConnectionStatus("Daly-Prüfung")
                    val discoveryInitiated = discoverServicesSafely(gatt)
                    if (!discoveryInitiated) {
                        updateConnectionStatus("Services konnten nicht gelesen werden")
                        scheduleReconnect(RECONNECT_DELAY_MS, "Services nicht gefunden")
                    }
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    isConnecting = false
                    if (activeGatt != gatt) return
                    stopReadFallback()
                    frameWatchdogJob?.cancel()
                    frameWatchdogJob = null
                    val fallbackMessage = if (status == BluetoothGatt.GATT_SUCCESS) {
                        "Getrennt"
                    } else {
                        "Verbindung getrennt: ${describeGattStatus(status)}"
                    }
                    updateConnectionStatus(fallbackMessage)
                    if (isAuthenticationError(status) && activeGatt == gatt) {
                        triggerBondFlow(gatt.device, status)
                    }
                    if (isRunning) {
                        scheduleReconnect(RECONNECT_DELAY_MS, fallbackMessage)
                    }
                    disconnectGatt()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                updateConnectionStatus("Services entdecken fehlgeschlagen")
                return
            }
            if (!isRunning) return
            if (activeGatt != gatt) return
            appendDebugEvent(formatDiscoveredServices(gatt))

            val service = findDalyService(gatt)
            if (service == null) {
                val discoveredServiceHexes = gatt.services.joinToString(", ") { it.uuid.toString().lowercase(Locale.ROOT).take(13) }
                updateConnectionStatus("Daly-Service nicht gefunden (Services: $discoveredServiceHexes)")
                scheduleReconnect(RECONNECT_DELAY_MS, "Daly-Service nicht gefunden")
                return
            }
            configureCharacteristics(service, gatt)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (activeGatt != gatt) return
            val raw = characteristic.value ?: return
            appendDebugEvent("Notify erhalten: ${characteristic.uuid} (${raw.size}B)")
            processIncomingDalyFrame(raw, characteristic.uuid == DALY_NOTIFY_CHAR_UUID)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (activeGatt != gatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                appendDebugEvent(
                    "Read-Fehler: ${characteristic.uuid} ${describeGattStatus(status)}"
                )
                return
            }
            val raw = characteristic.value ?: return
            appendDebugEvent("Read erhalten: ${characteristic.uuid} (${raw.size}B)")
            processIncomingDalyFrame(raw, characteristic.uuid == DALY_NOTIFY_CHAR_UUID)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (activeGatt != gatt) return
            appendDebugEvent(
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    "Write OK: ${characteristic.uuid}"
                } else {
                    "Write fehlgeschlagen: ${characteristic.uuid} ${describeGattStatus(status)}"
                }
            )
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            val isCccd = descriptor.uuid == CLIENT_CHARACTERISTIC_CONFIGURATION_UUID
            if (!isCccd) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (isDalyValidated) {
                    updateConnectionStatus("Daly-BMS aktiv")
                } else {
                    updateConnectionStatus("Daly-BMS bereit (Prüfung)")
                }
            } else {
                appendDebugEvent("CCCD-Write Fehler: ${describeGattStatus(status)}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverServicesSafely(gatt: BluetoothGatt): Boolean {
        return try {
            gatt.discoverServices()
        } catch (ex: SecurityException) {
            Log.w(TAG_DALY_BMS, "Service-Discovery nicht erlaubt: ${ex.message}")
            false
        }
    }

    private fun findDalyService(gatt: BluetoothGatt): BluetoothGattService? {
        val exact = gatt.services.firstOrNull { it.uuid == DALY_SERVICE_UUID } ?: gatt.getService(DALY_SERVICE_UUID)
        if (exact != null) {
            appendDebugEvent("Daly-Service exakt gefunden: ${exact.uuid}")
            return exact
        }

        val serviceByCharacteristics = gatt.services.firstOrNull { service ->
            service.getCharacteristic(DALY_NOTIFY_CHAR_UUID) != null &&
                service.getCharacteristic(DALY_CONTROL_CHAR_UUID) != null
        }
        if (serviceByCharacteristics != null) {
            appendDebugEvent("Daly-Service über feste Char-UUIDs gefunden: ${serviceByCharacteristics.uuid}")
            return serviceByCharacteristics
        }

        val scoredService = gatt.services
            .map { service ->
                service to scoreServiceForDaly(service)
            }
            .filter { (service, score) -> score > 40 && isLikelyDalyService(service) }
            .maxByOrNull { it.second }
            ?.first

        if (scoredService != null) {
            appendDebugEvent("Daly-Service über Scoring gefunden: ${scoredService.uuid}")
            return scoredService
        }

        val fallbackService = gatt.services.firstOrNull { service ->
            isLikelyDalyService(service)
        }
        if (fallbackService != null) {
            appendDebugEvent("Daly-Service mit 0xFFF-Fallback gefunden: ${fallbackService.uuid}")
        }
        return fallbackService
    }

    private fun isLikelyDalyService(service: BluetoothGattService): Boolean {
        val uuidText = service.uuid.toString().lowercase(Locale.ROOT)
        if (!uuidText.contains("fff")) return false

        val hasExactNotify = service.getCharacteristic(DALY_NOTIFY_CHAR_UUID) != null
        val hasExactControl = service.getCharacteristic(DALY_CONTROL_CHAR_UUID) != null
        if (hasExactNotify && hasExactControl) {
            return true
        }

        val hasNotify = service.characteristics.any {
            it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 ||
                it.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        }
        val hasWrite = service.characteristics.any {
            it.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
        }
        if (!hasNotify || !hasWrite) return false

        val hasDalyCharHint = service.characteristics.any { characteristic ->
            hasDalyServiceHint(characteristic.uuid.toString())
        }
        val hasKnownDalyCharacteristicHint = hasDalyCharHint ||
            hasDalyServiceHint(service.uuid.toString())
        return hasKnownDalyCharacteristicHint
    }

    private fun scoreServiceForDaly(service: BluetoothGattService): Int {
        var score = 0
        val uuidText = service.uuid.toString().lowercase(Locale.ROOT)
        if (DALY_SERVICE_HINTS.any { hint -> uuidText.contains(hint) }) {
            score += 35
        }
        if (uuidText.contains("fff")) {
            score += 15
        }
        service.characteristics.forEach { characteristic ->
            val charUuidText = characteristic.uuid.toString().lowercase(Locale.ROOT)
            val hasNotify = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
            val hasIndicate = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
            val hasWrite = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
            val hasWriteNoResponse = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
            val hasRead = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0

            if (characteristic.uuid == DALY_NOTIFY_CHAR_UUID || characteristic.uuid == DALY_CONTROL_CHAR_UUID) {
                score += 45
            }
            if (DALY_SERVICE_HINTS.any { hint -> charUuidText.contains(hint) }) {
                score += 12
            }
            if (hasNotify) {
                score += 16
            }
            if (hasIndicate) {
                score += 14
            }
            if (hasWrite || hasWriteNoResponse) {
                score += 9
            }
            if (hasRead) {
                score += 6
            }
        }
        return score
    }

    private fun configureCharacteristics(service: BluetoothGattService, gatt: BluetoothGatt) {
        if (!isLikelyDalyService(service) && service.uuid != DALY_SERVICE_UUID) {
            appendDebugEvent("Daly-Service nicht akzeptiert (Signatur passt nicht): ${service.uuid}")
            updateConnectionStatus("Nicht-DALY Gerät")
            if (isRunning) {
                disconnectGatt()
                scheduleReconnect(RECONNECT_DELAY_MS, "Nicht-DALY Gerät")
            }
            return
        }
        val notifyChar = resolveNotifyCharacteristic(service)
        val controlChar = resolveControlCharacteristic(service)
        if (controlChar != null) {
            controlChar.writeType = if (controlChar.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
        }

        if (notifyChar == null) {
            updateConnectionStatus("Notify-Charakteristik nicht gefunden")
            return
        }

        val notified = setCharacteristicNotificationSafely(gatt, notifyChar)
        if (!notified) {
            updateConnectionStatus("Benachrichtigung konnte nicht aktiviert werden")
            return
        }
        val descriptor = notifyChar.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_UUID)
        if (descriptor == null) {
            updateConnectionStatus("CCCD nicht gefunden")
            return
        }
        try {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            writeDescriptorSafely(gatt, descriptor)
            startDalyValidationCheck(gatt)
            updateConnectionStatus("Daly-BMS bereit (Prüfung)")
            requestInitialFrame(controlChar, gatt)
        } catch (ex: Exception) {
            Log.w(TAG_DALY_BMS, "CCCD write failed: ${ex.message}")
            startDalyValidationCheck(gatt)
            updateConnectionStatus("Daly-BMS bereit (Read)")
        } finally {
            startReadFallback(gatt, service)
            scheduleFrameWatchdog(gatt, service)
        }
    }

    @SuppressLint("MissingPermission")
    private fun setCharacteristicNotificationSafely(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ): Boolean {
        return try {
            gatt.setCharacteristicNotification(characteristic, true)
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeDescriptorSafely(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
    ): Boolean {
        return gatt.writeDescriptor(descriptor)
    }

    private fun startReadFallback(
        gatt: BluetoothGatt,
        service: BluetoothGattService,
        intervalMs: Long = 1_200L,
        attempts: Int = 12,
    ) {
        stopReadFallback()
        if (!isRunning || activeGatt != gatt) return

        val readableChars = service.characteristics.filter { char ->
            char.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
        }
        if (readableChars.isEmpty()) {
            appendDebugEvent("Kein lesbares Characteristic-Fallback verfügbar.")
            return
        }
        pollingFallbackJob = scope.launch {
            var attempt = 0
            while (attempt < attempts && isRunning && activeGatt == gatt) {
                if (lastFrameReceivedAtMs > 0L) return@launch
                attempt++
                appendDebugEvent("Fallback Poll ${attempt}/$attempts: lese ${readableChars.size} Characteristic(s)")
                readableChars.forEach { char ->
                    try {
                        readCharacteristicSafely(gatt, char)
                    } catch (ex: Exception) {
                        appendDebugEvent("Polling-Read nicht gestartet: ${char.uuid} ${ex.message}")
                    }
                    delay(120L)
                }
                delay(intervalMs)
            }
            if (lastFrameReceivedAtMs == 0L) {
                appendDebugEvent("Fallback-Polling beendet ohne Daten.")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun readCharacteristicSafely(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ): Boolean {
        return gatt.readCharacteristic(characteristic)
    }

    private fun stopReadFallback() {
        pollingFallbackJob?.cancel()
        pollingFallbackJob = null
    }

    private fun stopDalyValidation() {
        dalyValidationJob?.cancel()
        dalyValidationJob = null
    }

    private fun scheduleFrameWatchdog(
        gatt: BluetoothGatt,
        service: BluetoothGattService,
        timeoutMs: Long = 3_000L,
    ) {
        frameWatchdogJob?.cancel()
        frameWatchdogJob = scope.launch {
            delay(timeoutMs)
            if (!isRunning) return@launch
            if (activeGatt != gatt) return@launch
            val now = System.currentTimeMillis()
            if (now - lastFrameReceivedAtMs < timeoutMs) return@launch
            appendDebugEvent("Keine Daten innerhalb ${timeoutMs}ms, aktiviere Polling-Fallback.")
            startReadFallback(gatt, service, attempts = 6)
        }
    }

    private fun startDalyValidationCheck(gatt: BluetoothGatt) {
        stopDalyValidation()
        dalyValidationFrameCount = 0
        isDalyValidated = false
        dalyValidationJob = scope.launch {
            delay(DALY_VALIDATION_TIMEOUT_MS)
            if (!isRunning || activeGatt != gatt) return@launch
            if (isDalyValidated) return@launch
            val message = if (dalyValidationFrameCount == 0) {
                "Keine Frames empfangen"
            } else {
                "${dalyValidationFrameCount} unpassende Frames"
            }
            appendDebugEvent("Daly-Validierung fehlgeschlagen: $message")
            updateConnectionStatus("Kein DALY-BMS erkannt")
            if (isRunning) {
                disconnectGatt()
                scheduleReconnect(RECONNECT_DELAY_MS, "Kein DALY-BMS erkannt")
            }
        }
    }

    private fun onUnvalidatedDalyFrame(gatt: BluetoothGatt, parsed: DalyParsedFrame) {
        if (!isRunning || activeGatt != gatt || isDalyValidated) return
        if (isLikelyDalyTelemetryFrame(parsed)) {
            isDalyValidated = true
            stopDalyValidation()
            appendDebugEvent("Daly-Validierung erfolgreich")
            updateConnectionStatus("Daly-BMS aktiv")
            return
        }
        dalyValidationFrameCount++
        appendDebugEvent("Daly-Validierung: ungeeigneter Frame ${dalyValidationFrameCount}/${DALY_INVALID_FRAMES_BEFORE_REJECT}")
        if (dalyValidationFrameCount >= DALY_INVALID_FRAMES_BEFORE_REJECT) {
            appendDebugEvent("Daly-Validierung abgebrochen: Zu viele ungeeignete Frames")
            updateConnectionStatus("Kein DALY-BMS erkannt")
            stopDalyValidation()
            if (isRunning) {
                disconnectGatt()
                scheduleReconnect(RECONNECT_DELAY_MS, "Kein DALY-BMS erkannt")
            }
        }
    }

    private fun isLikelyDalyTelemetryFrame(parsed: DalyParsedFrame): Boolean {
        if (parsed.telemetry.isNotEmpty()) {
            if (parsed.telemetry.containsKey("daly_total_voltage") ||
                parsed.telemetry.containsKey("daly_current") ||
                parsed.telemetry.containsKey("daly_power") ||
                parsed.telemetry.containsKey("daly_state_of_charge") ||
                parsed.telemetry.containsKey("daly_cell_count") ||
                parsed.telemetry.containsKey("daly_capacity_remaining") ||
                parsed.telemetry.containsKey("daly_temperature_1")
            ) {
                return true
            }
        }
        val rawText = parsed.rawFrame.lowercase(Locale.ROOT)
        if (rawText.isNotBlank() && (rawText.contains("daly") || rawText.contains("bms") || rawText.contains("soc") || rawText.contains("cell"))) {
            return true
        }
        if (parsed.text["daly_battery_state"]?.isNotBlank() == true) return true
        return false
    }

    private fun isAuthenticationError(status: Int): Boolean {
        return status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION ||
            status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION ||
            status == 0x05 ||
            status == 0x0F ||
            status == 0x08
    }

    private fun triggerBondFlow(device: BluetoothDevice, status: Int): Boolean {
        if (status == BluetoothGatt.GATT_SUCCESS) return false
        if (bondRetryAttemptedForCurrentDevice) {
            appendDebugEvent("Kopplung bereits versucht – kein zweiter Bond-Versuch: ${describeGattStatus(status)}")
            return false
        }
        if (isBondedSafely(device)) {
            appendDebugEvent("Gerät bereits gekoppelt, trotzdem Authentifizierungsfehler: ${describeGattStatus(status)}")
            return false
        }
        bondRetryAttemptedForCurrentDevice = true
        appendDebugEvent("Kopplung erforderlich. Starte Bonding für ${device.address}.")
        try {
            val started = createBondSafely(device)
            if (!started) {
                appendDebugEvent("Bonding konnte nicht gestartet werden")
            }
            return true
        } catch (ex: Exception) {
            appendDebugEvent("Bonding-Start fehlgeschlagen: ${ex.message}")
            return false
        }
    }

    @SuppressLint("MissingPermission")
    private fun isBondedSafely(device: BluetoothDevice): Boolean {
        return try {
            device.bondState == BluetoothDevice.BOND_BONDED
        } catch (_: SecurityException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun createBondSafely(device: BluetoothDevice): Boolean {
        return device.createBond()
    }

    private fun describeGattStatus(status: Int): String {
        return when (status) {
            BluetoothGatt.GATT_SUCCESS -> "Erfolg"
            BluetoothGatt.GATT_FAILURE -> "Allgemeiner Fehler"
            BluetoothGatt.GATT_READ_NOT_PERMITTED -> "Lesen nicht erlaubt"
            BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "Schreiben nicht erlaubt"
            BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "fehlende Authentifizierung (PIN/Passkey)"
            BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "fehlende Verschlüsselung"
            0x13 -> "ATT-Fehler 0x13 (attributbezogene Protokollstörung, oft falsche Charakteristik/Version)"
            0x05 -> "nicht autorisiert (Auth)"
            0x08 -> "Timeout"
            0x0F -> "Authentifizierung fehlgeschlagen"
            else -> "Code $status"
        }
    }

    private fun resolveNotifyCharacteristic(service: BluetoothGattService): BluetoothGattCharacteristic? {
        val byUuid = service.getCharacteristic(DALY_NOTIFY_CHAR_UUID)
        if (byUuid != null) {
            return byUuid
        }
        val byHint = service.characteristics.firstOrNull { characteristic ->
            DALY_SERVICE_HINTS.any { hint ->
                characteristic.uuid.toString().lowercase(Locale.ROOT).contains(hint)
            }
        }
        if (byHint != null) {
            appendDebugEvent("Notify-Fallback (ID): ${byHint.uuid} in ${service.uuid}")
            return byHint
        }
        val byFlags = service.characteristics.firstOrNull {
            it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 ||
                it.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        }
        if (byFlags != null) {
            appendDebugEvent("Notify-Fallback (Flags): ${byFlags.uuid} in ${service.uuid}")
        }
        return byFlags
    }

    private fun resolveControlCharacteristic(service: BluetoothGattService): BluetoothGattCharacteristic? {
        val byUuid = service.getCharacteristic(DALY_CONTROL_CHAR_UUID)
        if (byUuid != null) {
            return byUuid
        }
        val byWrite = service.characteristics.firstOrNull {
            it.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
        }
        if (byWrite != null) {
            appendDebugEvent("Control-Fallback: ${byWrite.uuid} in ${service.uuid}")
        }
        return byWrite
    }

    private fun requestInitialFrame(controlChar: BluetoothGattCharacteristic?, gatt: BluetoothGatt) {
        if (controlChar == null) return
        val hasWriteResponse = controlChar.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
        val hasWriteNoResponse = controlChar.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
        if (!hasWriteResponse && !hasWriteNoResponse) {
            return
        }
        try {
            controlChar.writeType = if (hasWriteResponse) {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            controlChar.value = byteArrayOf(0x57, 0x53, 0x54, 0x21)
            writeCharacteristicSafely(gatt, controlChar)
        } catch (_: Exception) {
            // Optional: write may fail depending on frame format.
        }
    }

    @SuppressLint("MissingPermission")
    private fun writeCharacteristicSafely(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ): Boolean {
        return gatt.writeCharacteristic(characteristic)
    }

    private fun formatDiscoveredServices(gatt: BluetoothGatt): String {
        if (gatt.services.isEmpty()) {
            return "Services entdeckt: keine"
        }
        val maxItems = minOf(12, gatt.services.size)
        val serviceSummary = gatt.services
            .take(maxItems)
            .joinToString(", ") { service ->
                val characteristicSummary = service.characteristics.joinToString("/") { char ->
                    val flags = mutableListOf<String>()
                    if (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) flags.add("N")
                    if (char.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) flags.add("I")
                    if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) flags.add("W")
                    if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) flags.add("NW")
                    if (char.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) flags.add("R")
                    "${char.uuid.toString().substring(0, 12)}[${flags.joinToString("")}]"
                }
                "${service.uuid.toString().substring(0, 12)}:{${service.characteristics.size}:$characteristicSummary}"
            }
        return "Services entdeckt ($maxItems/${gatt.services.size}): $serviceSummary"
    }

    private fun processIncomingDalyFrame(bytes: ByteArray, fromNotify: Boolean) {
        if (!fromNotify && activeGatt == null) return
        lastFrameReceivedAtMs = System.currentTimeMillis()
        stopReadFallback()
        frameWatchdogJob?.cancel()
        frameWatchdogJob = null
        appendDebugEvent("Frame erhalten (${bytes.size}B, notify=$fromNotify)")
        val decoded = decodeFrame(bytes)
        val parsed = parseDalyFrame(decoded)
        val currentGatt = activeGatt
        if (currentGatt != null) {
            onUnvalidatedDalyFrame(currentGatt, parsed)
        }
        publishDalyFrame(parsed)
    }

    private fun decodeFrame(bytes: ByteArray): ParseMeta {
        val rawText = try {
            String(bytes, Charsets.UTF_8).trim('\u0000')
        } catch (_: Exception) {
            appendDebugEvent("UTF-8-Decodierung fehlgeschlagen")
            ""
        }.trim()
        if (rawText.isNotEmpty() && rawText.any { it.isLetterOrDigit() || it in "{}[]:,.;=#-\n\r\t " }) {
            return ParseMeta(rawText, parseAttempted = true)
        }
        val hex = bytes.joinToString(" ") { byteValue ->
            String.format(Locale.ROOT, "%02X", byteValue)
        }
        appendDebugEvent("Frame ohne Textinhalt empfangen, verwende HEX")
        return ParseMeta(hex, parseAttempted = false)
    }

    private fun publishDalyFrame(result: DalyParsedFrame) {
        val telemetry = result.telemetry.toMutableMap()
        telemetry["daly_last_update"] = System.currentTimeMillis().toFloat()
        val nowText = textTimestampFormat.format(Date())

        val status = if (telemetry.isNotEmpty()) "Verbundene Daten" else "Verbindung aktiv"
        val enrichedText = result.text.toMutableMap()
        val reportedStatus = if (isDalyValidated) {
            status
        } else {
            "Prüfung"
        }
        enrichedText["daly_connection_status"] = reportedStatus
        enrichedText["daly_last_update"] = nowText
        enrichedText["daly_raw_frame"] = result.rawFrame

        _telemetry.update { previous ->
            previous.toMutableMap().apply {
                putAll(telemetry)
            }
        }
        _telemetryText.update { previous ->
            previous.toMutableMap().apply {
                putAll(enrichedText)
            }
        }
        if (isDalyValidated) {
            updateConnectionStatus(status)
        }
    }

    private fun parseDalyFrame(meta: ParseMeta): DalyParsedFrame {
        val sourceText = meta.frameText.ifBlank { "—" }
        val numericValues = mutableMapOf<String, Float>()
        val textValues = mutableMapOf<String, String>()

        textValues["daly_raw_frame"] = sourceText.take(600)
        textValues["daly_connection_status"] = if (meta.parseAttempted) "Aktiv" else "Rohdaten"

        if (sourceText.isNotBlank()) {
            parseStructuredFrame(
                sourceText = sourceText,
                numericValues = numericValues,
                textValues = textValues,
            )
        } else {
            appendDebugEvent("Leerer Frameinhalt")
        }

        val voltage = numericValues["daly_total_voltage"]
        val current = numericValues["daly_current"]
        if (numericValues["daly_power"] == null && voltage != null && current != null) {
            numericValues["daly_power"] = voltage * current * POWER_FALLBACK_SCALAR
        }

        val soc = numericValues["daly_state_of_charge"]
        if (soc != null && soc in 0f..1f) {
            numericValues["daly_state_of_charge"] = soc * 100f
        }
        if (textValues["daly_battery_state"].isNullOrBlank()) {
            when {
                !isDalyValidated -> textValues["daly_battery_state"] = "Prüfung"
                current != null && current > 0.02f -> textValues["daly_battery_state"] = "Lädt"
                current != null && current < -0.02f -> textValues["daly_battery_state"] = "Entlädt"
                else -> textValues["daly_battery_state"] = "Ruhe"
            }
        }
        if (!textValues.containsKey("daly_connection_status")) {
            textValues["daly_connection_status"] = "Verbunden"
        }

        return DalyParsedFrame(
            telemetry = numericValues,
            text = textValues,
            rawFrame = sourceText,
        )
    }

    private fun parseStructuredFrame(
        sourceText: String,
        numericValues: MutableMap<String, Float>,
        textValues: MutableMap<String, String>,
    ) {
        val trimmed = sourceText.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            parseDalyJson(trimmed, numericValues, textValues)
            return
        }

        parseDelimitedPairs(trimmed, numericValues, textValues)
        parseFreeFormKeywords(trimmed, numericValues, textValues)
    }

    private fun parseDalyJson(
        payload: String,
        numericValues: MutableMap<String, Float>,
        textValues: MutableMap<String, String>,
    ) {
        try {
            val json = JSONObject(payload)
            val names = json.keys()
            while (names.hasNext()) {
                val rawKey = names.next()
                val rawValue = json.opt(rawKey)
                val normalized = normalizeDalyField(rawKey)
                val canonicalNumeric = resolveDalyNumericField(normalized)
                val canonicalText = resolveDalyTextField(normalized)

                if (canonicalNumeric != null) {
                    parseNumericValue(rawValue)?.let { numericValues[canonicalNumeric] = it }
                }
                if (canonicalText != null) {
                    rawValue?.toString()?.let { textValues[canonicalText] = it }
                }
            }
        } catch (_: Exception) {
            appendDebugEvent("JSON-Payload nicht interpretierbar, parse als Schlüssel/Wert")
            // Fallback to non-JSON parsing
            parseDelimitedPairs(payload, numericValues, textValues)
        }
    }

    private fun parseDelimitedPairs(
        payload: String,
        numericValues: MutableMap<String, Float>,
        textValues: MutableMap<String, String>,
    ) {
        val tokens = payload.replace(";", ",").replace("|", ",").replace("\n", ",")
        val candidatePairs = tokens.split(",")
        val pairPattern = Regex("(?i)([a-z][a-z0-9_\\-\\s]{0,20})[:=\\s]+([+-]?[0-9]+(?:[.,][0-9]+)?)%?")
        val compactPattern = Regex("(?i)\\b([a-z][a-z0-9_\\-]{0,20})\\s*([+-]?[0-9]+(?:[.,][0-9]+)?)\\b")

        candidatePairs.forEach { token ->
            val cleaned = token.trim()
            if (cleaned.isBlank()) return@forEach
            val pairMatch = pairPattern.find(cleaned)
            if (pairMatch != null) {
                val key = normalizeDalyField(pairMatch.groupValues[1])
                val value = parseFloat(pairMatch.groupValues[2])
                val valueText = pairMatch.groupValues[2]
                mergeParsedValue(
                    rawKey = key,
                    numeric = value,
                    rawTextValue = valueText,
                    numericValues = numericValues,
                    textValues = textValues,
                )
                return@forEach
            }
            val compactMatch = compactPattern.find(cleaned)
            if (compactMatch != null) {
                val key = normalizeDalyField(compactMatch.groupValues[1])
                val value = parseFloat(compactMatch.groupValues[2])
                val valueText = compactMatch.groupValues[2]
                mergeParsedValue(
                    rawKey = key,
                    numeric = value,
                    rawTextValue = valueText,
                    numericValues = numericValues,
                    textValues = textValues,
                )
            }
        }
    }

    private fun parseFreeFormKeywords(
        payload: String,
        numericValues: MutableMap<String, Float>,
        textValues: MutableMap<String, String>,
    ) {
        val normalized = payload.lowercase(Locale.ROOT)
        if (normalized.contains("charging") || normalized.contains("ladestr")) {
            textValues["daly_battery_state"] = "Lädt"
        } else if (normalized.contains("discharge") || normalized.contains("entladen")) {
            textValues["daly_battery_state"] = "Entlädt"
        }
        if (numericValues["daly_state_of_charge"] == null) {
            Regex("(?i)so[cč]\\s*(?:=|:)?\\s*([0-9]+(?:[.,][0-9]+)?)%?")
                .find(payload)
                ?.groupValues?.getOrNull(1)
                ?.let { parseFloat(it)?.let { numericValues["daly_state_of_charge"] = it } }
        }
    }

    private fun mergeParsedValue(
        rawKey: String,
        numeric: Float?,
        rawTextValue: String,
        numericValues: MutableMap<String, Float>,
        textValues: MutableMap<String, String>,
    ) {
        if (numeric == null) return
        val canonicalNumeric = resolveDalyNumericField(rawKey)
        val canonicalText = resolveDalyTextField(rawKey)
        if (canonicalNumeric != null) {
            numericValues[canonicalNumeric] = numeric
        } else if (canonicalText != null) {
            textValues[canonicalText] = rawTextValue
        }
    }

    private fun parseNumericValue(rawValue: Any?): Float? {
        return when (rawValue) {
            is Number -> rawValue.toFloat()
            is String -> parseFloat(rawValue)
            else -> null
        }
    }

    private fun parseFloat(raw: String): Float? {
        val normalized = raw.trim()
            .replace("°", "")
            .replace("C", "", ignoreCase = true)
            .replace("V", "", ignoreCase = true)
            .replace("A", "", ignoreCase = true)
            .replace("W", "", ignoreCase = true)
            .replace("%", "")
            .trim()
            .replace(",", ".")
        return normalized.toFloatOrNull()
    }

    private fun resolveDalyNumericField(rawKey: String): String? {
        return when (normalizeDalyField(rawKey)) {
            "voltage", "volt", "vbatt", "packvoltage", "totalvoltage", "batteryvoltage", "total_voltage", "v" ->
                "daly_total_voltage"
            "current", "amps", "amper", "ibat", "elekstrom", "strom", "chargecurrent" ->
                "daly_current"
            "power", "watt", "watts", "battery_power" -> "daly_power"
            "soc", "socpercent", "stateofcharge", "charge", "soh", "kapazitaet" ->
                "daly_state_of_charge"
            "cellcount", "cells", "cell" -> "daly_cell_count"
            "capacityremaining", "capacity", "remainingcapacity", "restkapazitaet", "capa" ->
                "daly_capacity_remaining"
            "temp1", "t1", "temperature1", "celltemp1", "batterietemp", "temp", "temperature" ->
                "daly_temperature_1"
            else -> null
        }
    }

    private fun resolveDalyTextField(rawKey: String): String? {
        return when (normalizeDalyField(rawKey)) {
            "state", "status", "batterystate", "mode", "charge_status", "battery_state", "stateofcharge" ->
                "daly_battery_state"
            else -> null
        }
    }

    private fun normalizeDalyField(rawKey: String): String {
        return rawKey
            .lowercase(Locale.ROOT)
            .replace(" ", "")
            .replace("-", "")
            .replace("_", "")
            .replace(".", "")
    }

    private fun disconnectGatt() {
        stopDalyValidation()
        isDalyValidated = false
        dalyValidationFrameCount = 0
        stopReadFallback()
        frameWatchdogJob?.cancel()
        frameWatchdogJob = null
        val gatt = activeGatt
        activeGatt = null
        try {
            closeGattSafely(gatt)
        } catch (_: Exception) {
            // ignore
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeGattSafely(gatt: BluetoothGatt?) {
        gatt?.disconnect()
        gatt?.close()
    }

    private fun updateConnectionStatus(status: String) {
        _connectionStatus.update { status }
        appendDebugEvent("Status: $status")
        _telemetryText.update { current ->
            val next = current.toMutableMap()
            next["daly_connection_status"] = status
            next
        }
    }

    private fun appendDebugEvent(message: String) {
        val cleanMessage = message.trim()
        if (cleanMessage.isBlank()) return
        val now = System.currentTimeMillis()
        if (cleanMessage == lastDebugLine && now - lastDebugLineMs < 700L) {
            return
        }
        lastDebugLine = cleanMessage
        lastDebugLineMs = now
        _debugEvents.update { current ->
            val nextLine = "${debugEventTimestampFormat.format(Date(now))} $cleanMessage"
            (listOf(nextLine) + current).take(MAX_DALY_DEBUG_EVENTS)
        }
    }
}
