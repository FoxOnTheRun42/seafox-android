import java.io.FileInputStream
import java.io.FileWriter
import java.util.Properties
import java.util.Locale

fun parseAppVersion(versionName: String): List<Int> = versionName
    .split('.')
    .map { it.toIntOrNull() ?: 0 }
    .let { parsed -> (parsed + listOf(0, 0, 0, 0)).take(4) }

fun escapeBuildConfigString(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

fun writeAppVersionProperties(versionFile: File, properties: Properties) {
    FileWriter(versionFile, false).use { writer ->
        properties.store(writer, null)
    }
}

fun shouldAutoIncrementBuildNumber(taskNames: List<String>): Boolean {
    if (taskNames.isEmpty()) return false
    val triggers = listOf("assemble", "bundle", "install", "package")
    return taskNames
        .map { it.lowercase(Locale.ROOT) }
        .any { taskName -> triggers.any(taskName::contains) }
}

val appVersionFile = rootProject.file("version.properties")
val shouldIncrementAppBuildNumber = shouldAutoIncrementBuildNumber(gradle.startParameter.taskNames)

val appVersionProperties: Properties by lazy {
    val properties = Properties()
    if (appVersionFile.exists()) {
        FileInputStream(appVersionFile).use { stream ->
            properties.load(stream)
        }
    }
    if (shouldIncrementAppBuildNumber) {
        val parts = parseAppVersion(properties.getProperty("app.version.name", "0.2.1.0")).toMutableList()
        parts[3] += 1
        properties.setProperty("app.version.name", parts.joinToString("."))
        if (!properties.containsKey("app.release.title")) {
            properties.setProperty("app.release.title", "")
        }
        writeAppVersionProperties(appVersionFile, properties)
    }
    properties
}

val appVersionName: String by lazy {
    appVersionProperties.getProperty("app.version.name", "0.2.1.0")
}

val appReleaseTitle: String by lazy {
    appVersionProperties.getProperty("app.release.title", "").trim()
}

val appReleaseSnapshot: String by lazy {
    appVersionProperties.getProperty("app.release.snapshot", "").trim()
}

val appBuildKeywords: String by lazy {
    appVersionProperties.getProperty("app.build.keywords", "").trim()
}

val appVersionCode: Int by lazy {
    val parts = parseAppVersion(appVersionName)
    (parts[0] * 1_000_000) +
        (parts[1] * 10_000) +
        (parts[2] * 100) +
        parts[3]
}

val releaseStoreFilePath = providers.environmentVariable("SEAFOX_RELEASE_STORE_FILE").orNull.orEmpty()
val releaseStorePassword = providers.environmentVariable("SEAFOX_RELEASE_STORE_PASSWORD").orNull.orEmpty()
val releaseKeyAlias = providers.environmentVariable("SEAFOX_RELEASE_KEY_ALIAS").orNull.orEmpty()
val releaseKeyPassword = providers.environmentVariable("SEAFOX_RELEASE_KEY_PASSWORD").orNull.orEmpty()
val hasReleaseSigningCredentials = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it.isNotBlank() }

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.seafox.nmea_dashboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.seafox.nmea_dashboard"
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "APP_RELEASE_TITLE", "\"${escapeBuildConfigString(appReleaseTitle)}\"")
        buildConfigField("String", "APP_RELEASE_SNAPSHOT", "\"${escapeBuildConfigString(appReleaseSnapshot)}\"")
        buildConfigField("String", "APP_BUILD_KEYWORDS", "\"${escapeBuildConfigString(appBuildKeywords)}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigningCredentials) {
                storeFile = file(releaseStoreFilePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("Boolean", "SEAFOX_RELEASE_BUILD", "false")
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("Boolean", "SEAFOX_RELEASE_BUILD", "true")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigningCredentials) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lint {
        disable += "GradleDependency"
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")

    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // MapLibre GL Native — GPU-accelerated vector map renderer (BSD 2-Clause)
    implementation("org.maplibre.gl:android-sdk:11.8.4")

    // Google Play Billing: app subscriptions only. Chart licenses stay legally separate.
    implementation("com.android.billingclient:billing:8.3.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
