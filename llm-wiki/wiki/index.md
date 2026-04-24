---
title: seaFOX Wiki Index
type: index
status: current
updated: 2026-04-24
sources:
  - ../../AGENTS.md
  - ../../README.md
  - ../../docs/PRODUCTION_READINESS.md
  - ../../docs/QA_MATRIX.md
  - ../../docs/RELEASE_CHECKLIST.md
  - ../../docs/PRIVACY_POLICY_DRAFT.md
  - ../../docs/SAFETY_DISCLAIMER_DRAFT.md
  - ../../docs/UI_ASSET_REDESIGN_PLAN.md
  - ../../runs/20260424-095641-ceo-sync/brief.md
---

# seaFOX Wiki Index

Dies ist der Einstieg in das LLM-gepflegte Projektwissen fuer seaFOX. Lies diese Seite zuerst, dann die passenden Detailseiten. Der Code und die verlinkten Projektdateien bleiben die Source of Truth.

## Core Pages

| Page | Purpose |
| --- | --- |
| [[project-overview]] | Produktziel, aktueller Stand, wichtigste Einstiegspunkte |
| [[architecture]] | Systemarchitektur, Datenfluss, Modulgrenzen |
| [[modules/chart-system]] | Karten, MapLibre, S-57/ENC, Overlays, Provider-Roadmap |
| [[modules/dashboard-and-widgets]] | Compose-Dashboard, Widget-System, Designrichtung |
| [[modules/data-ingestion-and-safety]] | NMEA, AIS, GPS, BLE, Autopilot, Privacy/Safety-Gates |
| [[modules/production-and-qa]] | Produktreife, Gates, Tests, Release-Risiken |
| [[open-questions]] | Offene Projektfragen und Pruefpunkte |

## Source Notes

| Page | Source |
| --- | --- |
| [[sources/karpathy-llm-wiki-pattern]] | Karpathy LLM-Wiki-Pattern, als Arbeitsmuster adaptiert |
| [[sources/project-docs-ingest-2026-04-24]] | Erste Ingestion aus Projektcodex, README und Docs |
| [[sources/chart-code-refresh-2026-04-24]] | Aktueller Chart-/Navigation-Codecheck: Safety Contour, Provider, Route/MOB |
| [[sources/product-safety-refresh-2026-04-24]] | Aktuelle Product-/Safety-Pruefung: Boot-Autostart, Entitlements, Diagnostics, Billing |
| [[sources/product-gate-refresh-2026-04-24]] | Aktueller Product-Gate-/QA-Refresh: Tests, Release-Gates, Privacy/Safety-Drafts |

## Fast Retrieval

- Build/Product Gate/Release-R8: [[modules/production-and-qa]]
- Karten/ENC/S-57/Safety Contour: [[modules/chart-system]]
- Free-Provider/QMAP DE/OpenSeaMap/OSM-Fallback: [[modules/chart-system]]
- Widgets/UI/Assets: [[modules/dashboard-and-widgets]]
- NMEA/AIS/Autopilot/Privacy/Boot-Autostart: [[modules/data-ingestion-and-safety]]
- BillingCatalog/Monetarisierung/Release: [[modules/production-and-qa]] und [[modules/data-ingestion-and-safety]]
- SupportDiagnostics JSON/File Export: [[modules/data-ingestion-and-safety]]
- Boot-Autostart/Billing/Support-Diagnostics-Risiken: [[modules/data-ingestion-and-safety]]
- Store-/Privacy-/Safety-Drafts: [[modules/production-and-qa]]

## Maintenance

- Letztes Log: siehe [[log]]
- Wiki-Schema: `../AGENTS.md`
- Suchbefehl: `rg -n "Suchbegriff" llm-wiki/wiki`
