package com.seafox.nmea_dashboard

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashReportFormatterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun formatsStableCrashReportFields() {
        val text = CrashReportFormatter.format(
            CrashReportPayload(
                appVersionName = "1.2.3",
                appVersionCode = 123,
                androidSdk = 35,
                createdAtEpochMs = 0L,
                threadName = "main",
                throwableClass = "java.lang.IllegalStateException",
                throwableMessage = "boom",
                stackTrace = "stack line",
            )
        )

        assertTrue(text.contains("seaFOX crash report"))
        assertTrue(text.contains("createdAt=1970-01-01T00:00:00.000Z"))
        assertTrue(text.contains("appVersionName=1.2.3"))
        assertTrue(text.contains("appVersionCode=123"))
        assertTrue(text.contains("androidSdk=35"))
        assertTrue(text.contains("thread=main"))
        assertTrue(text.contains("throwable=java.lang.IllegalStateException"))
        assertTrue(text.contains("message=boom"))
        assertTrue(text.contains("stack line"))
    }

    @Test
    fun fillsSafeDefaultsForBlankFields() {
        val text = CrashReportFormatter.format(
            CrashReportPayload(
                appVersionName = "",
                appVersionCode = -1,
                androidSdk = -1,
                createdAtEpochMs = -1L,
                threadName = "",
                throwableClass = "",
                throwableMessage = "",
                stackTrace = "",
            )
        )

        assertTrue(text.contains("appVersionName=unknown"))
        assertTrue(text.contains("appVersionCode=0"))
        assertTrue(text.contains("androidSdk=0"))
        assertTrue(text.contains("thread=unknown"))
        assertTrue(text.contains("throwable=unknown"))
        assertTrue(text.contains("message=<none>"))
        assertTrue(text.contains("<no stacktrace>"))
    }

    @Test
    fun inventoriesPrivateCrashReportsWithoutReadingStackTraces() {
        val filesDir = temporaryFolder.newFolder("files")
        val crashDir = LocalCrashReportStore.directory(filesDir).apply { mkdirs() }
        crashDir.resolve(LocalCrashReportStore.fileNameFor(100L)).writeText("stack one")
        crashDir.resolve(LocalCrashReportStore.fileNameFor(250L)).writeText("stack two")
        crashDir.resolve("notes.txt").writeText("not a crash")

        val inventory = LocalCrashReportStore.inventory(filesDir)

        assertEquals(2, inventory.reportCount)
        assertEquals(250L, inventory.latestCreatedAtEpochMs)
    }

    @Test
    fun emptyCrashInventoryUsesSafeDefaults() {
        val inventory = LocalCrashReportStore.inventory(temporaryFolder.newFolder("empty-files"))

        assertEquals(0, inventory.reportCount)
        assertEquals(0L, inventory.latestCreatedAtEpochMs)
    }
}
