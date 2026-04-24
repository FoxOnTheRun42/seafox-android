# seaFOX NMEA2000 Dashboard (Android / Compose)

## Ziel
Android-App für Boote mit NMEA2000-Telemetrie über Bord-WLAN.
Die Oberfläche zeigt nur aktiv konfigurierte Anzeigen, die als frei verschiebbare Fenster auf mehreren Seiten organisiert werden können.

## Funktionen im aktuellen Stand
- UDP-Empfang auf Port 41449 (`NmeaNetworkService`)
- Integrierte NMEA2000-Simulation (Top-Bar: `Sim: EIN/AUS`)
- Seitensystem mit horizontalem Wischen (`HorizontalPager`)
- Benannte Seiten erstellen/umbennenen
- Drag & Resize von Widgets
- Standard-Widgets: Batterie, Wassertank, Wind, Autopilot, Motordrehzahl
- Zustand wird in `SharedPreferences` gespeichert (`DashboardRepository`)

## Erwartetes Datenformat
Die App akzeptiert einfache Daten-Pakete über UDP:
- JSON (empfohlen): `{"battery_soc":85,"wind_speed":14.2,"wind_angle":47}`
- Key-Value: `battery_soc=85;wind_speed=14.2;wind_angle=47`

## Aktuell unterstützte NMEA2000 PGNs
- `129025`, `129029` – Position (Rapid/GNSS)
- `129026` – COG & SOG (Rapid Update)
- `127250` – Vessel Heading
- `130306` – Wind Data
- `127245` – Rudder
- `127489` – Engine Speed
- `127506` – Fluid Level (Water/Grey/Black Tank)
- `127508` – Battery (SOC/Spannung/Strom)
- `127237` – Autopilot (Heading/Track Control)
- `129038`, `129039` – AIS Position Reports (MMSI, Position, COG, SOG)

## Build, Test & Product Gate
### Voraussetzungen
- JDK 17
- Android SDK mit Platform `android-35` und Build Tools `35.0.0`
- Lokale SDK-Konfiguration via `local.properties` mit `sdk.dir=...` oder passender Android-Umgebung (`ANDROID_HOME`/`ANDROID_SDK_ROOT`)
- Optional fuer Device-QA: `adb` und ein entsperrtes Testgeraet oder ein Android-Emulator

1. **Projekt-Root ist eindeutig**: dieses Verzeichnis `seaFOX`.
   - Alle Gradle-Befehle muessen im Projekt-Root ausgefuehrt werden.
2. Voller lokaler Produkt-Check:

   ```bash
   ./scripts/seafox-product-check.sh
   ```

   Store-Kandidat mit Release-R8/Minify:

   ```bash
   ./scripts/seafox-product-check.sh --ci --release-r8
   ```

3. Schneller Compile ohne Versions-Increment:

   ```bash
   ./gradlew :app:compileDebugKotlin
   ```

4. Debug-APK bauen:

   ```bash
   ./gradlew :app:assembleDebug
   ```

   Hinweis: `assemble`, `bundle`, `install` und `package` erhoehen die Buildnummer in `version.properties`.

5. Nicht-destruktiver Emulator-/Device-Smoke-Test:

   ```bash
   ./scripts/seafox-device-smoke.sh --serial <device-id>
   ```

   Optional mit bereits vorhandener APK:

   ```bash
   ./scripts/seafox-device-smoke.sh --serial <device-id> --apk app/build/outputs/apk/debug/app-debug.apk
   ```

   Das Script fuehrt keine Gradle-Tasks aus, installiert nur optional per `adb install -r`, loescht keine App-Daten und speichert Screenshot, UI-Dump und Logcat standardmaessig unter `/tmp/seafox-device-smoke/`.

## Produktionsdokumente

- `docs/PRODUCTION_READINESS.md` beschreibt die Defizite, Monetarisierung, Karten-/Kurven-Roadmap und Release-Gates.
- `docs/QA_MATRIX.md` ist die Testmatrix fuer App, Karten, Kurven, AIS, MOB, Offline und Release.
- `docs/RELEASE_CHECKLIST.md` beschreibt Signing, Store-Wahrheit, Device-QA und Rollback fuer Store-Kandidaten.
- `docs/BILLING_BACKEND_CONTRACT.md` beschreibt die Servervalidierung fuer Play-Billing-Entitlements.
- `docs/SAFETY_DISCLAIMER_DRAFT.md` und `docs/PRIVACY_POLICY_DRAFT.md` sind Entwuerfe fuer Safety- und Datenschutztexte.
- `.github/workflows/android-ci.yml` fuehrt Compile, JVM-Tests, Lint und Release-R8 in CI aus.

## Aktuelle Testabdeckung

- Automatisiert: Debug-Kotlin-Compile, JVM-Tests, Android Lint und Release-R8 ueber `./scripts/seafox-product-check.sh --ci --release-r8`.
- Dokumentiert, aber lokal noch nicht nachgewiesen: Emulator-/Device-Flows, Compose-Screenshot-Tests, Phone-/Tablet-QA, signierter Store-Kandidat.
- Safety-/Privacy-Texte sind Drafts und duerfen vor Store/Marketing nicht als juristisch final behandelt werden.

## Repository-Lizenz und Drittanbieter

seaFOX ist ein proprietaeres kommerzielles Produkt. Ohne separate schriftliche Freigabe wird keine Nutzungs-, Kopier-, Weitergabe- oder Vermarktungslizenz am Quellcode erteilt; siehe `LICENSE`.

Drittanbieter-Bibliotheken und Karten-/Asset-Quellen behalten ihre jeweiligen Lizenzen und Nutzungsbedingungen. Eine kuratierte Startliste liegt in `docs/THIRD_PARTY_NOTICES.md`.

## Nächste sinnvolle Erweiterungen
- Wi-Fi Source auf UDP-Broadcast oder TCP-Host/Port konfigurierbar machen
- Mehr passende NMEA2000 PGN-Keys in Widget-Konfigurationen hinterlegen
- Raymarine-orientierte Vektor-Assets als eigene Widget-Hintergründe ersetzen
- Optional: Rechteck-Raster für Snap-to-Grid und Sperr-Modus
