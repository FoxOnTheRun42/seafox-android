---
title: Project Docs Initial Ingest
type: source
status: current
updated: 2026-04-24
sources:
  - ../../../AGENTS.md
  - ../../../CLAUDE.md
  - ../../../README.md
  - ../../../docs/PRODUCTION_READINESS.md
  - ../../../docs/QA_MATRIX.md
  - ../../../docs/UI_ASSET_REDESIGN_PLAN.md
  - ../../../build.gradle.kts
  - ../../../app/build.gradle.kts
  - ../../../scripts/seafox-product-check.sh
---

# Project Docs Initial Ingest

## Summary

Diese Ingestion fasst die vorhandenen seaFOX-Projektdokumente und Build-Dateien zu einer ersten LLM-Wiki-Wissensbasis zusammen. Die resultierenden Seiten sind Startpunkte, keine vollstaendige Codeanalyse.

## Sources Read

- `AGENTS.md`: Projektcodex, Architektur, Einstiegspunkte, Kartenanforderungen, Implementierungsstatus.
- `CLAUDE.md`: aeltere/alternative Agentenorientierung.
- `README.md`: App-Ziel, Datenformate, PGNs, Build-/Testbefehle.
- `docs/PRODUCTION_READINESS.md`: Produktstand, Defizite, Monetarisierung, Release-Gates.
- `docs/QA_MATRIX.md`: Testmatrix und aktuelle Abdeckung.
- `docs/UI_ASSET_REDESIGN_PLAN.md`: Premium-Marine-Cockpit-Designrichtung.
- Gradle-Dateien und `scripts/seafox-product-check.sh`: Build- und Gate-Fakten.

## Pages Created

- [[project-overview]]
- [[architecture]]
- [[chart-system]]
- [[dashboard-and-widgets]]
- [[data-ingestion-and-safety]]
- [[production-and-qa]]
- [[open-questions]]

## Checks

- Claims zu konkretem Verhalten sollten vor Codeaenderungen gegen die genannten Kotlin-Dateien geprueft werden.
- Produkt-/Lizenz-/Safety-Aussagen stammen aus Projekt-Dokumenten und sind keine rechtliche Bewertung.
