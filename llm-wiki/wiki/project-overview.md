---
title: seaFOX Project Overview
type: overview
status: current
updated: 2026-04-24
sources:
  - ../AGENTS.md
  - ../../AGENTS.md
  - ../../README.md
  - ../../docs/PRODUCTION_READINESS.md
  - ../../docs/QA_MATRIX.md
  - ../../docs/RELEASE_CHECKLIST.md
  - ../../docs/PRIVACY_POLICY_DRAFT.md
  - ../../docs/SAFETY_DISCLAIMER_DRAFT.md
  - ../../runs/20260424-095641-ceo-sync/brief.md
  - ../../app/src/main/java/com/boat/dashboard/data/BillingCatalog.kt
  - ../../app/src/main/java/com/boat/dashboard/data/EntitlementModels.kt
  - ../../app/src/main/java/com/boat/dashboard/data/SupportDiagnostics.kt
---

# seaFOX Project Overview

## Summary

seaFOX ist eine Android-App fuer Boots-Dashboards mit NMEA2000-/NMEA0183-naher Telemetrie, frei konfigurierbaren Widgets, MapLibre-basierter Seekartenansicht, Offline-Navigation und Produktambition Richtung verkaufbare Marine-App.

Der aktuelle Stand ist Alpha/Productization: Die App hat starke Feature-Substanz, aber der Fokus liegt laut CEO-Sync vom 2026-04-24 nicht auf breiter neuer Oberflaeche, sondern auf Integration, Wahrheit von Produktversprechen und Runtime-/QA-Nachweisen. Store-, Billing-, Device-QA-, Kartenlizenz- und Release-Schiene sind noch nicht voll belastbar.

Dieses LLM-Wiki ist Arbeitsgedaechtnis fuer Agenten, nicht Source of Truth. Fuer Code-, Safety-, Lizenz-, Datenschutz- oder Release-Entscheidungen muessen die verlinkten Projektdateien und der aktuelle Code erneut geprueft werden.

## Key Facts

- Projektroot: Repository-Root des `seaFOX`-Checkouts
- Application ID / Namespace: `com.seafox.nmea_dashboard`
- Tech Stack: Android, Kotlin, Jetpack Compose, MapLibre GL Native, Gradle Kotlin DSL.
- Zentrale App-Datei: `app/src/main/java/com/boat/dashboard/MainActivity.kt`
- Zentrales ViewModel: `app/src/main/java/com/boat/dashboard/ui/DashboardViewModel.kt`
- Zentrale Daten-/Persistenzpfade: `NmeaNetworkService.kt`, `DashboardRepository.kt`, `DalyBmsBleManager.kt`
- Produkt-Gate: `./scripts/seafox-product-check.sh`
- Release-Gate-Dokumente: `docs/PRODUCTION_READINESS.md`, `docs/QA_MATRIX.md`, `docs/RELEASE_CHECKLIST.md`
- Safety-/Privacy-Drafts: `docs/SAFETY_DISCLAIMER_DRAFT.md`, `docs/PRIVACY_POLICY_DRAFT.md`

## Product Shape

Die App soll ein vollwertiges Boots-Cockpit werden:

- frei platzierbare Dashboard-Widgets
- Seiten-/Layoutverwaltung
- NMEA-Netzwerkdaten und Simulator
- GPS, AIS, Autopilot, BLE-BMS und Supportdiagnostik
- SeaChart-Widget mit Online-/Offline-Karten, ENC/S-57-Ansaetzen und Overlays
- Entitlement-Domainlogik fuer Free, Pro, Navigator und Fleet
- Billing-Katalog fuer aktive App-Abos und inaktive externe Kartenlizenz-Platzhalter

## CEO Sync / Current Operating Mode

Der CEO-Sync vom 2026-04-24 trennt zwei aktive Arbeitsbahnen:

- Productization / Safety: Onboarding, Backup-Privacy, Boot-Autostart, Entitlement Policy, Support Diagnostics, Autopilot Safety Gate und Produktionsdokumente.
- Chart / Navigation: Chart Provider Registry, Safety-Contour-Policy, Route/MOB/Guard-Zone/Layline-Overlays, Fullscreen-Chart und reale Kartenbeweise.

Die Entscheidung: keine breiten neuen Produktflaechen, bis diese Bahnen ihren Integrationsnachweis haben. Der letzte dokumentierte Product Check lief fuer `./scripts/seafox-product-check.sh --ci` gruen; Emulator-/Device-QA blieb lokal blockiert, weil `adb` nicht verfuegbar war.

## Current Product Readiness

Laut `docs/PRODUCTION_READINESS.md` ist seaFOX noch nicht verkaufsreif. Die wichtigsten Blocker sind:

- keine Store-faehige Release-, Signing- und Billing-Schiene
- keine belastbare Emulator-/Device-Testautomatisierung
- kommerzielle Karten- und Lizenzstrategie offen
- S-57/S-52-nahe Kartendarstellung noch nicht amtlich oder zertifiziert
- Safety Contour ist noch nicht als vollstaendig realer ENC-Contour-Renderer bewiesen
- UI/UX muss fuer Cockpit-Bedienung weiter vereinfacht und validiert werden
- Crash-/Telemetry-/Support-Prozesse fehlen noch als Betriebssystem

Aktuell vorhandene Productization-Bausteine:

- First-run-Onboarding, Datenschutz-/Bootmodus-Einstellungen und Fullscreen-Chart sind im Compose-Code vorhanden.
- Autopilot-Kommandos laufen ueber ein Safety Gate.
- Backup-Privacy und Boot-Autostart sind als Produktzustaende modelliert; Boot-Autostart braucht weiter Device-Nachweis.
- Support Diagnostics haben Builder, JSON-Vertrag und internen File-Exporter mit JVM-Tests; Export-/Share-/Triage-Workflow bleibt Release-Gate.
- `BillingCatalog` und `EntitlementPolicy` sind getestet. Das ist Domain-/Vorbereitungsarbeit: Play Billing, Kaufwiederherstellung, Trial/Server-Validierung und UI-Freischaltung sind noch offen.
- App-Feature-Entitlements sind von Kartenlizenzen getrennt. C-Map/S-63 sind inaktive beziehungsweise nicht auswaehlbare Platzhalter, bis Lizenz, Zertifikate, Entitlements und Nutzungsbedingungen geklaert sind.
- `ChartProvider`/`ChartProviderRegistry` markieren Provider-Faehigkeiten und Availability; die Runtime nutzt aber noch weitgehend den bestehenden `SeaChartMapProvider`-/Download-Pfad. Das ist teilweise Skeleton-/Migrationsarbeit, nicht fertige Provider-Architektur.

## Wiki Role

`llm-wiki/` dient als Arbeitsgedaechtnis fuer Agenten: schnelle Orientierung, offene Pruefpunkte und Synthese aus Repo-Dokumenten. Es ersetzt keine Primaerquelle. Besonders bei Marine-Safety, Monetarisierung, Datenschutz, Store-Texten, Kartennutzung und Release-Freigaben gelten Code, Tests, `docs/**` und der konkrete Product-Gate-Lauf.

## Important Links

- [[architecture]]
- [[modules/chart-system]]
- [[modules/dashboard-and-widgets]]
- [[modules/data-ingestion-and-safety]]
- [[modules/production-and-qa]]
- [[open-questions]]
