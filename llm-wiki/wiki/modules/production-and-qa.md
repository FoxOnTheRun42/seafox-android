---
title: Production and QA
type: module
status: current
updated: 2026-04-24
sources:
  - ../../../docs/PRODUCTION_READINESS.md
  - ../../../docs/QA_MATRIX.md
  - ../../../docs/RELEASE_CHECKLIST.md
  - ../../../docs/PRIVACY_POLICY_DRAFT.md
  - ../../../docs/SAFETY_DISCLAIMER_DRAFT.md
  - ../../../.github/workflows/android-ci.yml
  - ../../../app/build.gradle.kts
  - ../../../scripts/seafox-product-check.sh
  - ../../../runs/20260424-095641-ceo-sync/brief.md
  - ../../../app/src/main/java/com/boat/dashboard/data/BillingCatalog.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/BillingEntitlementMapper.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/PlayBillingClientGateway.kt
  - ../../../app/src/main/java/com/boat/dashboard/ui/widgets/chart/FirstPartyChartPackages.kt
  - ../../../app/src/main/java/com/boat/dashboard/CrashReporting.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/SupportDiagnostics.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/data/BillingCatalogTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/data/BillingEntitlementMapperTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/CrashReportFormatterTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/data/SupportDiagnosticsBuilderTest.kt
  - ../../../app/src/test
  - ../sources/product-gate-refresh-2026-04-24.md
  - ../../../README.md
  - ../../../app/src/main/java/com/boat/dashboard/data/FeatureAccessPolicy.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/BootAutostartPolicyTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/data/FeatureAccessPolicyTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/HazardOverlayBuilderTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/FreeRasterChartProvidersTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/SeaChartSideLoadPackagesTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/S57CellSelectorTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/s57/S57ToGeoJsonTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/enc/EncRendererSkeletonTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/SeaChartWidgetSettingsModuleTest.kt
---

# Production and QA

## Summary

seaFOX hat am 2026-04-24 ein gruenes lokales Product Gate fuer statische Gesundheit, Kotlin-Compile, JVM-Tests, Android Lint und Release-R8. Seit der aktuellen Produktionshaertung ist Release-R8 nicht nur ein optionaler Script-Schalter, sondern das dokumentierte CI-/Store-Kandidaten-Gate: README, QA-Matrix, Release-Checklist und GitHub Actions verwenden `./scripts/seafox-product-check.sh --ci --release-r8`. Das ist ein wichtiger Release-Shrinker-Nachweis, aber noch keine Release-Freigabe: `adb` fehlt in der lokalen Umgebung, Emulator-/Device-QA wurde nicht ausgefuehrt, ein signierter Store-Kandidat ist nicht belegt, und Release-Gates fuer Play-Console-Produkte, Kauf-/Restore-Device-QA, produktive Backend-Receipt-Validierung, Store-/Beta-Flow und Karten-Licensing bleiben offen. Release-Checklist, Billing-Backend-Contract, Privacy-Policy-Entwurf und Safety-Disclaimer-Entwurf existieren als Arbeitsgrundlage, muessen vor Verkauf aber abgearbeitet, finalisiert und in App/Store uebernommen werden. Der neue Support-Diagnose-Share-Flow ist als Nutzeraktion mit Consent, App-Cache, FileProvider, Android-Sharesheet und Default-Redaction dokumentiert; es gibt keinen automatischen Upload, keine Backend-Triage und noch keinen Device-QA-Nachweis.

## Current Gate Snapshot

Quelle: `runs/20260424-095641-ceo-sync/brief.md`, danach im Wiki-Refresh erneut lokal ausgefuehrt und gegen `scripts/seafox-product-check.sh`, `README.md`, `docs/QA_MATRIX.md`, `docs/RELEASE_CHECKLIST.md`, `.github/workflows/android-ci.yml` und `app/build.gradle.kts` geprueft.

- `./scripts/seafox-product-check.sh --ci`: gruen fuer Static Project Health, `:app:compileDebugKotlin`, `:app:testDebugUnitTest` und `:app:lintDebug`.
- Der erneute lokale Lauf nach den Billing-/Diagnostics-Aenderungen blieb ebenfalls gruen fuer Compile, JVM-Tests und Lint.
- Das Gate prueft Pflichtdateien, Launcher-Intent, Application-ID/Namespace, vorhandenes `app/src/test`, Java-/Gradle-Version, Compile, JVM-Unit-Tests, Lint und optional Release-R8 oder Device Readiness.
- `--release-r8` fuehrt `:app:minifyReleaseWithR8` aus. Laut Script-Hilfe ist das eine Release-R8/Minify-Pruefung ohne signiertes Artefakt.
- `.github/workflows/android-ci.yml` fuehrt das Product Gate in CI mit `--ci --release-r8` aus und laedt Lint- sowie Release-R8-/Mapping-Reports als Artefakte hoch.
- `app/build.gradle.kts` aktiviert im Release-Build `isMinifyEnabled = true` und `isShrinkResources = true`; Release-Signing wird nur gesetzt, wenn alle `SEAFOX_RELEASE_*`-Umgebungsvariablen vorhanden sind.
- `adb` ist lokal nicht installiert. Deshalb wurde keine Emulator- oder Device-QA ausgefuehrt; `--device` waere in dieser Umgebung blockiert.
- Nach der Produktionshaertung wurde `./scripts/seafox-product-check.sh --ci --release-r8` lokal ausgefuehrt und bestand Compile, JVM-Tests, Lint und `:app:minifyReleaseWithR8`. Ein signiertes Store-Artefakt wurde dadurch nicht erzeugt.
- Nach Chart Roadmap Task 03 wurde ein gezielter JVM-Check fuer Billing-Katalog, Billing-Restore-Mapping, Entitlement-Policy und First-Party-Chart-Pack-Status gruen ausgefuehrt.
- Nach dem ersten Chart Roadmap Task 04-Schnitt wurde ein gezielter JVM-Check fuer ENC-Renderer-Skeleton, S-57-Zellenauswahl, SOUNDG/SCAMIN-GeoJSON, oeSENC-Rejection und Provider-Notices gruen ausgefuehrt.
- Nach Chart Roadmap Task 01 wurde der gezielte JVM-Provider-Check gruen ausgefuehrt: `./gradlew :app:testDebugUnitTest --tests '*ChartProviderRegistryTest' --tests '*FreeRasterChartProvidersTest' --tests '*SeaChartWidgetSettingsModuleTest'`.
- Nach Chart Roadmap Task 01 wurde auch das volle lokale Product Gate mit Release-R8 gruen ausgefuehrt: `./scripts/seafox-product-check.sh --ci --release-r8`. `adb` fehlt weiterhin, deshalb wurde keine Emulator-/Device-QA ausgefuehrt.
- Nach dem ersten Task-02-Schnitt wurde gezielt `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests '*SeaChartSideLoadPackagesTest'` gruen ausgefuehrt. Das beweist Dateivertraege und Compile, aber noch keinen echten Android-Dateiauswahl- oder Render-Screenshot.
- Ergebnis: Build-/Unit-/Lint-Status ist gruen; Runtime-, Store- und kommerzielle Release-Faehigkeit sind noch nicht belegt.
- `docs/RELEASE_CHECKLIST.md` verlangt fuer Store-Kandidaten zusaetzlich `--release-r8`, dokumentierte Phone- und Tablet-QA, Signing-Konfiguration, Store-Wahrheit und Rollback-Artefakte.

## Core Commands

```bash
./scripts/seafox-product-check.sh
./scripts/seafox-product-check.sh --ci
./scripts/seafox-product-check.sh --ci --device
./scripts/seafox-product-check.sh --ci --release-r8
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:minifyReleaseWithR8
./gradlew :app:assembleDebug
```

Hinweis: `assemble`, `bundle`, `install` und `package` erhoehen die Buildnummer in `version.properties`.

## Current Test Coverage

Vorhanden in `app/src/test` am 2026-04-24:

- `GeoCalcTest`: Distanz, Peilung, Zielpunkt, VMG und CPA-Grundlogik.
- `AutopilotSafetyGateTest`: blockiert nicht bewaffnete, unbestaetigte, Broadcast-Host- und abgelaufene Dispatches; bestaetigte explizite Gateway-Hosts werden akzeptiert.
- `BackupPrivacyPolicyTest`: private Backups und sensitive Keys wie MMSI, Route, MOB und Router-Host.
- `BootAutostartPolicyTest`: Boot, Locked-Boot, User-Unlocked und interner Delayed Launch respektieren den gespeicherten Opt-in.
- `BillingCatalogTest`: eindeutige Produkt-IDs, aktive Pro-/Navigator-/Fleet-App-Subscription-Produkte, aktives first-party Play-`INAPP`-Chart-Pack, case-/whitespace-tolerantes Lookup und inaktive C-MAP/S-63-Chart-Placeholder ohne Tier- oder Lizenzfreigabe.
- `PlayBillingPurchaseMapperTest`: echte Play-`Purchase`-Objekte werden zu internen Records gemappt, Produkt-IDs normalisiert und standardmaessig `unverified` gehalten.
- `BillingValidationJsonTest`: Backend-Validation-JSON wird zu sicheren Decisions geparst; unbekannte Statuswerte bleiben `unverified`, Tokens werden nicht zurueck serialisiert.
- `BillingValidationHttpClientTest`: Validation-POST-Body enthaelt nur Client-Inputs; leerer Endpoint bleibt sicher `unverified`.
- `BillingEntitlementMapperTest`: Restore-Mapping gewaehrt nur verifizierte `purchased` App-Abos und first-party Chart-Packs, blockiert pending/unverified/rejected und sammelt unacknowledged Tokens.
- `BillingRestoreCoordinatorTest`: Restore-/Servervalidierungs-Seam erzeugt Validation-Requests, behandelt fehlende Backend-Antworten als `unverified`, merged verified/rejected Decisions und haelt first-party Chart-Packs getrennt von App-Stufen.
- `BillingRuntimeRestoreApplierTest`: Runtime-Restore schreibt nur komplett validierte Entitlements in den lokalen Snapshot; pending oder fehlende Backend-Validation laesst bestehende Freischaltungen unveraendert.
- `FirstPartyChartPackagesTest`: Premium-Pack-Lizenzstatus, incomplete/installiert/expired-Zustaende und lokale canonical MBTiles-Dateinamen fuer first-party Pack-Discovery.
- `CrashReportFormatterTest`: lokales Crash-Report-Format mit stabilen Feldern, sicheren Defaults und privater Crash-Inventory-Metadatenlogik fuer Support-Diagnosen.
- `EntitlementPolicyTest`: Free/Pro/Navigator/Fleet-Featurelogik, getrennte first-party Chart-Pack-Entitlements, getrennte Chart-Provider-Lizenzen und Ablaufdatum.
- `FeatureAccessPolicyTest`: Widgets und Premiumfunktionen werden Free/Pro/Navigator/Fleet-Features zugeordnet; first-party Chart-Packs schalten keine App-Features frei.
- `RuntimeEntitlementGateTest`: Premium-Widget-Erstellung wird gegen den persistierten App-Entitlement-Snapshot geprueft; Deny-Messages nennen erforderliche Stufe und trennen Kartenpakete von App-Features.
- `SupportDiagnosticsBuilderTest`: Redaction, optionale sensitive Felder, stabile JSON-Felder, Seiten-/Widget-/Safety-Zusammenfassung, crash-report count/latest timestamp und JSON-Datei-Export in ein bereitgestelltes Verzeichnis. Der neue user-facing FileProvider/cache-Share mit Consent ist damit als Produktvertrag dokumentiert, aber noch nicht per Device-QA bewiesen.
- `ChartProviderRegistryTest`: NOAA/QMAP DE/S-57/OpenSeaCharts als selektierbar oder beta, C-MAP lizenzpflichtig, S-63 nicht implementiert.
- `FreeRasterChartProvidersTest`: QMAP-DE-Tilevertrag, OpenSeaCharts mit OSM-Fallback plus erzwungenem Seamark-Overlay und kein Free-Raster-Override fuer NOAA.
- `SeaChartSideLoadPackagesTest`: erlaubte MBTiles/GeoPackage-Dateiendungen sowie stabile Sideload-Datei- und Ordnernamen.
- `S57CellSelectorTest`: plain `.000`-Zellenauswahl, Kappung grosser ENC-Verzeichnisse und keine implizite oeSENC/oeSU-Unterstuetzung.
- `S57ToGeoJsonTest`: SOUNDG-Tiefenlabels mit stabiler Punkt-Decimal-Formatierung, SCAMIN-/Zoomfilter und Skip fuer Metadata/Unknown-Objekte.
- `EncRendererSkeletonTest`: plain S-57 als Beta/non-ECDIS, oeSENC als nicht implementiert, Layer-Rollen und Safety-Relevanzplanung fuer DEPARE/DEPCNT/SOUNDG.
- `FirstPartyChartPackagesTest`: `seafox-premium-de-coast` ist ohne Entitlement lizenzpflichtig, mit Entitlement lizenziert aber ohne lokale Datei unvollstaendig, mit lokaler Datei ein valider Raster-MBTiles-Kandidat und bei Ablauf expired.
- `SeaChartWidgetSettingsModuleTest`: persistierte Provider-Normalisierung inklusive legacy QMAP, OSM und JSON-Parsing mit echter JVM-JSON-Bibliothek.
- `SafetyContourPolicyTest`: Safety-Depth-Berechnung und Filterung von DEPARE, DEPCNT und SOUNDG.
- `HazardOverlayBuilderTest`: DEPARE/DEPCNT/SOUNDG-Fixtures, `kind`-Fallback, Tiefenalias-Felder, `DRVAL2`-Fallback und Non-Depth/invalid-depth-Ablehnung.

Fehlend:

- Emulator-Tooling im aktuellen Workspace, weil `adb` hier nicht installiert ist.
- Emulator-Smoke fuer App-Start, Simulator, Chart, MOB, Settings, Fullscreen-Chart und Support-Diagnostics-FileProvider/cache-Share mit Consent.
- manuelle Device-QA auf Tablet und Smartphone, inklusive Boot-Autostart-Verhalten.
- Compose Screenshot-Tests.
- NMEA-Replay-Fixtures.
- Karten-/ENC-Fixtures und Screenshot-Proof fuer Safety Contour und reale Tiefenfeatures.
- Hardware-Bench-Protokolle.

## Release Gates

Status am 2026-04-24: Compile/Test/Lint sind gruen, aber die Release-Gates sind offen. Vor einer Verkaufs- oder Store-Freigabe braucht seaFOX:

- wiederholbares gruenes `./scripts/seafox-product-check.sh --ci`
- Device-forderndes Gate mit `./scripts/seafox-product-check.sh --ci --device`, sobald `adb` und mindestens ein Device/Emulator verfuegbar sind
- verpflichtendes Release-R8/Minify-Gate mit `./scripts/seafox-product-check.sh --ci --release-r8` fuer CI und Store-Kandidaten
- Emulator-Smoke fuer App-Start, Simulator, Chart, MOB, Settings und kritische Settings-/Export-Pfade
- Emulator-Smoke fuer lokalen MBTiles/GeoPackage-Import ueber Android-Dateiauswahl, Offline-Anzeige von Raster-MBTiles und ehrliche Nicht-Renderbar-Meldung fuer GeoPackage/Vector-MBTiles
- manuelle Device-QA auf Tablet und Smartphone, jeweils Portrait/Landscape, Offline, GPS-Berechtigungen, MOB, Fullscreen-Chart, Kartenquelle, NMEA-Router-Ausfall, Diagnose-Share-Consent/FileProvider/Redaction und Boot-Autostart nur nach Opt-in
- Support-Wahrheit: Diagnose-Share ist Nutzer-initiiert, standardmaessig redigiert, cache-/FileProvider-basiert und ohne automatischen Upload oder Backend-Triage-Versprechen
- crash-freier Beta-Lauf mit Testpersonen
- Datenschutz, Impressum, Safety Disclaimer und Store Listing; `docs/PRIVACY_POLICY_DRAFT.md` und `docs/SAFETY_DISCLAIMER_DRAFT.md` sind Entwuerfe und brauchen juristische Finalisierung
- Release-Signing, Keystore-Verantwortung und Rollback-Plan; die Checklist nennt `SEAFOX_RELEASE_STORE_FILE`, `SEAFOX_RELEASE_STORE_PASSWORD`, `SEAFOX_RELEASE_KEY_ALIAS` und `SEAFOX_RELEASE_KEY_PASSWORD`, aber ein signierter Kandidat ist noch nicht nachgewiesen
- Play-Console-Produkte/Preise, Trial-Regeln, produktiver Backend-Receipt-Service und echte Play-Device-QA fuer die vorhandene Play-Billing-Kauf-/Restore-Schicht
- geklaerten Karten-/Provider-Lizenzstatus fuer beworbene Features
- Store-Wahrheit: seaFOX als Marine-Assistent/Dashboard, nicht als zertifiziertes ECDIS; Safety Contour nur als Assistenzfunktion, bis echte ENC-Fixtures und Device-Screenshots vorliegen

## Monetization Notes

Empfohlene Stufen:

- Free: Simulator, Basis-Dashboard, begrenzte Widgets, Online-/Open-Chart-Ansicht.
- Pro: Offline-Pakete, volle Widget-Konfiguration, Routen/Tracks, AIS-CPA, MOB, Ankerwache, Laylines, Trendkurven.
- Navigator: erweiterte Kartenpakete, Safety-Contour, erweiterte Alarme, Export/Import, Bootprofile.
- Fleet/Commercial: mehrere Boote, Support, Diagnosepakete, MDM-/Tablet-Setup, Lizenzverwaltung.

Kartenlizenzen sind getrennt von App-Feature-Entitlements zu behandeln. Der aktuelle Stand ist Domain-/Gatewaylogik mit JVM-Tests: `BillingCatalog.kt` kennt aktive App-Abo-Produkt-IDs fuer Pro, Navigator und Fleet (`seafox.pro.monthly/yearly`, `seafox.navigator.monthly/yearly`, `seafox.fleet.monthly/yearly`) sowie ein first-party Play-`INAPP` fuer `seafox-premium-de-coast` (`seafox.chartpack.de_coast`). `seafox.chart.cmap.external` und `seafox.chart.s63.external` bleiben inaktive externe Chart-License-Placeholder. `PlayBillingClientGateway.kt` kann ProductDetails laden, Google-Play-Kaufdialoge starten, aktive Subscriptions und One-Time-In-App-Produkte fuer Restore abfragen und Purchases acknowledgen. `PlayBillingPurchaseMapper.kt` uebersetzt echte Play-`Purchase`-Objekte in interne, standardmaessig unverified Records. `BillingValidationJson.kt` parst Backend-Antworten defensiv zu Token-Decisions; `BillingValidationHttpClient.kt` sendet Validation-Requests optional an `SEAFOX_BILLING_VALIDATION_URL`. `BillingRestoreCoordinator.kt` erzeugt Backend-Validation-Requests und laesst fehlende oder abgelehnte Validation nicht freischalten. `BillingRuntimeRestoreApplier.kt` uebernimmt nur vollstaendig validierte Kauf-/Restore-Ergebnisse in den lokalen `EntitlementSnapshot`; der UI-Pfad `Abo & Karten` zeigt Status, startet Kaufbuttons fuer aktive Katalogprodukte und kann Restore ausfuehren. `FirstPartyChartPackages.kt` erkennt canonical MBTiles-Dateien fuer das first-party Pack und der Dialog zeigt gekauft/nicht installiert/installiert/abgelaufen. `BillingEntitlementMapper.kt` gewaehrt Entitlements nur fuer verifizierte `purchased` Records und markiert unacknowledged Tokens. First-party Kartenpakete setzen nur `ownedChartPackIds`, keine App-Stufe und keine externen Provider-Lizenzen. `FeatureAccessPolicy.kt` mappt Widgets und Premiumfunktionen auf benoetigte App-Features; `RuntimeEntitlementGate.kt` blockiert Premium-Widget-Erstellung zur Laufzeit und `DashboardState` persistiert den Snapshot. Play-Console-Produkte/Preise, Trial-Regeln, produktiver Server-/Receipt-Backendservice, Premium-Pack-Auslieferung/Download, echte Play-Device-QA und vollstaendige Runtime-Enforcement-Pfade sind noch offen. C-MAP/S-63 duerfen bis zur geklaerten Lizenz-, Zertifikats-, Permit- und Provider-Integration nicht als kaufbare oder freigeschaltete Verkaufsfeatures behandelt werden.

## Privacy and Safety Drafts

- `docs/PRIVACY_POLICY_DRAFT.md` beschreibt Bootsdaten, Netzwerkdaten, Geraetedaten und optionale Supportdaten; lokale Speicherung ist Standard, Backups bleiben privat, oeffentliche Exporte brauchen Nutzeraktion.
- Sensible Daten sind unter anderem MMSI, Route, MOB-Position, GPS-/Trackdaten, Router-Host, NMEA-Quellen und Diagnoseinformationen. Diagnose-Shares redigieren Router-Host, MMSI, Route und MOB standardmaessig und duerfen erst nach Nutzer-Consent ueber FileProvider/Sharesheet geteilt werden.
- seaFOX soll ohne verpflichtenden Cloud-Account funktionieren. Lokale Crash-Reports werden privat in App-Dateien geschrieben; Cloud-Backup, Telemetrie, externes Crash-Reporting oder Support-Upload brauchen vor Aktivierung Nutzerinformation und dokumentierte Rechtsgrundlage.
- Offen vor Store-Release: Loeschkonzept, Exportkonzept, Kontaktweg, Impressum, Rechtsgrundlage, Drittanbieter-Liste und juristische Finalisierung.
- `docs/SAFETY_DISCLAIMER_DRAFT.md` setzt die Produktwahrheit: seaFOX ist kein zertifiziertes ECDIS, kein amtliches Navigationssystem und kein Ersatz fuer zugelassene Seekarten, Ausguck, Seemannschaft oder eigenverantwortliche Navigation.
- Autopilot-, Safety-Contour-, CPA/TCPA-, MOB-, Routen-, Layline-, Alarm- und Tiefenfunktionen bleiben Assistenzfunktionen und muessen vom Skipper geprueft werden.

## Important Links

- [[chart-system]]
- [[dashboard-and-widgets]]
- [[data-ingestion-and-safety]]
- [[open-questions]]
