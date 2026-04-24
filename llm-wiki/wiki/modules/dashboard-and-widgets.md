---
title: Dashboard and Widgets
type: module
status: current
updated: 2026-04-24
sources:
  - ../../../AGENTS.md
  - ../../../README.md
  - ../../../docs/UI_ASSET_REDESIGN_PLAN.md
  - ../../../app/src/main/java/com/boat/dashboard/MainActivity.kt
  - ../../../app/src/main/java/com/boat/dashboard/SeaFoxDesignTokens.kt
---

# Dashboard and Widgets

## Summary

Das Dashboard ist eine Jetpack-Compose-Oberflaeche mit mehreren Seiten und frei positionierbaren Widgets. Die Widgets sind canvas-basiert und sollen wie ein praezises, hochwertiges Marine-Cockpit wirken, nicht wie Standard-Material-Controls.

## Current Capabilities

- Mehrseitiges Dashboard mit horizontalem Wischen.
- Seiten erstellen und umbenennen.
- Widgets verschieben und skalieren.
- Simulator-Toggle.
- Zustand ueber `DashboardRepository` / `SharedPreferences`.
- Widget-Katalog mit 21 Widget-Typen in Navigation, Sicherheit, System, Energie/Tanks und Antrieb.

## Widget Sections

- Navigation: Wind, Kompass, Karten, Speed/Log, GPS, PGN-Empfang, Autopilot, Deep/Echolot, Temperatur.
- Sicherheit: AIS, Ankerwache.
- System: NMEA0183-Empfang, Systemauslastung, DALY BMS.
- Energie/Tanks: Wassertank, Schwarzwasser, Grauwasser, Batterie.
- Antrieb: Motordrehzahl.

## Design Direction

Aus `docs/UI_ASSET_REDESIGN_PLAN.md`:

- Premium marine cockpit.
- Ruhige, praezise, unter Stress lesbare Oberflaeche.
- Matte Graphit-/Mineralglas-Anmutung, warme Safety-Akzente, kalte Signal-Akzente.
- Code-native Icons, Gauges und Instrumentmarken fuer Schaerfe.
- Generierte Rasterassets nur fuer Materialqualitaet und Hintergruende.

## Known UX/Product Work

- Viele Funktionen liegen noch in dichten Popups.
- Cockpit-Bedienung braucht konsequent grosse Touch-Ziele.
- Dashboard-Modus, Vollbild-Kartenmodus, Bottom-Sheets und Statusleiste sollen klarer getrennt werden.
- Widget-Editor braucht Snap-Grid, Lock/Edit, Undo, Presets und Bootstyp-Vorlagen.

## QA Hooks

- Screenshot-Tests fuer Widgets auf Phone und Tablet.
- Rotationstests fuer Portrait/Landscape.
- Emulator-Flows fuer Onboarding, Simulator, Dashboard-Edit und Chart.
- Manuelle Cockpit-/Sonnenlicht-Lesbarkeit fuer Tag/Nacht-Themes.

## Important Links

- [[chart-system]]
- [[production-and-qa]]
