package com.seafox.nmea_dashboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SupportDiagnosticsBuilderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun redactsSensitiveRouterHostByDefault() {
        val report = SupportDiagnosticsBuilder.build(
            state = DashboardState(nmeaRouterHost = "192.168.4.1"),
            appVersionName = "1.0.0",
            androidSdk = 35,
            createdAtEpochMs = 1_000L,
        )

        assertEquals("<redacted>", report.nmeaRouterHost)
        assertEquals(1_000L, report.createdAtEpochMs)
    }

    @Test
    fun canIncludeSensitiveFieldsWhenUserExplicitlyAllowsIt() {
        val report = SupportDiagnosticsBuilder.build(
            state = DashboardState(
                nmeaRouterHost = "router.local",
                activeRoute = SerializedRoute(
                    id = "route-1",
                    name = "Harbor exit",
                    waypoints = listOf(
                        SerializedWaypoint(id = "wp-1", name = "Start", lat = 54.0, lon = 10.0),
                        SerializedWaypoint(id = "wp-2", name = "Turn", lat = 54.1, lon = 10.1),
                    ),
                ),
                mobPosition = MobMarker(lat = 54.0, lon = 10.0),
            ),
            appVersionName = "1.0.0",
            androidSdk = 35,
            includeSensitive = true,
        )

        assertEquals("router.local", report.nmeaRouterHost)
        assertTrue(report.hasActiveRoute)
        assertTrue(report.hasMobMarker)
    }

    @Test
    fun summarizesPagesWidgetsAndRedactsSafetyMarkersByDefault() {
        val state = DashboardState(
            pages = listOf(
                DashboardPage(
                    name = "Navigation",
                    widgets = listOf(
                        DashboardWidget(kind = WidgetKind.GPS, title = "GPS", xPx = 0f, yPx = 0f, widthPx = 100f, heightPx = 100f),
                        DashboardWidget(kind = WidgetKind.KARTEN, title = "Karten", xPx = 0f, yPx = 0f, widthPx = 100f, heightPx = 100f),
                    ),
                )
            ),
            activeRoute = SerializedRoute(
                id = "route-2",
                name = "Night route",
                waypoints = listOf(
                    SerializedWaypoint(id = "wp-3", name = "Start", lat = 54.2, lon = 10.2),
                    SerializedWaypoint(id = "wp-4", name = "Finish", lat = 54.3, lon = 10.3),
                ),
            ),
            mobPosition = MobMarker(lat = 54.0, lon = 10.0),
        )

        val report = SupportDiagnosticsBuilder.build(state, appVersionName = "", androidSdk = -1)

        assertEquals("unknown", report.appVersionName)
        assertEquals(0, report.androidSdk)
        assertEquals(1, report.pageCount)
        assertEquals(2, report.widgetCount)
        assertFalse(report.hasMobMarker)
        assertFalse(report.hasActiveRoute)
    }

    @Test
    fun serializesStableJsonContract() {
        val report = SupportDiagnosticsBuilder.build(
            state = DashboardState(
                backupPrivacyMode = BackupPrivacyMode.privateOnly,
                nmeaRouterProtocol = NmeaRouterProtocol.TCP,
                nmeaRouterHost = "10.0.0.2",
                udpPort = 70000,
            ),
            appVersionName = "1.2.3",
            androidSdk = 35,
            createdAtEpochMs = 42L,
            crashReportCount = 3,
            latestCrashReportAtEpochMs = 2_000L,
        )

        val fields = SupportDiagnosticsJson.toMap(report)
        val json = SupportDiagnosticsJson.toJsonString(report)

        assertEquals("1.2.3", fields["appVersionName"])
        assertEquals(35, fields["androidSdk"])
        assertEquals(42L, fields["createdAtEpochMs"])
        assertEquals("privateOnly", fields["backupPrivacyMode"])
        assertEquals("TCP", fields["nmeaRouterProtocol"])
        assertEquals("<redacted>", fields["nmeaRouterHost"])
        assertEquals(65535, fields["udpPort"])
        assertEquals(3, fields["crashReportCount"])
        assertEquals(2_000L, fields["latestCrashReportAtEpochMs"])
        assertTrue(json.contains("\"nmeaRouterHost\": \"<redacted>\""))
        assertTrue(json.contains("\"crashReportCount\": 3"))
    }

    @Test
    fun writesDiagnosticReportToInternalDirectoryTarget() {
        val directory = temporaryFolder.newFolder("support")
        val report = SupportDiagnosticsBuilder.build(
            state = DashboardState(nmeaRouterHost = "router.local"),
            appVersionName = "2.0.0",
            androidSdk = 34,
            createdAtEpochMs = 1234L,
        )

        val file = SupportDiagnosticsExporter.writeReport(directory, report)
        val text = file.readText()

        assertTrue(file.name.startsWith("seafox-diagnostics-1234"))
        assertTrue(text.contains("\"appVersionName\": \"2.0.0\""))
        assertTrue(text.contains("\"nmeaRouterHost\": \"<redacted>\""))
    }

    @Test
    fun shareContractUsesPrivateCacheSubdirectoryAndJsonMime() {
        val cacheRoot = temporaryFolder.newFolder("cache")
        val directory = SupportDiagnosticsShareContract.cacheDirectory(cacheRoot)

        assertEquals("support-diagnostics", directory.name)
        assertEquals(cacheRoot.absolutePath, directory.parentFile?.absolutePath)
        assertEquals("application/json", SupportDiagnosticsShareContract.MIME_TYPE)
        assertEquals("seaFOX Support-Diagnose", SupportDiagnosticsShareContract.SHARE_SUBJECT)
    }
}
