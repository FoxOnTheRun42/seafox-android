---
title: Chart Code Refresh 2026-04-24
type: source
status: current
updated: 2026-04-24
sources:
  - ../../../AGENTS.md
  - ../../../docs/PRODUCTION_READINESS.md
  - ../../../runs/20260424-095641-ceo-sync/brief.md
  - ../../../app/src/main/java/com/boat/dashboard/ui/widgets/chart/
  - ../../../app/src/main/java/com/boat/dashboard/MainActivity.kt
  - ../../../app/src/main/java/com/boat/dashboard/ui/DashboardViewModel.kt
  - ../../../app/src/main/java/com/boat/dashboard/data/WidgetModels.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/
---

# Chart Code Refresh 2026-04-24

## Summary

Diese Quellen-Notiz aktualisiert den Stand der Chart-/Navigation-Wiki-Seite anhand der Projektquellen vom 2026-04-24. Sie ist keine neue Source of Truth; sie dokumentiert nur, welche Claims beim Refresh geprueft wurden.

## Sources Read

- `../AGENTS.md`: Kartenarchitektur, Einstiegspunkte, alte Implementierungsnotizen und offene Punkte.
- `../docs/PRODUCTION_READINESS.md`: Produkt-Gates fuer Kartenrecht, S-57/S-52, Safety Contour, Offline Provider und Testpflicht.
- `../runs/20260424-095641-ceo-sync/brief.md`: aktuelle Chart-Lane-Aufgabe mit Safety-Contour-Truthfulness und ChartProvider-Runtime-Gap.
- `../app/src/main/java/com/boat/dashboard/ui/widgets/chart/*.kt`: ChartWidget, NavigationOverlay, Route/Hazard Builder, SafetyContourPolicy, OpenSeaMapOverlay, Provider Registry und NauticalOverlay.
- `../app/src/main/java/com/boat/dashboard/MainActivity.kt`, `DashboardViewModel.kt`, `WidgetModels.kt`: read-only cross-check fuer Route-/MOB-Runtime-Wiring.
- `../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/*.kt`: vorhandene JVM-Tests fuer GeoCalc, SafetyContourPolicy, HazardOverlayBuilder und ChartProviderRegistry.
- `../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/SeaChartWidgetSettingsModuleTest.kt`: persistierte Provider-Normalisierung fuer Legacy/OpenSeaMap, C-Map, S-63 und unbekannte Provider.

## Key Findings

- Safety Contour ist noch kein echter ENC-Contour-Renderer. Der aktuelle sichtbare Pfad ist ein `safety-contour-contract` Feature mit `placeholder = true`; `SafetyContourPolicy` und `HazardOverlayBuilder` liefern nur Policy-/Filter-Bausteine.
- `NauticalOverlay` rendert S-57/ENC-Daten als MapLibre-GeoJSON-Layer inklusive DEPARE, DEPCNT und SOUNDG-nahem Rendering, aber diese Layer sind nicht als Safety-Contour-Warnpfad validiert.
- `ChartProvider` ist eine gute Ziel-Abstraktion, aber der echte Runtime-Pfad nutzt weiter `SeaChartMapProvider` plus direkte Verdrahtung in `MainActivity`, `NauticalOverlay`, `OpenSeaMapOverlay` und `OfflineTileManager`.
- `ChartProviderRegistry` ist trotzdem wertvoll, weil C-Map und S-63 nicht als auswaehlbare Produktfeatures erscheinen, solange Lizenz/Implementierung fehlen. Persistierte SeaChart-Settings laufen ebenfalls durch eine Normalisierung und fallen fuer C-Map/S-63 auf NOAA zurueck.
- OpenSeaMap ist als Online-Raster-Seamark-Overlay verdrahtet. Es ist nicht gleichbedeutend mit einer offiziellen Offline-ENC.
- Route- und MOB-State sind aktueller als die aelteren AGENTS-Notizen: `activeRoute`, `mobPosition`, `activeRouteAsNavRoute()` und `ChartWidget`-Parameter existieren. Visuelle/device QA fehlt trotzdem.
- Laylines koennen nur erscheinen, wenn `showLaylines` aktiv ist und `trueWindDirectionDeg` geliefert wird.

## Checks

- Codepfade wurden per `sed`/`rg` gelesen; es wurden keine App-Dateien editiert.
- Wiki-Update beruehrt nur `wiki/modules/chart-system.md` und diese Quellen-Notiz.
- Kein Build/Testlauf fuer diese Dokumentationsaenderung. Fuer Release-Claims weiterhin `./scripts/seafox-product-check.sh --ci`, Screenshot-Plan und Device-/Emulator-QA verlangen.

## Open Follow-Ups

- Offen bleibt ein Focused JVM-Test fuer die Form des Safety-Contour-Contract-Features sowie ein Screenshot-/Fixture-Nachweis fuer echte ENC-Visualisierung.
- Eine reproduzierbare NOAA/S-57- oder GeoJSON-Fixture fuer DEPARE/DEPCNT/SOUNDG.
- Screenshot- oder emulatorgestuetzter Beweis fuer Route, MOB, Laylines und Safety-Contour-Warnpfad.

## Related Pages

- [[modules/chart-system]]
- [[modules/production-and-qa]]
- [[open-questions]]
