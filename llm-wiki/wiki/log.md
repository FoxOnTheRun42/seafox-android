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
