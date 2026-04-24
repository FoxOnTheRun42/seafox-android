# seaFOX — AGENTS.md Projektcodex

## Projektidentität

- **App-Name:** seaFOX
- **Package / Application ID:** `com.seafox.nmea_dashboard`
- **Typ:** Android-App (Kotlin, Jetpack Compose, MapLibre GL Native)
- **Version:** 0.2.3.x (auto-incrementing build number)
- **Ziel:** Vollwertiges NMEA2000-Boots-Dashboard mit freikonfigurierbaren Widgets, GPU-beschleunigter Seekartendarstellung, Offline-Navigation und Multi-Format-Kartenunterstützung

---

## Projektstruktur und Abgrenzung

Dieses Verzeichnis enthält **ausschließlich die Android-App**.
Eigenständige Nachbarprojekte (nie vermischen):

- `../nmea2000-adapter` — Kotlin/JVM-Gatewaylogik
- `../Daly-BMS-NMEA2000` — Firmware-/Hardwareprojekt

Build-Root: dieses Repository-Root (`seaFOX`-Checkout). Nicht aus einem Parent-Repo heraus bauen.

## Production Pipeline

Wenn ein Task Produktion, CI, QA, Evals, Release-Gates, Produktisierung oder Monetarisierung betrifft, den Skill `codex-production-pipeline` verwenden.

Wiederholbarer lokaler Gate-Befehl:

```bash
./scripts/seafox-product-check.sh
```

Der Check nutzt den Gradle Wrapper, kompiliert Debug-Kotlin, laesst JVM-Tests laufen, fuehrt Android Lint aus und prueft die wichtigsten Projekt-/Dokumentationsartefakte.

---

## LLM Wiki

Das projektinterne LLM-Wiki liegt unter `llm-wiki/` und folgt dem Karpathy-Pattern aus `llm-wiki/wiki/sources/karpathy-llm-wiki-pattern.md`:

- `llm-wiki/raw/` enthaelt unveraenderte externe Quellen, Clips, PDFs, Notizen und Assets. Diese Dateien nicht nachtraeglich umschreiben; bei Updates eine neue Quelle ablegen.
- `llm-wiki/wiki/` enthaelt die vom LLM gepflegte Wissensbasis mit `index.md`, `log.md`, Quellenzusammenfassungen und Themen-/Modulseiten.
- `llm-wiki/AGENTS.md` ist das operative Schema fuer Ingest, Query und Wiki-Lint.
- Bei Aufgaben zu Architektur, Roadmap, Produktentscheidungen, QA, Karten, NMEA oder wiederkehrendem Projektwissen nach diesem Projektcodex zuerst `llm-wiki/wiki/index.md` lesen.
- Wenn waehrend einer Aufgabe dauerhaft nuetzliches Projektwissen entsteht, passende Wiki-Seiten aktualisieren und einen Eintrag in `llm-wiki/wiki/log.md` anhaengen.
- Wiki-Seiten sind Arbeitsgedaechtnis, nicht Source of Truth. Fuer Code-, Safety-, Rechts-, Lizenz- und Release-Entscheidungen immer die referenzierten Quellen oder den Code selbst pruefen.

---

## Architektur

```
┌─────────────────────────────────────────────────────┐
│                    Datenquellen                      │
│  UDP/TCP ← NmeaNetworkService (Port 41449/custom)   │
│  GPS     ← LocationManager                          │
│  BLE     ← DalyBmsBleManager (Daly BMS 0xFFF0)     │
│  Sim     ← integrierter NMEA-Simulator              │
└─────────────┬───────────────────────────────────────┘
              ▼
┌─────────────────────────────────────────────────────┐
│              DashboardViewModel                      │
│  StateFlow<DashboardState> + NmeaUpdate-Flows        │
│  AIS-Targets, PGN-History, 0183-Classification       │
│  GPS-Location, Autopilot-Dispatch                    │
└─────────────┬───────────────────────────────────────┘
              ▼
┌─────────────────────────────────────────────────────┐
│              Compose UI (MainActivity)               │
│  HorizontalPager → Seiten → Widgets (Drag/Resize)   │
│  Hamburger-Menü, Sim-Toggle, Karten-Settings         │
└─────────────┬───────────────────────────────────────┘
              ▼
┌─────────────────────────────────────────────────────┐
│              Kartensubsystem                         │
│  ChartWidget (MapLibre GL Native)                    │
│  ├── ChartStyle (Day/Night, Online/Offline)          │
│  ├── NauticalOverlay (S-57 ENC → GeoJSON → Layer)   │
│  ├── AisOverlay (CPA/TCPA-Bedrohungsklassifikation)  │
│  ├── OfflineTileManager (MBTiles, Tile-Directories)  │
│  └── s57/ (Iso8211Parser → S57Reader → S57ToGeoJson) │
└─────────────┬───────────────────────────────────────┘
              ▼
┌─────────────────────────────────────────────────────┐
│          DashboardRepository (Persistenz)             │
│  SharedPreferences + versionierte Backups            │
│  MediaStore + Legacy-Migration                       │
└─────────────────────────────────────────────────────┘
```

---

## Einstiegspunkte (Priorität A — immer zuerst lesen)

| Datei | Rolle |
|---|---|
| `app/src/main/java/com/boat/dashboard/MainActivity.kt` | App-Start, gesamte Compose-UI, Widget-Rendering, Menüs, Karten-Download |
| `app/src/main/java/com/boat/dashboard/ui/DashboardViewModel.kt` | Zentrales ViewModel, Zustandsverwaltung, GPS, AIS, Autopilot |
| `app/src/main/java/com/boat/dashboard/data/NmeaNetworkService.kt` | NMEA-Empfang (UDP/TCP), AIS-Fragment-Reassembly |
| `app/src/main/java/com/boat/dashboard/data/DashboardRepository.kt` | Persistenz, Backup-System |

## Einstiegspunkte Karten (Priorität A bei Kartenarbeit)

| Datei | Rolle |
|---|---|
| `ui/widgets/chart/ChartWidget.kt` | MapLibre-Compose, Nav/AIS/OpenSeaMap-Verdrahtung, Zoom-Tracking |
| `ui/widgets/chart/NavigationOverlay.kt` | ✅ Heading/COG/Prediktor/Track/Boot + Route/Hazard-Delegation |
| `ui/widgets/chart/NavigationVectorSettings.kt` | ✅ Settings + Route/Waypoint/RouteLeg-Datenmodelle |
| `ui/widgets/chart/RouteOverlayBuilder.kt` | ✅ Route-Legs, Waypoints, XTE-Korridor, ETA-Labels (GeoJSON) |
| `ui/widgets/chart/NavHazardOverlayBuilder.kt` | ✅ Laylines, Guard Zones, MOB, Safety Contour (GeoJSON) |
| `ui/widgets/chart/OpenSeaMapOverlay.kt` | ✅ Seezeichen-Raster-Overlay (tiles.openseamap.org) |
| `ui/widgets/chart/GeoCalc.kt` | ✅ Geo-Utilities (Haversine, Destination, XTE, VMG, CPA) |
| `ui/widgets/chart/Catalog031Parser.kt` | ✅ CATALOG.031-Parser mit Positions-/Zoom-Scoring |
| `ui/widgets/chart/ChartProvider.kt` | ✅ Interface für universelle Kartenquellen-Abstraktion |
| `ui/widgets/chart/ChartProviderType.kt` | ✅ Enum: RASTER_TILES, VECTOR_TILES, GEOJSON, STYLE_JSON |
| `ui/widgets/chart/ChartData.kt` | ✅ Sealed interface für Provider-Payloads |
| `ui/widgets/chart/OfflineChartPackage.kt` | ✅ Offline-Paket-Beschreibung (Download, Bounds, Format) |
| `ui/widgets/chart/ChartStyle.kt` | Day/Night, Online/Offline-Fallback |
| `ui/widgets/chart/NauticalOverlay.kt` | S-57 ENC → 17 MapLibre-Layer, Zoom+Options-Parameter |
| `ui/widgets/chart/AisOverlay.kt` | ✅ CPA-Marker, COG-Prediktorlinien, CPA-Links |
| `ui/widgets/chart/OfflineTileManager.kt` | MBTiles + Tile-Directory-Support |
| `ui/widgets/chart/s57/Iso8211Parser.kt` | ISO-8211-Binärparser |
| `ui/widgets/chart/s57/S57Reader.kt` | ✅ edgeNodeIds, topologyIndicator in SpatialRef |
| `ui/widgets/chart/s57/S57ToGeoJson.kt` | ✅ Ring-Verkettung, Interior Rings, Winding Order, SCAMIN |
| `ui/widgets/chart/s57/S57ObjectCodes.kt` | IHO-Objektklassen-Dictionary |

---

## Widget-System

21 Widget-Typen, organisiert in 5 Sektionen:

| Sektion | Widgets |
|---|---|
| Navigation | Wind, Kompass, Karten, Speed/Log, GPS, PGN-Empfang, Autopilot, Deep/Echolot, Temperatur |
| Sicherheit | AIS, Ankerwache |
| System | NMEA0183-Empfang, Systemauslastung, DALY BMS |
| Energie/Tanks | Wassertank, Schwarzwasser, Grauwasser, Batterie |
| Antrieb | Motordrehzahl |

Alle Widgets sind Canvas-basiert (kein Standard-Material-UI). Design-Tokens in `SeaFoxDesignTokens.kt`.

---

## Datenpfade

- **Dashboard-State:** `SharedPreferences` → Key `dashboard_state`
- **Offlinekarten intern:** `filesDir/seaCHART`
- **Offlinekarten extern:** `/storage/emulated/0/Android/data/com.seafox.nmea_dashboard/files/seaCHART`
- **MBTiles:** `seaCHART/*.mbtiles`
- **ENC-Zellen:** `seaCHART/ENC_ROOT/**/*.000`
- **Assets:** `offline-style-day.json`, `offline-style-night.json`, `world_land_110m.geojson`

---

## ═══════════════════════════════════════════════════
## IMPLEMENTIERUNGSSTATUS (Stand: 2026-04-05)
## ═══════════════════════════════════════════════════

### Parallel-Run 1 (2026-04-04, 4 Subagenten)
- S-57 Polygon-Topologie gefixt (Ring-Verkettung, Interior Rings, Winding Order)
- SCAMIN-Filtering implementiert
- Catalog031Parser mit positionsbasiertem Scoring
- NavigationOverlay (Heading, COG, Prediktor, Track, Bootssymbol)
- GeoCalc Utility (Haversine, Destination, XTE, VMG, CPA)
- AIS-Overlay erweitert (COG-Vektoren, CPA-Marker/Links)
- SeaChartWidgetSettings + Serialisierung erweitert
- S57Reader: edgeNodeIds, topologyIndicator

### Parallel-Run 2 (2026-04-04, 10 Subagenten in 2 Wellen)
- OpenSeaMapOverlay (Seezeichen-Raster von openseamap.org)
- RouteOverlayBuilder (Route-Legs, Waypoints, XTE-Korridor, ETA-Labels)
- NavHazardOverlayBuilder (Laylines, Guard Zones, MOB, Safety Contour)
- Kamera-Zentrum-/Zoom-reaktive ENC-Auswahl via Catalog031Parser
- ChartProvider Interface + ChartData + ChartProviderType + OfflineChartPackage
- NavigationOverlay delegiert jetzt an Route/Hazard-Builder

### ✅ Build erfolgreich verifiziert (2026-04-05)
- `:app:assembleDebug` lief im Repository-Root erfolgreich durch
- Ergebnis: `BUILD SUCCESSFUL in 39s`
- APK erzeugt unter `app/build/outputs/apk/debug/app-debug.apk`
- Nur 3 technische Compile-Fixes nötig, ohne Logikänderung:
  - `local.properties`: `sdk.dir` auf vorhandenes Android-SDK korrigiert
  - `AisOverlay.kt`: `Float` → `Double` bei Point-Koordinaten
  - `ChartWidget.kt`: nullable `cameraPosition.target` sicher behandelt

### Verdrahtung verifiziert (Zeilen in MainActivity.kt)
- `SeaChartWidgetSettings` → `NavigationVectorSettings`: Z. 8473–8490
- `NauticalOverlayOptions`: Z. 8491–8495
- `Catalog031Parser.parse()`: Z. 3780
- `wrap360()` in `Widgets.kt` Z. 4260 (internal fun, same package)
- Zoom-Bucket via `addOnCameraIdleListener` in ChartWidget

---

## ═══════════════════════════════════════════════════
## KARTENSYSTEM — Architektur und Anforderungen
## ═══════════════════════════════════════════════════

### Zielarchitektur: Universeller Kartenadapter

Das Kartensystem MUSS eine einheitliche Abstraktionsschicht (`ChartProvider`) haben, über die ALLE Kartenformate angebunden werden. Jeder Provider liefert entweder:

1. **Raster-Tiles** (→ MapLibre RasterSource) oder
2. **GeoJSON-Features** (→ MapLibre GeoJsonSource mit Layerregeln) oder
3. **Vector-Tiles** (→ MapLibre VectorSource) oder
4. **Style-JSON** (→ MapLibre Style direkt)

```kotlin
interface ChartProvider {
    val id: String
    val displayName: String
    val type: ChartProviderType  // RASTER_TILES, VECTOR_TILES, GEOJSON, STYLE_JSON

    /** Prüfe ob für Region/Zoom Daten verfügbar */
    suspend fun hasCoverage(bounds: LatLngBounds, zoom: Int): Boolean

    /** Lade/aktualisiere Kartendaten für Region */
    suspend fun loadForRegion(bounds: LatLngBounds, zoom: Int): ChartData

    /** Wende Daten auf MapLibre-Style an */
    fun applyToStyle(style: Style, data: ChartData)

    /** Entferne alle Layer/Sources dieses Providers */
    fun removeFromStyle(style: Style)

    /** Verfügbare Offline-Pakete auflisten */
    suspend fun listOfflinePackages(): List<OfflineChartPackage>

    /** Offline-Paket herunterladen */
    suspend fun downloadPackage(pkg: OfflineChartPackage, progress: (Float) -> Unit)
}
```

---

### Unterstützte Kartenformate — VOLLSTÄNDIGE Liste

#### 1. S-57 ENC (IHO Transfer Standard)
- **Format:** ISO 8211 Container mit .000-Dateien
- **Quellen:** NOAA (kostenlos), UKHO, BSH, alle nationalen Hydrographischen Dienste
- **Status:** Eigener Parser vorhanden (Iso8211Parser + S57Reader + S57ToGeoJson)
- **Rendering:** → GeoJSON → MapLibre-Layer (17 Layer in NauticalOverlay)
- **Bekannte Defekte (MÜSSEN behoben werden):**
  - Polygon-Topologie: Edge-Verkettung zu geschlossenen Ringen fehlt
  - Interior Rings (USAG=2) werden ignoriert → keine Löcher in Polygonen
  - SCAMIN-Filtering fehlt → zu viele Features bei niedrigem Zoom
  - CATALOG.031-Parser fehlt → keine positionsbasierte Zellauswahl
  - Multi-Scale-Handling fehlt → kein automatisches Nachladen bei Zoom
  - Face-Topologie (TOPI) wird nicht genutzt

#### 2. S-63 (verschlüsselte S-57)
- **Format:** AES-verschlüsselte S-57-Zellen mit SA/SA-Zertifikaten
- **Quellen:** Kommerziell (Primar, IC-ENC, UKHO)
- **Implementation:** S-57-Parser wiederverwenden, Entschlüsselungsschicht davorsetzen
- **Schlüsselverwaltung:** User-Permit (HW_ID + Cell Permit) → Zell-Schlüssel → AES-Entschlüsselung
- **Hinweis:** Lizenzrechtlich komplex, erfordert IHO Data Protection Scheme Compliance

#### 3. S-100 / S-101 (Next-Gen IHO Standard)
- **Format:** GML-basiert (nicht mehr ISO 8211), HDF5 für Gridded-Daten
- **Quellen:** Zukünftiger Standard, Pilotprojekte bei NOAA, UKHO, BSH
- **Implementation:** Separater GML-Parser oder GDAL-basiert
- **Wichtige S-100 Produktspezifikationen:**
  - S-101: ENC (Ersatz für S-57)
  - S-102: Bathymetric Surface
  - S-104: Water Level Information
  - S-111: Surface Currents
  - S-122: Marine Protected Areas
  - S-123: Marine Radio Services
  - S-124: Navigational Warnings
  - S-127: Traffic Management
  - S-128: Catalogue of Nautical Products

#### 4. BSB/KAP (NOAA Raster-Seekarten)
- **Format:** BSB-Header + KAP-komprimierte Rasterdaten (Run-Length Encoding)
- **Quellen:** NOAA (kostenlos, aber auslaufend zugunsten ENC)
- **Implementation:**
  - BSB-Header parsen (Referenzpunkte, Projektion, Palette)
  - KAP-RLE dekomprimieren
  - Georeferenzierung über REF-Punkte (Polynom-Transformation)
  - → Raster-Tiles generieren → MapLibre RasterSource
- **Alternativ:** Via GDAL (`gdal_translate`) zu GeoTIFF → Tiles

#### 5. MBTiles
- **Format:** SQLite-Datenbank mit Tiles (Raster PNG/JPEG oder Vector PBF)
- **Quellen:** Jede Tile-Quelle die als MBTiles exportiert wird, OpenSeaMap, GDAL-Export
- **Status:** Grundlegend implementiert (OfflineTileManager)
- **Erweiterungen nötig:**
  - Vector-MBTiles Support (PBF-Tiles → MapLibre VectorSource)
  - Metadaten-Auswertung (bounds, minzoom, maxzoom, format)
  - Multi-MBTiles gleichzeitig (z.B. Basiskarte + Seekarten-Overlay)

#### 6. PMTiles (Cloud-Optimized Tiles)
- **Format:** Single-File Tile-Archive mit HTTP Range Requests
- **Quellen:** Protomaps, OpenFreeMap, Custom-Builds
- **Implementation:** pmtiles-Bibliothek oder eigener Reader
- **Vorteil:** Kein Tile-Server nötig, direkt von HTTP/lokaler Datei

#### 7. OpenSeaMap / OpenNauticalChart
- **Format:** Raster-Tiles (Slippy Map, z/x/y) oder Vektor-Tiles
- **Quellen:** https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png (Overlay)
- **Implementation:**
  - Online: Direkt als RasterSource mit Tile-URL
  - Offline: Tiles in MBTiles oder Tile-Directory cachen
  - Ist ein OVERLAY, braucht Basiskarte darunter
- **Tile-URLs:**
  - Seezeichen: `https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png`
  - Basiskarte: OpenStreetMap oder OpenFreeMap darunter

#### 8. C-MAP (Navico / Jeppesen)
- **Format:** Proprietäres CM93/2 (ältere) oder C-MAP Genesis (neuere)
- **Quellen:** Kommerziell (Navico, Jeppesen Marine)
- **Implementation:**
  - CM93/2: Reverse-engineered in OpenCPN, ähnlich S-57 aber eigenes Format
  - C-MAP API: REST-API mit OAuth für Genesis-Tiles (Registrierung nötig)
  - Rendering: Je nach Format → Raster oder Vektor
- **Hinweis:** Ohne offizielle API-Partnerschaft schwierig, CM93-Support nur über OpenCPN-Libraries realistisch

#### 9. Navionics (Garmin)
- **Format:** Proprietäre .db/.sql Kartendateien
- **Quellen:** Kommerziell (Garmin/Navionics)
- **Implementation:**
  - Navionics WebAPI: `https://webapp.navionics.com/tile/{z}/{x}/{y}?layer=config_1_1`
  - Sonarkarten: `layer=sonar`
  - Community-Edits: `layer=community`
  - → Direkt als RasterSource nutzbar (Online)
  - Offline: Tiles cachen in MBTiles
- **Hinweis:** Terms of Service beachten, Caching möglicherweise nicht erlaubt

#### 10. GeoTIFF (Georeferenzierte Raster)
- **Format:** TIFF mit eingebetteter Georeferenzierung (EPSG-Codes)
- **Quellen:** GEBCO (Bathymetrie), BSH, beliebige GIS-Exporte
- **Implementation:**
  - GDAL (`gdal2tiles`) → Tile-Directory oder MBTiles
  - Oder: Direkt rendern über GeoTIFF-Reader + On-the-fly Tiling
  - Empfehlung: Vorkonvertierung zu MBTiles (Offline) oder Tiles (Online)

#### 11. GPX/KML/KMZ (Tracks, Routen, Waypoints)
- **Format:** XML-basiert (GPX: GPS Exchange, KML: Google Earth)
- **Quellen:** GPS-Logger, Plotter, Google Earth, OpenCPN-Export
- **Implementation:**
  - GPX-Parser → GeoJSON → MapLibre-Layer (Tracks, Waypoints)
  - KML-Parser → GeoJSON (kml2geojson oder eigener Parser)
  - KMZ: ZIP-Archiv → KML extrahieren

#### 12. GRIB / GRIB2 (Wetter-Overlay)
- **Format:** Binäres Gitter-Format (WMO-Standard)
- **Quellen:** NOAA GFS, ECMWF, DWD (OpenData)
- **Daten:** Wind, Druck, Wellen, Strömung, Temperatur
- **Implementation:**
  - GRIB2-Parser (eccodes-lib oder eigener Minimal-Parser)
  - Daten interpolieren → Heatmap oder Windbarben-Overlay
  - Rendering: Canvas-Overlay oder GeoJSON-Punkte mit Symbolen
- **Hinweis:** Zuerst als separates Widget implementieren, dann auf Karte überlagern

#### 13. Mapbox / MapLibre Vector Tiles (MVT/PBF)
- **Format:** Protocol-Buffer-komprimierte Vektordaten im z/x/y-Schema
- **Quellen:** OpenFreeMap (bereits genutzt), MapTiler, Custom TileServer
- **Status:** Bereits implementiert als Basiskarte (OpenFreeMap liberty/dark)
- **Erweiterung:** Custom Style-JSON für nautische Darstellung

#### 14. OpenCPN-Charts (.oesenc, .oesu)
- **Format:** Verschlüsselte OpenCPN-Seekarten
- **Quellen:** o-charts.org (kommerziell, günstig)
- **Implementation:**
  - Erfordert oesenc-Plugin oder Lizenzvereinbarung
  - Alternative: OpenCPN als externen Renderer nutzen (kompliziert auf Android)
- **Hinweis:** Realistisch nur mit Plugin-Support

#### 15. WMS / WMTS / TMS (Web Map Services)
- **Format:** Standard-OGC-Webservices für Karten-Tiles
- **Quellen:** Diverse (BSH WMS, GEBCO WMTS, nationale Dienste)
- **Implementation:**
  - WMTS/TMS: Direkt als MapLibre RasterSource mit Template-URL
  - WMS: GetMap-Requests → Raster-Tiles (langsamer)
  - Beispiel-URLs:
    - BSH: `https://gdi.bsh.de/mapservice/rest/services/ENC/MapServer/WMTS/tile/...`
    - GEBCO: `https://www.gebco.net/data_and_products/gebco_web_services/web_map_service/...`

---

### Priorisierte Implementierungsreihenfolge

```
Prio 1 (MUSS — seetauglich machen):
  ├── ✅ S-57 ENC Polygon-Topologie (Ring-Verkettung, Interior, Winding)
  ├── ✅ SCAMIN-Filtering (scaminToMinZoom in buildFeatures)
  ├── ✅ CATALOG.031-Parser (Positions-/Zoom-Scoring, Kamera-Zentrum-Reaktiv)
  ├── ⚠️  Multi-Scale-Handling (Zoom-Bucket + Kamera-Zentrum da, volles Auto-Reload noch offen)
  ├── ✅ Navigationsvektoren (Heading, COG, Prediktor, Track, Bootssymbol)
  └── ✅ OpenSeaMap Overlay (OpenSeaMapOverlay.kt, Raster-Tiles verdrahtet)

Prio 2 (SOLL — Offline-Navigation):
  ├── ⚠️  Waypoint-Navigation — RouteOverlayBuilder.kt fertig (Legs, XTE, ETA), App-State fehlt
  ├── ⬚  MBTiles Vector-Support (PBF)
  ├── ⬚  ENC-Downloader (NOAA, mit Progress)
  ├── ⬚  BSB/KAP Raster-Seekarten
  ├── ⬚  WMS/WMTS-Integration
  ├── ⬚  PMTiles-Support
  ├── ⬚  GPX/KML Track-Overlay
  ├── ⚠️  Laylines — NavHazardOverlayBuilder.kt fertig, Windquelle fehlt
  ├── ⚠️  Guard Zones — NavHazardOverlayBuilder.kt fertig, UI/Toggle fehlt
  └── ⚠️  MOB-Marker — NavHazardOverlayBuilder.kt fertig, MOB-State fehlt

Prio 3 (KANN — kommerzielle Quellen):
  ├── ⬚  Navionics WebAPI Tiles
  ├── ⬚  S-63 verschlüsselte ENC
  ├── ⬚  C-MAP CM93/Genesis
  └── ⬚  OpenCPN oesenc

Prio 4 (ZUKUNFT):
  ├── ⬚  S-100/S-101 (GML-Parser)
  ├── ⬚  GRIB2 Wetter-Overlay
  └── ⬚  GeoTIFF direkt-Rendering

Infrastruktur:
  ├── ✅ ChartProvider Interface + ChartData/ChartProviderType/OfflineChartPackage
  └── ⬚  Konkrete Provider-Implementierungen (noch alles direkt verdrahtet)
```

### Legende
- ✅ = implementiert und verdrahtet
- ⚠️ = GeoJSON-Builder fertig, App-State/UI fehlt zum Aktivieren
- ⬚ = noch offen

---

## ═══════════════════════════════════════════════════
## NAVIGATIONSVEKTOREN — Overlay-System auf der Karte
## ═══════════════════════════════════════════════════

### Aktueller Stand

In `SeaChartWidgetSettings` existieren bereits:
```kotlin
val showCourseLine: Boolean = false
val courseLineBearingDeg: Float = 0f
val courseLineDistanceNm: Float = 5f
```
Diese Settings werden **nirgends gerendert**. Die gesamte Vektor-Rendering-Logik fehlt.

### Zielarchitektur: NavigationOverlay

Analog zu `AisOverlay` und `NauticalOverlay` → eigenes `NavigationOverlay`-Objekt das alle Navigationsvektoren als MapLibre-Layer rendert.

```kotlin
object NavigationOverlay {

    // ── Layer-IDs ──
    private const val HEADING_LINE_LAYER = "nav-heading-line"
    private const val COG_VECTOR_LAYER = "nav-cog-vector"
    private const val PREDICTOR_LAYER = "nav-predictor"
    private const val PREDICTOR_MARKS_LAYER = "nav-predictor-marks"
    private const val COURSE_LINE_LAYER = "nav-course-line"
    private const val BEARING_LINE_LAYER = "nav-bearing-line"
    private const val ROUTE_LINE_LAYER = "nav-route-line"
    private const val ROUTE_WPT_LAYER = "nav-route-waypoints"
    private const val ROUTE_LABEL_LAYER = "nav-route-labels"
    private const val XTE_CORRIDOR_LAYER = "nav-xte-corridor"
    private const val LAYLINE_PORT_LAYER = "nav-layline-port"
    private const val LAYLINE_STBD_LAYER = "nav-layline-stbd"
    private const val GUARD_ZONE_LAYER = "nav-guard-zone"
    private const val SAFETY_CONTOUR_LAYER = "nav-safety-contour"
    private const val MOB_MARKER_LAYER = "nav-mob-marker"
    private const val BOAT_ICON_LAYER = "nav-boat-icon"

    // ── Source-IDs ──
    private const val NAV_VECTORS_SOURCE = "nav-vectors-source"
    private const val NAV_ROUTE_SOURCE = "nav-route-source"
    private const val NAV_ZONES_SOURCE = "nav-zones-source"

    fun update(
        style: Style,
        ownLat: Float, ownLon: Float,
        headingDeg: Float?, cogDeg: Float?, sogKn: Float?,
        settings: NavigationVectorSettings,
        activeRoute: Route?,
    ) { ... }
}
```

### Vollständige Liste aller Navigationsvektoren

#### 1. Heading-Vektor (HDG)
- **Was:** Linie vom Boot in Richtung Heading (Bug)
- **Farbe:** Weiß, durchgezogen, 2px
- **Länge:** Konfigurierbar (Default: 0.5 NM Festlänge)
- **Datenquelle:** `heading` aus NMEA PGN 127250
- **Darstellung:** Gerade Linie vom Bootsmittelpunkt in Heading-Richtung
- **Hinweis:** Zeigt wohin der Bug zeigt, NICHT wohin das Boot fährt (Abdrift!)

#### 2. COG-Vektor (Course Over Ground)
- **Was:** Linie in Richtung des tatsächlichen Kurses über Grund
- **Farbe:** Gelb, gestrichelt, 2px
- **Länge:** Konfigurierbar (Default: zeitbasiert, 6 min bei aktuellem SOG)
- **Datenquelle:** `course_over_ground_true` aus PGN 129026
- **Unterschied zu HDG:** Berücksichtigt Strom und Abdrift
- **Darstellung:** Gestrichelte Linie vom Boot in COG-Richtung

#### 3. Prediktor / Time Vector
- **Was:** Zeigt wo das Boot in X Minuten sein wird (basierend auf COG+SOG)
- **Berechnung:**
  ```
  dist_nm = SOG_kn × (predictor_minutes / 60)
  pred_lat = own_lat + dist_nm/60 × cos(COG_rad)
  pred_lon = own_lon + dist_nm/(60 × cos(own_lat_rad)) × sin(COG_rad)
  ```
- **Farbe:** Gelb, durchgezogen, 3px, mit Zeitmarken
- **Zeitmarken:** Kleine Querstriche alle X Minuten (konfigurierbar: 1/3/6/10 min)
- **Labels:** Zeitstempel an jeder Marke ("12:36", "12:42", ...)
- **Settings:**
  ```kotlin
  data class PredictorSettings(
      val enabled: Boolean = true,
      val minutes: Int = 6,         // Vorhersagezeitraum
      val intervalMinutes: Int = 1,  // Abstand der Zeitmarken
      val showLabels: Boolean = true,
  )
  ```

#### 4. Kurslinie (Course Line / Bearing Line)
- **Was:** Benutzerdefinierte Peilung vom Boot aus (bereits in Settings angelegt)
- **Farbe:** Cyan, gepunktet, 1.5px
- **Länge:** `courseLineDistanceNm` (Settings, default 5 NM)
- **Richtung:** `courseLineBearingDeg` (Settings, 0-360°)
- **Nutzung:** Orientierungshilfe, Ansteuerungspeilung
- **UI:** Bearing per Zahleneingabe oder durch Tippen auf die Karte setzen

#### 5. Bootssymbol (Own Ship)
- **Was:** Eigene Position als gerichtetes Dreieck/Bootssymbol
- **Darstellung:**
  - Dreieck zeigt in Heading-Richtung
  - Größe skaliert mit Zoom (aber nie kleiner als 12px, nie größer als 40px)
  - Farbe: Blau (Day) / Hellblau (Night)
  - Dicker Rand wenn GPS-Fix aktiv
  - Transparenter Rand wenn GPS-Fix veraltet (>10s)
- **Rendering:** Als MapLibre SymbolLayer mit rotiertem Icon oder als GeoJSON mit Bearing-Property

#### 6. Waypoint-Navigation / Route
- **Was:** Geplante Route als Folge von Waypoints
- **Datenstruktur:**
  ```kotlin
  data class Waypoint(
      val id: String,
      val name: String,
      val lat: Double,
      val lon: Double,
      val passRadius: Float = 0.1f,  // NM — Radius für "Waypoint erreicht"
  )

  data class RouteLeg(
      val from: Waypoint,
      val to: Waypoint,
      val bearingDeg: Float,   // berechneter Sollkurs
      val distanceNm: Float,   // Leg-Distanz
  )

  data class Route(
      val id: String,
      val name: String,
      val waypoints: List<Waypoint>,
      val legs: List<RouteLeg>,  // berechnet aus waypoints
      val activeLegIndex: Int = 0,
  )
  ```
- **Darstellung:**
  - Leg-Linien: Magenta, durchgezogen, 2px
  - Aktiver Leg: Magenta, 3px, hervorgehoben
  - Vergangene Legs: Grau, 1px
  - Waypoints: Kreis mit Name-Label
  - Aktiver Waypoint: Größerer Kreis, pulsierend
  - Nächster WPT: Info-Overlay (Bearing, Distance, ETA)

#### 7. Cross Track Error (XTE) Korridor
- **Was:** Visualisierung der Abweichung vom geplanten Kurs
- **Berechnung:**
  ```
  XTE = asin(sin(d13/R) × sin(θ13 − θ12)) × R
  wobei:
    d13 = Distanz Boot → Start-Waypoint
    θ13 = Bearing Boot → Start-Waypoint
    θ12 = Bearing Start → Ziel-Waypoint
    R = Erdradius
  ```
- **Darstellung:**
  - Korridor um aktiven Leg: Halbtransparentes Band (±XTE-Limit)
  - XTE-Limit konfigurierbar (Default: 0.25 NM)
  - Farbe: Grün wenn innerhalb, Rot wenn außerhalb
  - Numerischer XTE-Wert als Text-Overlay

#### 8. ETA / TTG / DTW Berechnungen
- **Was:** Ankunftszeit, Restzeit, Restdistanz zum nächsten/letzten Waypoint
- **Berechnung:**
  ```
  DTW = Großkreisdistanz(Boot, nächster_WPT)
  TTG = DTW / SOG
  ETA = jetzt + TTG
  VMG = SOG × cos(Winkel zwischen COG und Bearing-to-WPT)
  TTG_vmg = DTW / VMG  // realistischer bei Gegenwind
  ```
- **Darstellung:** Text-Overlay auf der Karte oder in separatem Nav-Info-Widget
- **Settings:**
  ```kotlin
  data class EtaSettings(
      val useVmg: Boolean = true,   // VMG statt SOG für ETAs
      val showOnChart: Boolean = true,
      val showInWidget: Boolean = true,
  )
  ```

#### 9. Laylines (Segelboote)
- **Was:** Optimale Wendewinkel relativ zur Windrichtung
- **Berechnung:**
  ```
  layline_port_deg = TWD + tacking_angle
  layline_stbd_deg = TWD - tacking_angle
  ```
- **Darstellung:**
  - Port-Layline: Rot, gestrichelt
  - Steuerbord-Layline: Grün, gestrichelt
  - Vom Ziel-Waypoint aus gezeichnet (zeigt wo man wenden muss)
  - Optional auch vom Boot aus (zeigt VMG-Korridor)
- **Voraussetzung:** Wind-Daten vorhanden + BoatProfile.type == SEGELBOOT
- **Settings:**
  ```kotlin
  data class LaylineSettings(
      val enabled: Boolean = false,
      val tackingAngleDeg: Int = 45,  // aus Wind-Widget-Settings
      val showFromWaypoint: Boolean = true,
      val showFromBoat: Boolean = true,
      val length: Float = 3f,  // NM
  )
  ```

#### 10. Guard Zone / Alarm Zone
- **Was:** Kreisförmige oder sektorale Alarmzone um das Boot
- **Darstellung:**
  - Kreisring um das Boot (innerer + äußerer Radius)
  - Oder: Sektor (Start-Bearing, End-Bearing, Radius)
  - Halbtransparent Rot wenn AIS-Ziel im Guard-Zone
  - Halbtransparent Grün wenn frei
- **Settings:**
  ```kotlin
  data class GuardZoneSettings(
      val enabled: Boolean = false,
      val innerRadiusNm: Float = 0.5f,
      val outerRadiusNm: Float = 2.0f,
      val sectorStartDeg: Float = 0f,   // 0 = Vollkreis
      val sectorEndDeg: Float = 360f,
      val alarmOnEntry: Boolean = true,
  )
  ```

#### 11. Safety Contour / Depth Safety Ring
- **Was:** Dynamische Sicherheitstiefenlinie basierend auf Boots-Tiefgang
- **Berechnung:** Tiefgang aus `BoatProfile.draftMeters` + Sicherheitsmarge
- **Darstellung:** Rote gestrichelte Linie auf der Karte (aus DEPARE-Daten)
- **Voraussetzung:** S-57 ENC geladen mit DEPARE-Flächen

#### 12. MOB-Marker (Man Over Board)
- **Was:** Sofort-Marker bei MOB-Alarm
- **Funktion:**
  - Speichert aktuelle GPS-Position als MOB-Punkt
  - Zeichnet rotes Kreuz + Bearing/Distance-Linie vom Boot zum MOB
  - Bleibt permanent bis gelöscht
  - Optional: Aufzeichnung der Drift (periodische Positionsupdates)
- **UI:** Großer roter MOB-Button im Karten-Widget (immer sichtbar)

#### 13. AIS-CPA-Vektoren
- **Was:** Erweiterung des bestehenden AIS-Overlays um Prediktor-Linien
- **Darstellung:**
  - COG-Vektor für jedes AIS-Ziel (Länge = SOG × 6min)
  - CPA-Punkt als markierter Punkt auf der Karte
  - Verbindungslinie Boot ↔ CPA-Punkt bei Bedrohung (orange/rot)
  - TCPA als Label am CPA-Punkt
- **Status:** AisOverlay existiert (Kreise + Labels), Vektoren fehlen

#### 14. Track / Trail (Kielwasser)
- **Was:** Aufzeichnung der gefahrenen Strecke
- **Darstellung:**
  - Linie hinter dem Boot (letzte N Minuten/Stunden)
  - Farbe: fading von Weiß → Transparent
  - Optional: Farbcodiert nach Geschwindigkeit
- **Settings:**
  ```kotlin
  data class TrackSettings(
      val enabled: Boolean = true,
      val durationMinutes: Int = 60,
      val recordIntervalSeconds: Int = 10,
      val colorBySpeed: Boolean = false,
  )
  ```

---

### Zusammengefasste Datenstruktur

```kotlin
data class NavigationVectorSettings(
    // Heading / COG / Prediktor
    val showHeadingLine: Boolean = true,
    val headingLineLengthNm: Float = 0.5f,
    val showCogVector: Boolean = true,
    val cogVectorMinutes: Int = 6,
    val showPredictor: Boolean = true,
    val predictorMinutes: Int = 6,
    val predictorIntervalMinutes: Int = 1,
    val showPredictorLabels: Boolean = true,

    // Kurslinie (bereits in SeaChartWidgetSettings)
    val showCourseLine: Boolean = false,
    val courseLineBearingDeg: Float = 0f,
    val courseLineDistanceNm: Float = 5f,

    // Bootssymbol
    val showBoatIcon: Boolean = true,
    val boatIconSizeDp: Int = 24,

    // Route / Waypoints
    val showRoute: Boolean = false,
    val showXteCorridor: Boolean = true,
    val xteLimitNm: Float = 0.25f,
    val showEtaOnChart: Boolean = true,
    val useVmgForEta: Boolean = true,

    // Laylines (Segeln)
    val showLaylines: Boolean = false,
    val tackingAngleDeg: Int = 45,
    val laylinesFromWaypoint: Boolean = true,
    val laylinesFromBoat: Boolean = true,
    val laylineLengthNm: Float = 3f,

    // Guard Zone
    val guardZoneEnabled: Boolean = false,
    val guardZoneInnerNm: Float = 0.5f,
    val guardZoneOuterNm: Float = 2.0f,
    val guardZoneSectorStartDeg: Float = 0f,
    val guardZoneSectorEndDeg: Float = 360f,

    // Track/Trail
    val showTrack: Boolean = true,
    val trackDurationMinutes: Int = 60,
    val trackRecordIntervalSeconds: Int = 10,

    // AIS-Vektoren (Erweiterung des AisOverlay)
    val showAisCogVectors: Boolean = true,
    val showAisCpaPoints: Boolean = true,
    val aisPredictorMinutes: Int = 6,

    // Safety
    val showSafetyContour: Boolean = false,
    val safetyDepthMeters: Float = 3f,
)
```

### Geo-Berechnungen (Utility-Klasse)

Alle Navigationsvektoren brauchen eine gemeinsame Geo-Utility:

```kotlin
object GeoCalc {
    const val EARTH_RADIUS_NM = 3440.065

    /** Großkreisdistanz in NM */
    fun distanceNm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double

    /** Anfangspeilung (Forward Azimuth) in Grad */
    fun bearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double

    /** Zielpunkt bei gegebener Distanz und Peilung */
    fun destination(lat: Double, lon: Double, bearingDeg: Double, distanceNm: Double): Pair<Double, Double>

    /** Cross Track Distance (vorzeichenbehaftet, positiv = rechts) */
    fun crossTrackNm(
        boatLat: Double, boatLon: Double,
        legStartLat: Double, legStartLon: Double,
        legEndLat: Double, legEndLon: Double,
    ): Double

    /** Along Track Distance (wie weit auf dem Leg) */
    fun alongTrackNm(
        boatLat: Double, boatLon: Double,
        legStartLat: Double, legStartLon: Double,
        legEndLat: Double, legEndLon: Double,
    ): Double

    /** VMG (Velocity Made Good) zum Waypoint */
    fun vmgKn(sogKn: Float, cogDeg: Float, bearingToWptDeg: Float): Float

    /** CPA-Berechnung zwischen zwei bewegten Objekten */
    fun computeCpa(
        lat1: Double, lon1: Double, cog1: Double, sog1: Double,
        lat2: Double, lon2: Double, cog2: Double, sog2: Double,
    ): CpaResult

    data class CpaResult(
        val cpaNm: Double,
        val tcpaMinutes: Double,
        val cpaLat: Double,
        val cpaLon: Double,
    )
}
```

### Rendering-Reihenfolge auf der Karte (Z-Order)

```
Unterste Ebene (zuerst gezeichnet):
  1. Basiskarte (OpenFreeMap / Offline-Style)
  2. Nautical Overlay (S-57 ENC Layer)
  3. MBTiles / Raster-Overlay
  4. Safety Contour
  5. XTE-Korridor
  6. Guard Zone
  7. Route-Linien (Legs)
  8. Track/Trail
  9. Laylines
  10. Heading-Linie
  11. COG-Vektor
  12. Prediktor + Zeitmarken
  13. Kurslinie
  14. AIS-CPA-Vektoren
  15. AIS-Ziele (Kreise + Labels)
  16. Waypoints (Kreise + Labels)
  17. MOB-Marker
  18. Bootssymbol (Own Ship)
Oberste Ebene (zuletzt gezeichnet)
```

### Farbschema (Day/Night)

| Vektor | Day-Farbe | Night-Farbe |
|---|---|---|
| Heading-Linie | `#FFFFFF` | `#CCCCCC` |
| COG-Vektor | `#FFD700` (Gold) | `#CCAA00` |
| Prediktor | `#FFD700` (Gold) | `#CCAA00` |
| Kurslinie | `#00FFFF` (Cyan) | `#008888` |
| Route-Leg aktiv | `#FF00FF` (Magenta) | `#CC00CC` |
| Route-Leg passiv | `#888888` | `#444444` |
| XTE ok | `#4CAF50` (Grün) 15% | `#2E7D32` 15% |
| XTE Alarm | `#F44336` (Rot) 15% | `#C62828` 15% |
| Layline Port | `#F44336` (Rot) | `#C62828` |
| Layline Stbd | `#4CAF50` (Grün) | `#2E7D32` |
| Guard Zone frei | `#4CAF50` 10% | `#2E7D32` 10% |
| Guard Zone Alarm | `#F44336` 20% | `#C62828` 20% |
| Track | `#FFFFFF` → transparent | `#888888` → transparent |
| MOB | `#FF0000` | `#FF0000` |
| Bootssymbol | `#1565C0` | `#42A5F5` |

---

### Kritische Fixes für S-57 Polygon-Topologie

#### Problem 1: Edge-zu-Ring-Verkettung

Aktuell werden alle Edge-Koordinaten linear aneinandergehängt. Korrekt:

```
Algorithm: Edge Chain to Ring
1. Sammle alle Edge-Referenzen (FSPT) für das Feature
2. Für jede Edge: Hole Koordinaten (start_node + intermediate + end_node)
3. Verkette Edges zu Ringen:
   a. Nimm erste Edge als Startring
   b. Suche nächste Edge deren Start = Ende des aktuellen Rings
   c. Hänge an, wiederhole bis Ring geschlossen
   d. Wenn Ring geschlossen → neuen Ring beginnen (Interior)
4. Erster geschlossener Ring = Exterior (CCW)
5. Weitere Ringe = Interior/Holes (CW)
6. Winding-Order prüfen und ggf. umkehren
```

#### Problem 2: Interior Rings

USAG (Usage) aus SpatialRef MUSS ausgewertet werden:
- `USAG=1` → Exterior-Ring (äußere Begrenzung)
- `USAG=2` → Interior-Ring (Loch/Insel)
- `USAG=3` → Exterior truncated

GeoJSON-Polygon: `[[exterior], [hole1], [hole2], ...]`

#### Problem 3: Face-Topologie

TOPI (Topology Indicator) aus VRPT MUSS gespeichert und genutzt werden:
- `TOPI=1` → Beginning node
- `TOPI=2` → End node
- `TOPI=3` → Left face
- `TOPI=4` → Right face

Dies bestimmt, auf welcher Seite einer Edge die Fläche liegt.

#### Problem 4: SCAMIN

SCAMIN-Attribut (Scale Minimum) bei MapLibre als `minzoom` umsetzen:

```
SCAMIN → MapLibre minzoom Mapping:
  SCAMIN ≤ 10000    → minzoom 14
  SCAMIN ≤ 25000    → minzoom 12
  SCAMIN ≤ 50000    → minzoom 10
  SCAMIN ≤ 100000   → minzoom 8
  SCAMIN ≤ 250000   → minzoom 6
  SCAMIN ≤ 500000   → minzoom 4
  SCAMIN > 500000   → minzoom 2
```

Filter direkt im GeoJSON-to-Layer-Schritt anwenden, NICHT alle Features rendern und per Zoom-Expression filtern.

---

### Karten-Dateisystem-Konvention

```
seaCHART/
├── providers.json              ← Konfiguration aktiver Providers
├── enc/                        ← S-57 ENC Zellen
│   ├── CATALOG.031             ← ENC-Katalog (positionsbasierte Zellauswahl)
│   ├── US5MA10M/               ← Zell-Ordner
│   │   ├── US5MA10M.000        ← Basiszelle
│   │   └── US5MA10M.001        ← Update
│   └── ...
├── s63/                        ← S-63 verschlüsselte Zellen
│   ├── permits/                ← Zell-Permits
│   └── cells/                  ← Verschlüsselte .000-Dateien
├── bsb/                        ← BSB/KAP Rasterkarten
│   └── *.kap
├── mbtiles/                    ← MBTiles-Archive
│   └── *.mbtiles
├── pmtiles/                    ← PMTiles-Archive
│   └── *.pmtiles
├── tiles/                      ← Gecachte Tile-Directories
│   └── {provider}/{z}/{x}/{y}
├── tracks/                     ← GPX/KML Tracks
│   ├── *.gpx
│   └── *.kml
├── grib/                       ← GRIB2 Wetterdaten
│   └── *.grib2
└── styles/                     ← Custom MapLibre Styles
    └── *.json
```

---

## Autopilot-System

Unterstützte Zielgeräte: **Garmin, Raymarine, Simrad**
Gateway-Backends: **SignalK v2, Yacht Devices 0183, Actisense 0183, Direct UDP JSON**

Generiert korrekte NMEA0183-Sätze: HSC, APB, RMB, RMC, MWV (für Windsteuerung)
Simrad erhält zusätzlich SimNet PGN 65341/65480.

---

## Unterstützte NMEA2000 PGNs

| PGN | Beschreibung |
|---|---|
| 129025, 129029 | Position (Rapid/GNSS) |
| 129026 | COG & SOG |
| 127250 | Vessel Heading |
| 130306 | Wind Data |
| 127245 | Rudder |
| 127489 | Engine Speed |
| 127506 | Fluid Level (Water/Grey/Black) |
| 127508 | Battery (SOC/Spannung/Strom) |
| 127237 | Autopilot (Heading/Track Control) |
| 129038, 129039 | AIS Position Reports |

---

## Build & Deploy

```bash
# Product gate
./scripts/seafox-product-check.sh

# Debug build
./gradlew :app:assembleDebug

# Install
adb -s <device-id> install -r app/build/outputs/apk/debug/app-debug.apk

# Restart
adb -s <device-id> shell am force-stop com.seafox.nmea_dashboard
adb -s <device-id> shell am start -n com.seafox.nmea_dashboard/.MainActivity
```

Immer den Projekt-Wrapper `./gradlew` verwenden. `assemble`, `bundle`, `install` und `package` erhoehen die Buildnummer in `version.properties`.

---

## Analyse-Prioritäten bei neuen Aufgaben

**Bei Kartenarbeit (immer zuerst):**
1. Dieses CLAUDE.md vollständig lesen
2. `ChartWidget.kt` → wie wird die Karte angezeigt?
3. `NauticalOverlay.kt` → wie werden S-57-Daten zu Layer?
4. `S57ToGeoJson.kt` → wo genau scheitert die Geometrie?
5. `S57Reader.kt` + `Iso8211Parser.kt` → wie werden Daten extrahiert?

**Bei Widget-Arbeit:**
1. `WidgetCatalog.kt` → Metadaten, Default-Keys, Hilfe-Texte
2. `Widgets.kt` → Canvas-Rendering aller Widget-Typen
3. `DashboardViewModel.kt` → Datenzufluss

**Bei Netzwerk/NMEA-Arbeit:**
1. `NmeaNetworkService.kt` → Parsing, Flows
2. `AutopilotProtocols.kt` → Sende-Logik

---

## Code-Konventionen

- Deutsche UI-Texte, englische Variablen/Kommentare
- Compose + Canvas für Custom-Widgets (kein Standard-Material)
- SharedPreferences für State (kein Room/SQLite)
- JSON-Serialisierung manuell (org.json, kein Gson/Moshi)
- Log-Tags: Konstanten am Dateianfang (`private const val TAG = "..."`)
- Coroutines: SupervisorJob + Dispatchers.IO für Netzwerk/Dateien
- MapLibre statt Google Maps (BSD-lizenziert, kein API-Key)

---

## Technische Randbedingungen

- **minSdk 24** (Android 7.0) — breite Gerätekompatibilität für ältere Bord-Tablets
- **compileSdk 35** — aktuellste APIs verfügbar
- **Kein Internet an Bord garantiert** — alles MUSS offline funktionieren
- **Speicher begrenzt** — OOM-Schutz bei Kartendaten (max 20 ENC-Zellen gleichzeitig)
- **Seegang** — Touch-Targets groß genug, keine filigrane UI
- **Dauerbetrieb** — App läuft 24/7, Memory Leaks sind kritisch
- **Autostart** — BootCompletedReceiver mit 12s Delay

---

## Bekannte offene Probleme (nach Priorität)

### Gelöst (Build erfolgreich verifiziert)
- ~~S-57 Polygon-Topologie~~ → Ring-Verkettung, Interior Rings, Winding Order
- ~~Navigationsvektoren~~ → Heading, COG, Prediktor, Zeitmarken, Bootssymbol, Track
- ~~SCAMIN-Filtering~~ → scaminToMinZoom() in buildFeatures()
- ~~Positionsbasierte Zellauswahl~~ → Catalog031Parser + Kamera-Zentrum-Reaktiv
- ~~AIS ohne Vektoren~~ → COG-Prediktorlinien, CPA-Marker/Links
- ~~OpenSeaMap~~ → Seezeichen-Raster-Overlay verdrahtet
- ~~Route/XTE/ETA GeoJSON~~ → RouteOverlayBuilder komplett
- ~~Laylines/Guard Zones/MOB GeoJSON~~ → NavHazardOverlayBuilder komplett
- ~~ChartProvider-Abstraktion~~ → Interface + ChartData sealed class + OfflineChartPackage

### Noch offen — App-State fehlt zum Aktivieren
1. **Route-State-Pipeline** — kein `activeRoute` im DashboardState/ViewModel → RouteOverlayBuilder liegt dormant
2. **MOB-State** — kein MOB-Button/Marker im UI → NavHazardOverlayBuilder.buildMobFeatures() unused
3. **Layline-Windquelle** — keine TWD-Variable im ViewModel → Laylines dormant
4. **Guard Zone UI** — kein Toggle/Settings-Panel → Guard Zones dormant
5. **Safety Contour** — Placeholder-Contract, braucht DEPARE-Daten-Matching

### Noch offen — Feature-Arbeit
6. **Multi-Scale-Auto-Reload** — Zoom+Kamera-Tracking da, aber NauticalOverlay lädt nicht automatisch passende Zellen nach
7. **ChartProvider-Implementierungen** — Interface existiert, konkrete Provider (S-57, MBTiles, OpenSeaMap) noch direkt verdrahtet
8. **Weitere Kartenformate** — BSB/KAP, PMTiles, WMS/WMTS, GPX/KML, S-63 etc.
9. **Offline-Download-Manager** — User muss Dateien manuell kopieren
10. **Cold-Start Performance** — SCAMIN hilft, aber kein Lazy Loading / Streaming
11. **MBTiles Vector-Support** — nur Raster, kein PBF

### Empfohlene nächste Schritte
1. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
2. Visuell testen → Sim einschalten und Heading-Linie, COG-Vektor, Prediktor-Marken, Bootssymbol sowie ENC-Polygone prüfen
3. `activeRoute` in DashboardState + ViewModel → Route-Rendering aktivieren
4. MOB-Button ins Karten-UI → NavHazardOverlayBuilder.buildMobFeatures() verdrahten
5. ChartProvider konkret für S-57 implementieren → Abstraktion validieren

---

## Parallel-Run-Records

| Datum | Agenten | Brief |
|---|---|---|
| 2026-04-04 19:52 | 4 Lanes | `runs/20260404-195241/brief.md` |
| 2026-04-04 23:46 | 10 Lanes (2 Wellen) | `runs/20260404-234633/brief.md` |

---

## Testdaten

- Debug-Render-Traces: `../tmp/strict_s57_render_trace_2026-03-27.txt`
- Vollständiger Render-Trace: `../tmp/next_render_complete_trace.txt`
- NOAA-ENC-Testdaten: Frei verfügbar unter https://charts.noaa.gov/ENCs/ENCsIndv.shtml
