package com.seafox.nmea_dashboard

import android.app.Application
import android.util.Log

private const val TAG_SEAFOX_APPLICATION = "seaFOXApplication"

class SeaFoxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalCrashReporter.install(this)
        val releaseTitle = BuildConfig.APP_RELEASE_TITLE.takeIf { it.isNotBlank() } ?: "ohne Titel"
        val releaseSnapshot = BuildConfig.APP_RELEASE_SNAPSHOT.takeIf { it.isNotBlank() } ?: "ohne Snapshot"
        val buildKeywords = BuildConfig.APP_BUILD_KEYWORDS.takeIf { it.isNotBlank() } ?: "ohne Stichwoerter"
        Log.i(
            TAG_SEAFOX_APPLICATION,
            "App start version=${BuildConfig.VERSION_NAME} code=${BuildConfig.VERSION_CODE} release=$releaseTitle snapshot=$releaseSnapshot keywords=$buildKeywords"
        )
        runCatching {
            ensureSeaChartInternalDirectoryStructure(this)
        }.onFailure { exception ->
            Log.w(TAG_SEAFOX_APPLICATION, "Konnte SeaCHART-Verzeichnisstruktur nicht initialisieren", exception)
        }
    }
}
