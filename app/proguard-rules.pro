# seaFOX release hardening.
#
# The Android Gradle Plugin already keeps manifest components, but these explicit rules
# make release builds easier to audit during store-readiness work.
-keep class com.seafox.nmea_dashboard.SeaFoxApplication { *; }
-keep class com.seafox.nmea_dashboard.MainActivity { *; }
-keep class com.seafox.nmea_dashboard.BootCompletedReceiver { *; }

# Map renderers use native bridges and reflection-heavy internals. Keep them intact until
# we have dedicated release smoke tests on real devices.
-keep class org.maplibre.android.** { *; }
-keep class org.osmdroid.** { *; }

-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
