# seaFOX UI Rewire Run

Stand: 2026-04-25

## Product Goal

seaFOX soll sich im Cockpit wie ein verkaufbares Marine-Produkt anfuehlen: Datenquelle, Karte, GPS/AIS, Alarme, Datenschutz und Entitlements muessen sichtbar, ehrlich und mit grossen Touch-Zielen bedienbar sein.

## User Segment

- Bootseigner mit Android-Tablet im Cockpit.
- Skipper mit NMEA-WLAN-Router, Offlinekarten und selbst konfigurierten Instrumenten.
- Testnutzer, die zuerst im Demo-Modus pruefen und spaeter echte Datenquellen anschliessen.

## Hard Constraints

- Keine ECDIS-Claims.
- App-Abo und Kartenkaeufe bleiben getrennt.
- C-Map/S-63/oeSENC bleiben gesperrt, bis Lizenz, Permit und Implementierung real sind.
- Bestehende Safety-, Privacy- und Entitlement-Gates duerfen nicht verwässert werden.
- `adb` ist lokal nicht verfuegbar; Device-QA bleibt bis Android-Tooling/Device offen.
- Dirty UI-/Designer-Wiki-Dateien aus paralleler Arbeit werden nicht ueberschrieben.

## Success Targets

- P0 UI-Slices laufen durch `./scripts/seafox-product-check.sh --ci --release-r8`.
- Top-Bar zeigt NMEA-Zustand ohne Settings-Suche.
- Demo/Live/Stale/Disconnected-Zustaende sind testbar und ehrlich aus vorhandenem State abgeleitet.
- Neue UI erzeugt keine neuen Store-/License-Versprechen.
- MainActivity wird inkrementell entkoppelt, nicht per Big-Bang-Rewrite.

## Active Pods

- UI Design Synthesis: Briefs und dirty Design-Wiki als Input.
- MainActivity Architecture: sichere Extraktionsreihenfolge.
- Chart UI: Provider/Attribution/Import/Shop naechster Slice.
- NMEA UX: StatusChip, Connection Sheet, Setup-Reife.
- QA/Release: Gate-Matrix und Device-Blocker.
- Entitlement/Privacy UX: Locked/Premium-Zustaende vereinheitlichen.

## Build Slice 01

NMEA Top-Bar Status:

- `NmeaStatusSummarizer` als testbare Domain-Logik.
- `NmeaStatusChip` in der Top-Bar.
- Tap oeffnet kompakte NMEA-Datenquellen-Uebersicht.
- Uebersicht reused bestehende Router-Konfiguration und PGN-Diagnose.

## Build Slice 02

Chart Source Transparency:

- `ChartSourceUiModels` als testbare Domain-/UI-Modellschicht.
- Source-Truth-Pille im Chart und Fullscreen.
- Badges fuer frei/online/import/offline/premium/lizenz/nicht fertig.
- Permanente Attribution und Safety-Hinweis, weil MapLibre-Attribution deaktiviert ist.
- C-Map/S-63 bleiben nicht renderbar und nicht als Kaufversprechen formuliert.

## Build Slice 03

Menu Primitive Extraction:

- `CompactMenuDialog`, `CompactMenuTextButton` und `CompactMenuDropdownItem` aus `MainActivity.kt` geloest.
- `ScrollableMenuTextContent` bleibt privat in `ui/CompactMenu.kt`.
- Keine Dialog-State-Maschinen, keine Billing-/Support-/Chart-Logik verschoben.
- Ziel: kleinerer MainActivity-Monolith und sicherere spaetere UI-Neuverkabelung.

## Next Slices

1. NMEA source management: gespeicherte Quellen, Diagnose-Fusion, spaeter Scan/Test.
2. Chart settings v2: Kartenquelle, Overlays, Import/Shop/gespeicherte Karten.
3. Calibration flow erst nach NMEA-Status/Diagnose.
4. Optional: `OptionSelectorRow` und Menu-TextField-Styles separat extrahieren.
