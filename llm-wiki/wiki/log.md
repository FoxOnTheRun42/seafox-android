---
title: seaFOX LLM Wiki Log
type: log
status: current
updated: 2026-04-24
sources:
  - ../AGENTS.md
---

# seaFOX LLM Wiki Log

Chronologisches Protokoll fuer Ingests, Queries, Lint-Passes und groessere Wiki-Pflege. Eintraege sind append-only.

## [2026-04-24] implementation | Runtime premium widget render lock

- Sources: `../app/src/main/java/com/boat/dashboard/MainActivity.kt`, `../app/src/main/java/com/boat/dashboard/data/RuntimeEntitlementGate.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/data/RuntimeEntitlementGateTest.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/data/FeatureAccessPolicyTest.kt`, `../docs/PRODUCTION_READINESS.md`, `../docs/QA_MATRIX.md`, `../docs/RELEASE_CHECKLIST.md`
- Updated: `../docs/PRODUCTION_READINESS.md`, `../docs/QA_MATRIX.md`, `../docs/RELEASE_CHECKLIST.md`, `wiki/modules/data-ingestion-and-safety.md`, `wiki/modules/production-and-qa.md`, `wiki/open-questions.md`, `wiki/log.md`
- Notes: Bestehende Premium-Widgets werden im Dashboard jetzt gegen den aktuellen `EntitlementSnapshot` geprueft. Nach Free-Restore, Downgrade oder abgelaufenem Snapshot bleiben AIS/Anchor Watch/NMEA/System-Cards verschiebbar und loeschbar, rendern aber nur einen Lock-Placeholder statt Premium-Funktion; der System-Performance-Sampler laeuft nur mit entsperrtem System-Widget. Gezielter Compile plus `RuntimeEntitlementGateTest` war gruen; das volle `./scripts/seafox-product-check.sh --ci --release-r8` war ebenfalls gruen. Device-/Play-QA bleibt offen.

## [2026-04-24] docs | Designer Briefs in docs/ eingetragen

- Sources: externe Claude-Chat-Session mit Delta-Briefs, bisher ausserhalb des Repos
- Updated: `../docs/DESIGNER_BRIEF_CHART_EXTENSION.md`, `../docs/DESIGNER_BRIEF_NMEA_SETUP.md`, `../docs/DESIGNER_BRIEF_CALIBRATION.md`, `../docs/DESIGNER_BRIEFS_INDEX.md`, `wiki/modules/dashboard-and-widgets.md`, `wiki/log.md`
- Notes: Drei Delta-Briefs zum Master `UI_UX_DESIGNER_BRIEF.md` ins Repo eingetragen. Chart-Extension-Design und NMEA-Setup-Design sind auf externem Designer-Canvas umgesetzt, Compose-Code fehlt noch. Kalibrierungs-Brief ist geschrieben, Design noch nicht begonnen. Neuer Index `DESIGNER_BRIEFS_INDEX.md` erklaert Status und Leihreihenfolge fuer kuenftige Agenten-Sessions. Alle Delta-Briefs folgen bestehenden Tokens und Safety-Regeln; Konflikte werden zurueckgemeldet statt einseitig entschieden.

## [2026-04-24] implementation | Premium pack install status

- Sources: `../app/src/main/java/com/boat/dashboard/ui/widgets/chart/FirstPartyChartPackages.kt`, `../app/src/main/java/com/boat/dashboard/MainActivity.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/FirstPartyChartPackagesTest.kt`
- Updated: `wiki/modules/chart-system.md`, `wiki/modules/production-and-qa.md`, `wiki/log.md`
- Notes: First-party Premium-Packs haben canonical MBTiles-Dateinamen und eine lokale Discovery. `Abo & Karten` zeigt Kartenpakete jetzt als `Nicht gekauft`, `Gekauft, Paketdatei fehlt`, `Installiert` oder `Lizenz abgelaufen`. Damit bleibt ein gekauftes, aber nicht ausgeliefertes Pack bewusst nicht als renderbare Karte sichtbar. Echte Download-Auslieferung/Backend-URL bleibt offen.

## [2026-04-24] implementation | Play Billing purchase flow

- Sources: `../app/src/main/java/com/boat/dashboard/data/PlayBillingClientGateway.kt`, `../app/src/main/java/com/boat/dashboard/data/BillingCatalog.kt`, `../app/src/main/java/com/boat/dashboard/MainActivity.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/data/BillingCatalogTest.kt`
- Updated: `wiki/modules/data-ingestion-and-safety.md`, `wiki/modules/production-and-qa.md`, `wiki/log.md`
- Notes: `Abo & Karten` zeigt aktive Katalogprodukte als Kaufbuttons. Das Gateway fragt `ProductDetails`, startet `launchBillingFlow` und leitet `PurchasesUpdated` in dieselbe Backend-Validation/Runtime-Applier-Pipeline wie Restore. Ohne Play-Console-Produkt oder ohne `SEAFOX_BILLING_VALIDATION_URL` entsteht keine Freischaltung. Play-Console-Setup, Preise, produktiver Backend-Service und Device-Play-QA bleiben offen.

## [2026-04-24] implementation | Billing runtime restore UI

- Sources: `../app/src/main/java/com/boat/dashboard/MainActivity.kt`, `../app/src/main/java/com/boat/dashboard/data/BillingRuntimeRestoreApplier.kt`, `../app/src/main/java/com/boat/dashboard/data/BillingValidationHttpClient.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/data/BillingRuntimeRestoreApplierTest.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/data/BillingValidationHttpClientTest.kt`, `../docs/BILLING_BACKEND_CONTRACT.md`
- Updated: `wiki/modules/data-ingestion-and-safety.md`, `wiki/modules/production-and-qa.md`, `wiki/log.md`
- Notes: `Abo & Karten` im Datenmenue zeigt den lokalen Entitlement-Snapshot und startet Play-Restore fuer App-Abos und first-party Kartenpakete. Runtime-Applier schreibt nur vollstaendig validierte Restore-Ergebnisse in den App-State; fehlender `SEAFOX_BILLING_VALIDATION_URL`, missing backend decisions und pending Kaeufe schalten nichts frei und ueberschreiben bestehende Freischaltungen nicht. Kauf-Flow, Play-Console-Produkte, produktiver Backend-Service und Device-QA bleiben offen.

## [2026-04-24] implementation | Runtime entitlement widget gate

- Sources: `../app/src/main/java/com/boat/dashboard/data/RuntimeEntitlementGate.kt`, `../app/src/main/java/com/boat/dashboard/data/WidgetModels.kt`, `../app/src/main/java/com/boat/dashboard/data/DashboardRepository.kt`, `../app/src/main/java/com/boat/dashboard/ui/DashboardViewModel.kt`, `../app/src/main/java/com/boat/dashboard/MainActivity.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/data/RuntimeEntitlementGateTest.kt`
- Updated: `wiki/modules/data-ingestion-and-safety.md`, `wiki/modules/production-and-qa.md`, `wiki/log.md`
- Notes: `EntitlementSnapshot` wird jetzt im `DashboardState` persistiert. `DashboardViewModel.addWidget` blockiert Premium-Widgets ueber `RuntimeEntitlementGate`; Deny-Messages nennen die benoetigte Stufe und sagen klar, dass Kartenpakete/Lizenzen keine App-Features freischalten. Kauf-UI, echter Backend-HTTP-Client und vollstaendige Runtime-Abdeckung aller Premiumaktionen bleiben offen.

## [2026-04-24] implementation | Billing validation JSON contract

- Sources: `../app/src/main/java/com/boat/dashboard/data/BillingValidationJson.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/data/BillingValidationJsonTest.kt`, `../docs/BILLING_BACKEND_CONTRACT.md`
- Updated: `wiki/modules/data-ingestion-and-safety.md`, `wiki/modules/production-and-qa.md`, `wiki/log.md`
- Notes: Backend-Validation-Antworten werden defensiv in `BillingValidationDecision`s geparst: `verified`, `rejected`, sonst `unverified`; unbekannte Statuswerte schalten nichts frei. Serialisierung echoed keine Purchase Tokens. Weiterhin kein echter HTTP-Client oder Play-Console-Setup.

## [2026-04-24] implementation | Play Billing purchase mapper

- Sources: `../app/src/main/java/com/boat/dashboard/data/PlayBillingPurchaseMapper.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/data/PlayBillingPurchaseMapperTest.kt`, `../docs/BILLING_BACKEND_CONTRACT.md`
- Updated: `wiki/modules/data-ingestion-and-safety.md`, `wiki/modules/production-and-qa.md`, `wiki/log.md`
- Notes: Echte Google-Play-`Purchase`-Objekte werden jetzt in interne `BillingPurchaseRecord`s uebersetzt und starten standardmaessig als `unverified`. Pending/acknowledged/Product-ID-Normalisierung sind JVM-getestet. Backend-Validation bleibt noetig, bevor Entitlements freigeschaltet werden.

## [2026-04-24] implementation | Billing restore validation seam

- Sources: `../app/src/main/java/com/boat/dashboard/data/BillingRestoreCoordinator.kt`, `../app/src/main/java/com/boat/dashboard/data/BillingEntitlementMapper.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/data/BillingRestoreCoordinatorTest.kt`, `../docs/BILLING_BACKEND_CONTRACT.md`
- Updated: `wiki/modules/data-ingestion-and-safety.md`, `wiki/modules/production-and-qa.md`, `wiki/log.md`
- Notes: Restore-/Servervalidierungs-Bruecke als reine Domainlogik ergaenzt: Play-Restore-Daten erzeugen `BillingValidationRequest`s, fehlende Serverantworten bleiben `unverified`, verified/rejected Decisions werden vor dem Entitlement-Mapping gemerged. Verified first-party Chart-Pack setzt nur `ownedChartPackIds`; C-Map/S-63 bleiben inaktiv. Kein Netzwerkclient, keine Kauf-UI und keine Runtime-Freischaltung.

## [2026-04-24] implementation | Crash metadata in support diagnostics

- Sources: `../app/src/main/java/com/boat/dashboard/CrashReporting.kt`, `../app/src/main/java/com/boat/dashboard/data/SupportDiagnostics.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/CrashReportFormatterTest.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/data/SupportDiagnosticsBuilderTest.kt`
- Updated: `wiki/modules/data-ingestion-and-safety.md`, `wiki/modules/production-and-qa.md`, `wiki/log.md`
- Notes: Lokale Crash-Reports bleiben private App-Dateien. Support-Diagnose erhaelt nur sichere Inventory-Metadaten (`crashReportCount`, `latestCrashReportAtEpochMs`), keine Stacktraces, Exception-Messages oder Crash-Dateiinhalte. Gezielte JVM-Tests fuer Crash-Inventory und Support-Diagnose-Felder sind vorgesehen/ausgefuehrt im Implementierungsblock.

## [2026-04-24] docs | Support diagnostics share flow contract

- Sources: `../docs/PRODUCTION_READINESS.md`, `../docs/QA_MATRIX.md`, `../docs/RELEASE_CHECKLIST.md`, `wiki/modules/data-ingestion-and-safety.md`, `wiki/modules/production-and-qa.md`
- Updated: `../docs/PRODUCTION_READINESS.md`, `../docs/QA_MATRIX.md`, `../docs/RELEASE_CHECKLIST.md`, `wiki/modules/data-ingestion-and-safety.md`, `wiki/modules/production-and-qa.md`, `wiki/log.md`
- Notes: Dokumentation fuer neuen user-facing Support-Diagnose-Share-Flow nachgezogen: Nutzer-Consent vor Share, JSON im App-Cache, FileProvider/Android-Sharesheet, Default-Redaction fuer Router-Host, MMSI, Route und MOB. Kein automatischer Upload, kein Backend-Triage-Versprechen und noch kein Device-QA-Nachweis. Keine Source-Dateien angefasst und gemaess Auftrag keine Tests/Builds ausgefuehrt.

## [2026-04-24] implementation | Chart Roadmap Task 04 renderer skeleton

- Sources: `../app/src/main/java/com/boat/dashboard/ui/widgets/chart/enc/EncRendererSkeleton.kt`, `../app/src/main/java/com/boat/dashboard/ui/widgets/chart/S57CellSelector.kt`, `../app/src/main/java/com/boat/dashboard/ui/widgets/chart/s57/S57ChartProvider.kt`, `../app/src/main/java/com/boat/dashboard/ui/widgets/chart/s57/S57ToGeoJson.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/enc/EncRendererSkeletonTest.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/S57CellSelectorTest.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/s57/S57ToGeoJsonTest.kt`
- Updated: `wiki/modules/chart-system.md`, `wiki/modules/production-and-qa.md`, `wiki/open-questions.md`, `wiki/log.md`
- Notes: Task 04 als sicheres Skeleton begonnen: Plain S-57 bleibt Beta/non-ECDIS, oeSENC bleibt explizit `notImplemented`, S-57-Zellenauswahl ist wiederverwendbar, und ein duenner `S57ChartProvider` kann spaeter in die Provider-Registry/Runtime migriert werden. Gezielte JVM-Tests fuer Renderer-Capability, Layer-Rollen, SCAMIN, SOUNDG, oeSENC-Rejection und Provider-Notices sind gruen. Kein echter S-52-/oeSENC-Renderer, kein Device-Screenshot und keine Safety-Contour-Releasefreigabe.

## [2026-04-24] implementation | Chart Roadmap Task 03 premium pack start

- Sources: `../app/src/main/java/com/boat/dashboard/data/BillingCatalog.kt`, `../app/src/main/java/com/boat/dashboard/data/BillingEntitlementMapper.kt`, `../app/src/main/java/com/boat/dashboard/data/EntitlementModels.kt`, `../app/src/main/java/com/boat/dashboard/data/PlayBillingClientGateway.kt`, `../app/src/main/java/com/boat/dashboard/ui/widgets/chart/FirstPartyChartPackages.kt`, `../docs/BILLING_BACKEND_CONTRACT.md`
- Updated: `wiki/modules/chart-system.md`, `wiki/modules/production-and-qa.md`, `wiki/modules/data-ingestion-and-safety.md`, `wiki/open-questions.md`, `wiki/log.md`
- Notes: Task 03 als Billing-/Entitlement-Fundament umgesetzt: `seafox.chartpack.de_coast` ist ein first-party Play-`INAPP`, setzt nur `ownedChartPackIds = ["seafox-premium-de-coast"]`, Query-Restore kann Subscriptions und In-App-Produkte zusammenfuehren, und `FirstPartyChartPackages` markiert gekaufte Pakete ohne lokale Datei weiterhin als `incomplete`. C-Map/S-63 bleiben inaktive externe Platzhalter. Gezielter JVM-Check war gruen; Play-Console, Kauf-UI, Backend-Verifikation und Paket-Auslieferung fehlen weiterhin.

## [2026-04-24] implementation | Chart Roadmap Task 02 sideload start

- Sources: `../app/src/main/java/com/boat/dashboard/ui/widgets/chart/SeaChartSideLoadPackages.kt`, `../app/src/main/java/com/boat/dashboard/ui/widgets/chart/OfflineTileManager.kt`, `../app/src/main/java/com/boat/dashboard/ui/widgets/chart/ChartWidget.kt`, `../app/src/main/java/com/boat/dashboard/MainActivity.kt`, `../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/SeaChartSideLoadPackagesTest.kt`
- Updated: `wiki/modules/chart-system.md`, `wiki/modules/production-and-qa.md`, `wiki/open-questions.md`, `wiki/index.md`, `wiki/log.md`
- Notes: Task 02 gestartet: lokaler Android-Dateiimport fuer MBTiles/GeoPackage, app-interne Kopie in Provider-Ordner, Raster-MBTiles als renderbar, Vector-MBTiles/GeoPackage validiert aber nicht als renderbar beworben. Gezielter Compile/JVM-Test war gruen; Emulator-/Device-SAF-Flow und echter Render-Screenshot fehlen weiterhin wegen fehlendem `adb`.

## [2026-04-24] implementation | Chart Roadmap Task 01 free providers

- Sources: `../app/src/main/java/com/boat/dashboard/ui/widgets/chart/FreeRasterChartProviders.kt`, `../app/src/main/java/com/boat/dashboard/ui/widgets/chart/ChartProviderRegistry.kt`, `../app/src/main/java/com/boat/dashboard/ui/widgets/chart/ChartWidget.kt`, `../app/src/main/java/com/boat/dashboard/MainActivity.kt`, `../docs/PRODUCTION_READINESS.md`, `../docs/QA_MATRIX.md`, `../docs/THIRD_PARTY_NOTICES.md`
- Updated: `wiki/modules/chart-system.md`, `wiki/modules/production-and-qa.md`, `wiki/open-questions.md`, `wiki/index.md`, `wiki/log.md`
- Notes: Task 01 der Chart Roadmap umgesetzt: QMAP DE als freie Online-Raster-Beta, OpenSeaCharts mit internem OSM-Fallback plus erzwungenem OpenSeaMap-Seamark-Overlay, OSM bewusst nicht als eigener Seekartenprovider. Gezielter JVM-Check fuer Provider-Registry, FreeRaster-Vertraege und Settings-Normalisierung ist gruen; volles `./scripts/seafox-product-check.sh --ci --release-r8` ist ebenfalls gruen. Device-/Emulator-QA bleibt blockiert, weil `adb` nicht installiert ist.

## [2026-04-24] refresh | Productization, chart truth and release gates

- Sources: `../runs/20260424-095641-ceo-sync/brief.md`, `../docs/PRODUCTION_READINESS.md`, `../docs/QA_MATRIX.md`, `../docs/RELEASE_CHECKLIST.md`, `../docs/PRIVACY_POLICY_DRAFT.md`, `../docs/SAFETY_DISCLAIMER_DRAFT.md`, `../app/src/main/java/com/boat/dashboard/**`, `../app/src/test/**`
- Updated: `wiki/project-overview.md`, `wiki/architecture.md`, `wiki/modules/chart-system.md`, `wiki/modules/data-ingestion-and-safety.md`, `wiki/modules/production-and-qa.md`, `wiki/open-questions.md`, `wiki/index.md`, `wiki/sources/chart-code-refresh-2026-04-24.md`, `wiki/sources/product-safety-refresh-2026-04-24.md`, `wiki/sources/product-gate-refresh-2026-04-24.md`
- Notes: Wiki nach parallelem Agenten-Refresh auf aktuellen Stand gebracht: Safety Contour als Placeholder/Contract statt reale ENC-Contour, Boot-Autostart-Risiko, Billing/Entitlements als Domain-/Kataloglogik, SupportDiagnostics mit JSON/File-Utility ohne UI-Share-Flow, Release-/Privacy-/Safety-Drafts und frisches gruenes `./scripts/seafox-product-check.sh --ci` ohne Device-QA.

## [2026-04-24] ingest | Initial seaFOX project wiki

- Sources: `AGENTS.md`, `CLAUDE.md`, `README.md`, `docs/PRODUCTION_READINESS.md`, `docs/QA_MATRIX.md`, `docs/UI_ASSET_REDESIGN_PLAN.md`, Gradle config, `scripts/seafox-product-check.sh`
- Updated: `wiki/index.md`, `wiki/project-overview.md`, `wiki/architecture.md`, `wiki/modules/chart-system.md`, `wiki/modules/dashboard-and-widgets.md`, `wiki/modules/data-ingestion-and-safety.md`, `wiki/modules/production-and-qa.md`, `wiki/open-questions.md`, `wiki/sources/project-docs-ingest-2026-04-24.md`
- Notes: Erste Wissensbasis fuer Projektorientierung, Architektur, Karten, QA und Produktreife angelegt.

## [2026-04-24] ingest | Karpathy LLM Wiki pattern

- Sources: https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f
- Updated: `llm-wiki/README.md`, `llm-wiki/AGENTS.md`, `wiki/sources/karpathy-llm-wiki-pattern.md`, `wiki/index.md`
- Notes: Pattern auf seaFOX adaptiert: raw sources, gepflegte Markdown-Wiki-Schicht, Index, Log und Agenten-Schema.

## [2026-04-24] ingest | Production hardening sync

- Sources: `README.md`, `.github/workflows/android-ci.yml`, `app/build.gradle.kts`, `scripts/seafox-product-check.sh`, `docs/PRODUCTION_READINESS.md`, `docs/QA_MATRIX.md`, `docs/RELEASE_CHECKLIST.md`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/boat/dashboard/BootCompletedReceiver.kt`, `app/src/main/java/com/boat/dashboard/data/BillingCatalog.kt`, `app/src/main/java/com/boat/dashboard/data/SupportDiagnostics.kt`, `app/src/test/java/com/seafox/nmea_dashboard/data/BillingCatalogTest.kt`, `app/src/test/java/com/seafox/nmea_dashboard/data/SupportDiagnosticsBuilderTest.kt`
- Updated: `wiki/index.md`, `wiki/modules/production-and-qa.md`, `wiki/modules/data-ingestion-and-safety.md`, `wiki/sources/product-gate-refresh-2026-04-24.md`, `wiki/sources/product-safety-refresh-2026-04-24.md`
- Notes: Dauerhaftes Wissen zu Release-R8 als CI-/Store-Gate, BillingCatalog als Domain-Katalog ohne Play-Billing, SupportDiagnostics JSON/File-Export im Datenlayer und weiterhin offenem Boot-Autostart-Opt-in-Risiko nachgezogen. `./scripts/seafox-product-check.sh --ci` wurde erneut lokal gruen ausgefuehrt; kein lokaler R8- oder Device-Test in diesem Wiki-Sync.

## [2026-04-24] ingest | Boot policy, feature access and chart fixtures

- Sources: `app/src/main/java/com/boat/dashboard/BootAutostartPolicy.kt`, `app/src/test/java/com/seafox/nmea_dashboard/BootAutostartPolicyTest.kt`, `app/src/main/java/com/boat/dashboard/data/FeatureAccessPolicy.kt`, `app/src/test/java/com/seafox/nmea_dashboard/data/FeatureAccessPolicyTest.kt`, `app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/HazardOverlayBuilderTest.kt`, `scripts/seafox-product-check.sh`
- Updated: `wiki/modules/data-ingestion-and-safety.md`, `wiki/modules/production-and-qa.md`, `wiki/sources/product-safety-refresh-2026-04-24.md`, `wiki/sources/product-gate-refresh-2026-04-24.md`
- Notes: Boot-Autostart-Code-Bypass fuer Unlock/internal Delayed Launch ist per Policy und JVM-Test gehaertet; echte Device-Boot-QA fehlt weiterhin. FeatureAccessPolicy ergaenzt Domain-Gating fuer Widgets/Premiumfunktionen. HazardOverlayBuilder-Fixtures decken DEPARE/DEPCNT/SOUNDG-Tiefenfilter ab. Lokales `--ci --release-r8` Gate wurde bestanden.

## [2026-04-24] ingest | Play Billing gateway and local crash reports

- Sources: `app/build.gradle.kts`, `app/src/main/java/com/boat/dashboard/data/PlayBillingClientGateway.kt`, `app/src/main/java/com/boat/dashboard/data/BillingEntitlementMapper.kt`, `app/src/test/java/com/seafox/nmea_dashboard/data/BillingEntitlementMapperTest.kt`, `app/src/main/java/com/boat/dashboard/CrashReporting.kt`, `app/src/test/java/com/seafox/nmea_dashboard/CrashReportFormatterTest.kt`, `docs/BILLING_BACKEND_CONTRACT.md`
- Updated: `wiki/modules/data-ingestion-and-safety.md`, `wiki/modules/production-and-qa.md`
- Notes: Play Billing Library 8.3.0 ist ueber das Java-Artefakt eingebunden; KTX wurde wegen Kotlin-Metadata-Inkompatibilitaet vermieden. Restore-Mapping gewaehrt Entitlements erst nach verifiziertem Kaufstatus. Lokale Crash-Reports schreiben private App-Dateien ohne Cloud-Upload. Full Gate `--ci --release-r8` blieb gruen.
