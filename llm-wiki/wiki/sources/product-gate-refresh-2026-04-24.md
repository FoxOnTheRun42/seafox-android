---
title: Product Gate Refresh 2026-04-24
type: source
status: current
updated: 2026-04-24
sources:
  - ../../../README.md
  - ../../../scripts/seafox-product-check.sh
  - ../../../.github/workflows/android-ci.yml
  - ../../../app/build.gradle.kts
  - ../../../runs/20260424-095641-ceo-sync/brief.md
  - ../../../docs/PRODUCTION_READINESS.md
  - ../../../docs/QA_MATRIX.md
  - ../../../docs/RELEASE_CHECKLIST.md
  - ../../../docs/PRIVACY_POLICY_DRAFT.md
  - ../../../docs/SAFETY_DISCLAIMER_DRAFT.md
  - ../../../app/src/main/java/com/boat/dashboard/data/BillingCatalog.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/FeatureAccessPolicy.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/SupportDiagnostics.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/BootAutostartPolicyTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/data/BillingCatalogTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/data/FeatureAccessPolicyTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/data/SupportDiagnosticsBuilderTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/HazardOverlayBuilderTest.kt
  - ../../../app/src/test
---

# Product Gate Refresh 2026-04-24

## Sources Read

- `README.md`: dokumentiert `./scripts/seafox-product-check.sh --ci --release-r8` als Store-Kandidaten-Check.
- `scripts/seafox-product-check.sh`: lokales Product Gate fuer statische Projektgesundheit, Compile, JVM-Tests, Lint, optional Release-R8 und optional Device-Pflicht.
- `.github/workflows/android-ci.yml`: CI fuehrt das Product Gate mit `--ci --release-r8` aus und archiviert Lint-/R8-Reports.
- `app/build.gradle.kts`: Release-BuildType nutzt R8/Resource-Shrinking und optionale Env-basierte Signing-Konfiguration.
- `runs/20260424-095641-ceo-sync/brief.md`: heutiges Gate-Ergebnis und Produkt-/Chart-Risiken.
- `docs/PRODUCTION_READINESS.md` und `docs/QA_MATRIX.md`: aktualisierte Produkt- und QA-Gates.
- `docs/RELEASE_CHECKLIST.md`: Store-Kandidaten-Checklist fuer Release-R8, Signing, Store-Wahrheit, Device-QA und Rollback.
- `docs/PRIVACY_POLICY_DRAFT.md`: technischer Datenschutzentwurf fuer Store- und Rechtspruefung.
- `docs/SAFETY_DISCLAIMER_DRAFT.md`: Safety-Disclaimer-Entwurf fuer App/Store.
- `BillingCatalog.kt`, `BillingCatalogTest.kt` und `app/src/test`: aktuelle JVM-Testdateien fuer Data-, Billing-, Entitlement-, Diagnostics- und Chart-Domainlogik.
- `FeatureAccessPolicy.kt`: Domain-Gating fuer Widgets und Premiumfunktionen.
- `SupportDiagnostics.kt`: redigierter Diagnosebericht, JSON-Vertrag und interner Datei-Export.

## Gate Facts

- Das berichtete Gate `./scripts/seafox-product-check.sh --ci` ist gruen fuer statische Checks, `:app:compileDebugKotlin`, `:app:testDebugUnitTest` und `:app:lintDebug`.
- Der gleiche Gate-Befehl wurde im Wiki-Refresh nach den Billing-/Diagnostics-Aenderungen erneut lokal ausgefuehrt und blieb gruen fuer Compile, JVM-Tests und Lint.
- `adb` ist in der lokalen Umgebung nicht installiert. Dadurch wurde keine Emulator-/Device-QA ausgefuehrt.
- Das Script kann `--device` erzwingen und wuerde dann ohne `adb` fehlschlagen.
- `--release-r8` fuehrt `:app:minifyReleaseWithR8` aus und ist laut README, QA-Matrix, Release-Checklist und CI-Workflow das verpflichtende Gate fuer Store-Kandidaten.
- Der CI-Workflow `Android Product Gate` laeuft auf Pull Requests, Pushes zu `main`, `master`, `develop`, `codex/**` und manuell; er nutzt Java 17 und Android SDK 35.
- Nach den Produktionshaertungen wurde `./scripts/seafox-product-check.sh --ci --release-r8` lokal ausgefuehrt und bestand Compile, JVM-Tests, Lint und `:app:minifyReleaseWithR8`. Das ist ein Shrinker-/Gate-Nachweis, kein signiertes Store-Artefakt.
- `docs/RELEASE_CHECKLIST.md` macht `./scripts/seafox-product-check.sh --ci --release-r8` fuer Store-Kandidaten verbindlich.
- Release-Signing ist ueber `SEAFOX_RELEASE_STORE_FILE`, `SEAFOX_RELEASE_STORE_PASSWORD`, `SEAFOX_RELEASE_KEY_ALIAS` und `SEAFOX_RELEASE_KEY_PASSWORD` vorgesehen; `app/build.gradle.kts` setzt die SigningConfig nur, wenn alle Werte vorhanden sind. Ein signierter Kandidat ist noch offen.

## Test Inventory

- `AutopilotSafetyGateTest`: Safety-Arming, explizite Bestaetigung, Broadcast-Host-Blockade und Timeout.
- `BackupPrivacyPolicyTest`: Public-Backup-Regeln und sensitive Keys.
- `BootAutostartPolicyTest`: Boot-/Unlock-/interner Delayed-Launch-Opt-in.
- `BillingCatalogTest`: Produkt-ID-Eindeutigkeit, aktive Pro-/Navigator-/Fleet-App-Subscriptions, case-/whitespace-tolerantes Lookup und inaktive C-MAP/S-63-Chart-Placeholders ohne Tier-/Provider-Freigabe.
- `EntitlementPolicyTest`: Feature-Tiers, externe Chart-Lizenzen und Ablaufzeit.
- `FeatureAccessPolicyTest`: Widget-/Feature-Zugriff fuer Free/Pro/Navigator/Fleet.
- `SupportDiagnosticsBuilderTest`: Redaction, optionale sensitive Felder, JSON-Vertrag, Seiten-/Widget-/Safety-Zusammenfassung und Datei-Export in ein bereitgestelltes Verzeichnis; kein belegter user-facing Share-Flow.
- `ChartProviderRegistryTest`: selektierbare/beta Provider, C-MAP lizenzpflichtig, S-63 nicht implementiert.
- `HazardOverlayBuilderTest`: DEPARE/DEPCNT/SOUNDG-Fixtures und Tiefenalias-/Fallback-Filter.
- `GeoCalcTest`: nautische Kernberechnungen.
- `SafetyContourPolicyTest`: Safety-Depth und Tiefenfeature-Filter.

## Open Release Risk

- Compile/Test/Lint sind gruen, aber Runtime-QA fehlt.
- Emulator-/Device-QA fehlt wegen fehlendem `adb`.
- Release-Gates fuer Play Billing, Release-Signing, Store-/Beta-Flow und Karten-/Provider-Licensing bleiben offen.
- Entitlement und Billing sind aktuell Domain-/Kataloglogik; harte kaufbasierte App-Enforcement-Pfade sind nicht belegt.
- Safety Contour und kommerzielle Kartenprovider brauchen reale Fixture-/Screenshot-/Lizenznachweise, bevor sie als verkaufsfertig gelten.
- Privacy Policy und Safety Disclaimer liegen als Entwuerfe vor, sind aber vor Verkauf juristisch zu finalisieren und in App/Store zu uebernehmen.
- Store-Wahrheit: seaFOX ist als Marine-Assistent/Dashboard zu beschreiben, nicht als zertifiziertes ECDIS oder alleinige Navigationsquelle.

## Related Pages

- [[modules/production-and-qa]]
- [[modules/data-ingestion-and-safety]]
- [[open-questions]]
