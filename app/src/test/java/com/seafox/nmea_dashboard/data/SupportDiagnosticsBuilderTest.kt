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
            state = DashboardState(nmeaRouterHost = "router.local"),
            appVersionName = "1.0.0",
            androidSdk = 35,
            includeSensitive = true,
        )

        assertEquals("router.local", report.nmeaRouterHost)
    }

    @Test
    fun summarizesPagesWidgetsAndSafetyMarkers() {
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
            mobPosition = MobMarker(lat = 54.0, lon = 10.0),
        )

        val report = SupportDiagnosticsBuilder.build(state, appVersionName = "", androidSdk = -1)

        assertEquals("unknown", report.appVersionName)
        assertEquals(0, report.androidSdk)
        assertEquals(1, report.pageCount)
        assertEquals(2, report.widgetCount)
        assertTrue(report.hasMobMarker)
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
        assertTrue(json.contains("\"nmeaRouterHost\": \"<redacted>\""))
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
}
