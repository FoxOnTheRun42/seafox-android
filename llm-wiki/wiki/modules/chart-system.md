---
title: Chart System
type: module
status: current
updated: 2026-04-24
sources:
  - ../../../AGENTS.md
  - ../../../docs/PRODUCTION_READINESS.md
  - ../../../runs/20260424-095641-ceo-sync/brief.md
  - ../../../app/src/main/java/com/boat/dashboard/ui/widgets/chart/ChartWidget.kt
  - ../../../app/src/main/java/com/boat/dashboard/ui/widgets/chart/NavigationOverlay.kt
  - ../../../app/src/main/java/com/boat/dashboard/ui/widgets/chart/NavHazardOverlayBuilder.kt
  - ../../../app/src/main/java/com/boat/dashboard/ui/widgets/chart/HazardOverlayBuilder.kt
  - ../../../app/src/main/java/com/boat/dashboard/ui/widgets/chart/SafetyContourPolicy.kt
  - ../../../app/src/main/java/com/boat/dashboard/ui/widgets/chart/ChartProviderRegistry.kt
  - ../../../app/src/main/java/com/boat/dashboard/ui/widgets/chart/FreeRasterChartProviders.kt
  - ../../../app/src/main/java/com/boat/dashboard/ui/widgets/chart/SeaChartSideLoadPackages.kt
  - ../../../app/src/main/java/com/boat/dashboard/ui/widgets/chart/OpenSeaMapOverlay.kt
  - ../../../app/src/main/java/com/boat/dashboard/ui/widgets/chart/RouteOverlayBuilder.kt
  - ../../../app/src/main/java/com/boat/dashboard/ui/widgets/chart/NauticalOverlay.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/FreeRasterChartProvidersTest.kt
  - ../../../app/src/test/java/com/seafox/nmea_dashboard/ui/widgets/chart/SeaChartSideLoadPackagesTest.kt
  - ../sources/chart-code-refresh-2026-04-24.md
---

# Chart System

## Summary

Das Kartensystem ist ein fortgeschrittener Alpha-Baustein, aber noch kein produktreifes oder ECDIS-konformes Navigationssystem. MapLibre rendert die Karte, `NauticalOverlay` kann lokale S-57/ENC-Dateien zu GeoJSON-Layern machen, und `NavigationOverlay` legt AIS-, Track-, Route-, MOB-, Guard-Zone-, Layline- und Safety-Contour-Contract-Features darueber.

Chart Roadmap Task 01 ist umgesetzt: QMAP DE ist als freie Online-Raster-Beta angebunden, `OPEN_SEA_CHARTS` verwendet einen internen OSM-Orientierungsfallback plus erzwungenes OpenSeaMap-Seamark-Overlay, und OSM wird bewusst nicht als eigener Seekartenprovider angeboten.

Chart Roadmap Task 02 ist teilweise produktiv umgesetzt: Raster-MBTiles koennen per Android-Dateiauswahl sideloaded, validiert, app-intern gespeichert und offline gerendert werden. Vector-MBTiles und GeoPackage werden validiert/gespeichert, aber bewusst als noch nicht renderbar markiert.

Die wichtigste Wahrheit fuer Produkt- und Release-Kommunikation: Safety Contour ist aktuell ein Contract-/Placeholder-Pfad plus Depth-Filter-Policy, keine bewiesene echte ENC-Contour-Visualisierung aus realen DEPARE/DEPCNT/SOUNDG-Zellen.

## Key Files

- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/ChartWidget.kt`
- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/ChartStyle.kt`
- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/NauticalOverlay.kt`
- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/AisOverlay.kt`
- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/NavigationOverlay.kt`
- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/RouteOverlayBuilder.kt`
- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/NavHazardOverlayBuilder.kt`
- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/HazardOverlayBuilder.kt`
- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/SafetyContourPolicy.kt`
- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/OpenSeaMapOverlay.kt`
- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/FreeRasterChartProviders.kt`
- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/SeaChartSideLoadPackages.kt`
- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/ChartProvider.kt`
- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/ChartProviderRegistry.kt`
- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/OfflineTileManager.kt`
- `app/src/main/java/com/boat/dashboard/ui/widgets/chart/s57/`

## Current Behavior

- `ChartWidget` owns the MapLibre `MapView`, day/night style switching, fullscreen dialog, follow-position toggle, MOB button, viewport callbacks and overlay re-application after style changes.
- `NauticalOverlay` scans `.000` ENC files, optionally uses `CATALOG.031` and camera/zoom bounds to prefer cells, converts selected S-57 files through `S57ToGeoJson`, and applies MapLibre layers for land, depth areas, depth contours, buoys, beacons, lights, obstructions, soundings, restricted areas and infrastructure.
- `FreeRasterChartProviders` bridges the first free online raster providers into the MapLibre runtime: QMAP DE uses `https://freenauticalchart.net/qmap-de/{z}/{x}/{y}.png`; `OPEN_SEA_CHARTS` gets an OSM standard raster fallback plus forced seamark overlay; NOAA/S-57/S-63/C-Map do not get this raster override.
- `SeaChartSideLoadPackages` validates local `.mbtiles`, `.gpkg` and `.geopackage` files. Raster-MBTiles are renderable now; Vector-MBTiles and GeoPackage are accepted as imported packages but not rendered yet.
- The chart download dialog has a local import action using Android `OpenDocument`; imported packages are copied into the selected provider's app-specific `seaCHART/<provider>/sideload_*` folder before rendering or status display.
- `ChartWidget` now prefers the selected active MBTiles source over the global first-found MBTiles and removes the free online raster basemap when a local MBTiles package is active.
- `OpenSeaMapOverlay` is implemented as an idempotent raster seamark overlay using `https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png`; selecting `OPEN_SEA_CHARTS` forces that overlay path and does not load a local ENC path.
- `NavigationOverlay` renders heading, COG, predictor marks/labels, course line, boat symbol and recent track from own-position telemetry.
- `RouteOverlayBuilder` builds GeoJSON for route legs, waypoints, active/future/past status, XTE corridor and ETA labels. Current app code passes an active route into `ChartWidget`, so this is no longer merely an isolated builder, but it still needs runtime screenshot/device proof.
- `NavHazardOverlayBuilder` builds laylines, guard zones, MOB marker/link and safety-contour contract features. MOB state is now wired through `ChartWidget` parameters and the chart MOB button; laylines depend on true-wind direction and chart settings.
- `SafetyContourPolicy` calculates effective safety depth from draft, margin and configured depth, and identifies shallow DEPARE/DEPCNT/SOUNDG-like features through `HazardOverlayBuilder` filters.
- `ChartProviderRegistry` gates selectable providers: NOAA, QMAP DE, S-57 and OpenSeaCharts are selectable/beta/available; C-Map is license-required and not selectable; S-63 is not implemented and not selectable. Persisted `SeaChartWidgetSettings` provider names are normalized through the same gate, so stored C-Map/S-63 values fall back to NOAA, legacy QMAP values become QMAP DE, and legacy OSM values become OpenSeaCharts instead of silently becoming a separate provider.

## Honest Gaps

- Safety Contour: `NavigationOverlay` draws a point-like `safety-contour-contract` marker at own ship when enabled. That is useful as an integration contract, but it is not a contour line, shaded safety area or alarm path derived from real ENC geometry.
- ENC visualization: `NauticalOverlay` has real S-57-to-GeoJSON rendering, including DEPARE and DEPCNT layers, but the current Safety Contour path is not connected to those rendered layers as a verified safety visual.
- ChartProvider skeleton vs runtime: `ChartProvider`, `ChartData`, `ChartProviderType` and `OfflineChartPackage` define the future provider abstraction. `ChartProviderRegistry` now controls selectable legacy `SeaChartMapProvider` values, but runtime loading still mostly goes through the enum/source-path/download code in `MainActivity`, `NauticalOverlay`, `OpenSeaMapOverlay` and `OfflineTileManager`, not through concrete `ChartProvider` implementations.
- OpenSeaMap: seamark raster overlay is wired and selectable, but it is an online tile overlay, not a licensed offline ENC provider or official navigation source.
- QMAP DE/OSM: QMAP DE is online raster only in Task 01; OSM is an internal orientation fallback, not a user-facing sea-chart provider, offline package or bulk-prefetch source.
- Route/MOB/Laylines: builders and main chart wiring exist. Release confidence still requires user-flow proof for route creation/selection, MOB trigger/clear behavior on device, and laylines with a reliable true-wind source.
- S-57/S-52: S-57 parsing/rendering exists, but styling is not S-52/ECDIS compliant. Product copy must avoid official-navigation claims.
- Offline/format breadth: Raster-MBTiles sideloading exists. Vector MBTiles/PBF, GeoPackage rendering, tile-directory rendering, PMTiles, WMS/WMTS, BSB/KAP, GPX/KML, S-63 and C-Map are not production-ready provider implementations.

## Sources / Checks

- Checked `../AGENTS.md`: chart architecture and older status list still identify MapLibre, S-57, OpenSeaMap, Route/MOB/Layline builders and ChartProvider abstraction as core chart work.
- Checked `../docs/PRODUCTION_READINESS.md`: release gate explicitly says S-57 is not S-52/ECDIS conformant and Safety Contour lacks a full rendered path from real ENC cells plus fixture screenshots.
- Checked `../runs/20260424-095641-ceo-sync/brief.md`: current lane handoff says Safety Contour is placeholder/contract unless connected to real ENC/GeoJSON rendering, and ChartProvider is not yet the real runtime path.
- Checked chart code under `../app/src/main/java/com/boat/dashboard/ui/widgets/chart`: route/MOB/layline/hazard builders exist; Safety Contour contract emits a placeholder feature; OpenSeaMap emits a raster tile layer; provider interfaces/registry exist.
- Checked unit-test surface: `ChartProviderRegistryTest`, `FreeRasterChartProvidersTest`, `SeaChartSideLoadPackagesTest`, `SeaChartWidgetSettingsModuleTest`, `SafetyContourPolicyTest`, `HazardOverlayBuilderTest` and `GeoCalcTest` cover provider gating, free online raster contracts, MBTiles/GeoPackage sideload filename contracts, persisted provider normalization, safety-depth policy, DEPARE/DEPCNT/SOUNDG shallow filtering and geometry math. Missing test coverage remains for exact SAF import UI, real MBTiles render screenshots, the safety-contour contract feature shape and real rendered ENC screenshots.
- Targeted JVM check after Task 01 passed: `./gradlew :app:testDebugUnitTest --tests '*ChartProviderRegistryTest' --tests '*FreeRasterChartProvidersTest' --tests '*SeaChartWidgetSettingsModuleTest'`.
- Full local product gate after Task 01 passed: `./scripts/seafox-product-check.sh --ci --release-r8`. Device/emulator QA still did not run because `adb` is not installed.

## Contradictions / Checks

- `../AGENTS.md` contains older 2026-04-05 notes saying route and MOB app-state were missing. Current code has `DashboardViewModel.activeRouteAsNavRoute()`, persisted `activeRoute`, `mobPosition`, and `ChartWidget` MOB toggle wiring. Treat the older notes as stale unless a runtime test disproves the current wiring.
- `NavHazardOverlayBuilder` names the safety feature `safety-contour-contract` and sets `placeholder = true`; any UI or documentation calling it a real contour is misleading until the ENC-derived visual path is added and captured.
- `ChartProviderRegistry` makes provider selection and persisted provider settings safer, but it should not be described as the completed provider architecture until concrete providers implement `ChartProvider.loadForRegion()` and `applyToStyle()` in the runtime path.

## Open Questions

- What real ENC fixture should become the canonical Safety Contour proof case: NOAA sample cell, local `seaCHART/ENC_ROOT` cell, or a checked-in minimal GeoJSON fixture?
- Should Safety Contour render as a highlighted DEPCNT line, shallow DEPARE fill, alarm corridor, or a separate warning overlay derived from `HazardOverlayBuilder.filterDepthAreaFeatures`?
- Where should concrete `ChartProvider` implementations live, and what is the migration order for MBTiles raster, tile directories, S-57, OpenSeaMap/QMAP raster and future vector-tile providers?
- Should GeoPackage be rendered through a local tile adapter, converted to MBTiles on import, or deferred until the provider registry has concrete package handlers?
- What is the user-facing route creation/import path for normal users: manual waypoint editor, GPX/KML import, or only persisted debug/sample routes for now?
- Does MOB need a persistent, always-visible cockpit control outside the chart widget, and what confirmation/clear behavior is acceptable on phone and tablet?
- Which true-wind signal is authoritative for laylines, and how should the UI show stale or missing wind data?
- What screenshot/emulator/device checklist is sufficient before marking chart lane release-ready?

## Important Links

- [[data-ingestion-and-safety]]
- [[production-and-qa]]
- [[open-questions]]
