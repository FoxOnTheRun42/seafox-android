# seaFOX Production Readiness

Stand: 2026-04-24

Ziel: seaFOX soll nicht nur ein technischer Prototyp sein, sondern ein verkaufbares Android-Produkt fuer Boots-Dashboards, Seekarten, Navigation und Borddaten. Dieses Dokument ist das zentrale Product-Gate: Was fehlt, was zuerst gebaut werden muss, und welche Risiken wir vor Monetarisierung schliessen muessen.

## Pipeline-Entscheidung

Modus aus dem Codex Production Pipeline Skill: `Create minimum viable pipeline`.

Begruendung: Die App kompiliert und hat starke Feature-Substanz, aber ihr fehlen reproduzierbarer Wrapper-Build, CI, automatisierte Tests, verbindliche QA-Matrix, Release-Gates, Store-/Billing-Schiene und klare Karten-/Lizenzstrategie.

## Aktueller Produktstand

seaFOX ist ein fortgeschrittener Alpha-Prototyp mit ernstzunehmendem Kern:

- NMEA2000-/NMEA0183-nahe Datenerfassung ueber Netzwerk und Simulation.
- Compose-Dashboard mit frei platzierbaren Widgets.
- MapLibre-basiertes SeaChart-Widget mit Offline-/ENC-Ansaetzen.
- AIS, Route, MOB, Laylines, Guard-Zones und Safety-Contour-Kontrakte als Overlay-Bausteine.
- Autopilot-Kommandos laufen ueber ein Safety-Gate mit Opt-in, Host-Pruefung, sichtbarer Bestaetigung und Timeout.
- Backups sind standardmaessig private App-Daten; oeffentliche Backups brauchen einen expliziten Privacy-Modus.
- Boot-Autostart ist hinter einem zentralen Opt-in-Policy-Gate; Boot, Unlock und verzögerter interner Launch pruefen denselben gespeicherten Nutzerentscheid.
- Kartenprovider haben ein Capability-/Availability-Modell, damit C-Map/S-63 nicht als nutzbar erscheinen, solange Lizenz/Implementierung fehlen.
- Persistierte Kartenprovider-Settings werden auf selektierbare Provider normalisiert; C-Map/S-63 fallen ohne Lizenz-/Implementierungspfad auf eine sichere Quelle zurueck.
- Freie Online-Provider sind als erster Chart-Roadmap-Schritt angebunden: QMAP DE als Raster-Beta fuer deutsche Gewaesser, OpenSeaMap als Seamark-Overlay und OSM nur als interner Online-Fallback ohne Seekartenversprechen.
- Lokales Sideloading ist fuer Kartenpakete angebunden: Raster-MBTiles koennen ueber Android-Dateiauswahl importiert und offline gerendert werden; Vector-MBTiles und GeoPackage werden validiert/gespeichert, aber ehrlich als noch nicht renderbar markiert.
- Erststart-Onboarding, Fullscreen-Chart und Datenschutz-/Bootmodus-Einstellungen sind als Produkt-Shell vorhanden.
- Ein S-57/ENC-Renderer-Skeleton beschreibt jetzt Format-Capabilities, Layer-Rollen, SCAMIN-/Zoom-Planung und oeSENC-Grenzen. Das ist ein testbarer Beta-Vertrag, keine S-52-/ECDIS-Zertifizierung.
- Entitlement-Domainlogik trennt App-Stufen von Kartenlizenzen, bevor Play Billing angeschlossen wird.
- Billing-Katalog fuer `Pro`, `Navigator` und `Fleet` ist vorbereitet; C-Map/S-63 sind inaktive externe Platzhalter ohne Rechtefreischaltung.
- Ein erstes eigenes seaFOX Premium-Kartenpaket (`seafox.chartpack.de_coast`) ist als aktives Play-`INAPP`-Produkt modelliert. Es erzeugt nur `ownedChartPackIds`, keine App-Stufe und keine externen Kartenprovider-Lizenzen.
- Feature-Access-Policy ordnet Widgets und Kernfunktionen testbar `Free`, `Pro`, `Navigator` und `Fleet` zu, ohne Kartenlizenzen mit App-Abos zu vermischen.
- Play Billing Library ist als App-Subscription-Gateway vorbereitet; Restore-Mapping gewaehrt Entitlements erst nach verifiziertem Kaufstatus und markiert unacknowledged Tokens.
- Support-Diagnoseberichte koennen sensible Router-/Bootsdaten standardmaessig redigieren und als JSON-Datei exportieren.
- Lokale Crash-Reports werden als private App-Dateien geschrieben, ohne Cloud-/Telemetry-Upload.
- Persistenz ueber SharedPreferences und vorhandene App-Versionierung.

Noch nicht produktreif:

- Keine vollstaendige Store-faehige Release- und Signing-Schiene; Play-Billing-Fundament ist vorhanden, Kauf-UI, Play-Console-Produkte und Servervalidierung sind noch offen.
- Keine belastbare UI-/Emulator-/Device-Testautomatisierung.
- Keine zertifizierte oder rechtlich sauber fertig lizenzierte Kartenstrategie fuer kommerzielle Premiumkarten.
- Monolithische UI-Struktur mit hohem Aenderungsrisiko.
- Keine Crash-/Telemetry-/Support-Prozesse mit Backend, Export-UI und Triage-Workflow.

## P0 Defizite vor Verkauf

- Reproduzierbarkeit: Vor dieser Pipeline gab es keinen `gradlew`; Builds waren von globalem Gradle und lokaler Java-Version abhaengig.
- QA: Es gab keine JVM-, UI- oder Instrumentation-Tests. Ein einzelner Compile-Erfolg reicht fuer ein Marine-Produkt nicht.
- Kartenrecht: S-63, C-Map und kommerzielle ENC-Quellen sind keine reine Technikfrage. Es braucht Lizenzvertrag, Entitlement, Zertifikats-/Permit-Handling und klare Nutzungsbedingungen.
- Kartendarstellung: S-57 Rendering ist nicht S-52/ECDIS-konform. S-52-nahe Arbeit bleibt ein nicht-zertifiziertes Styling-/Renderer-Experiment und darf nicht als amtliche Navigation beworben werden.
- Safety Contour: Safety-Depth-Berechnung und DEPARE/DEPCNT/SOUNDG-Filter sind vorhanden und mit Fixtures getestet. Es fehlen noch ein vollstaendiger gerenderter Contour-/Warnpfad aus realen ENC-Zellen und Fixture-Screenshots.
- UI/UX: Onboarding und Fullscreen-Chart sind vorhanden, aber viele Funktionen liegen weiter in dichten Popups. Fuer Cockpit-Bedienung fehlen noch konsequent grosse Touch-Ziele, klare Statusleisten und validierte Tablet-/Phone-Flows.
- Release: Release-BuildType, R8/ProGuard-Regeln und optionale Env-Signing-Konfiguration sind vorhanden. Es fehlen Play-Store-Prozess, echte Keystore-Verwaltung, signierter CI-Build, Rollback-Plan und Beta-/Crash-Schiene.
- Datenschutz: Private Backups und redigierte Diagnoseberichte sind vorbereitet. Es fehlen Privacy Policy, Loeschkonzept, Export-UX und Audit der verbleibenden Logging-Pfade.
- Betrieb: Diagnosebericht-Logik, lokale Crash-Reports und Boot-Autostart-Policy-Tests sind vorhanden. Es fehlen Export-/Share-Flow, Crash-Triage-Backend und Kompatibilitaetsmatrix fuer Tablets/Android-Versionen.

## P1 Was gebaut werden muss

- Product Shell: Onboarding fuer GPS, Netzwerkquelle, Simulator, Kartenquelle, Datenschutz und Safety-Hinweis.
- Cockpit UI: Ein klarer Dashboard-Modus, ein Vollbild-Kartenmodus, Bottom-Sheets fuer Kartenoptionen, grosse Tages-/Nacht-Schalter, Touch-Ziele ab 48dp.
- Status System: Permanente Leiste fuer GPS, NMEA, AIS, Kartenquelle, Offline-Zustand, Alarmstatus und Datenalter.
- Widget-Editor: Separater Edit-Modus mit Snap-Grid, Sperren, Undo, Presets und Vorlagen fuer Segelboot/Motorboot/Anker.
- Chart Provider Registry: Einheitliche Provider-Auswahl fuer QMAP DE, NOAA/S-57, OpenSeaMap/OSM-Fallback, MBTiles, Tile-Directory, S-63/C-Map als lizenzierte Module.
- Kartenpakete: Download-Manager mit Fortschritt, Speicherbedarf, Update-Status, Coverage-Vorschau, Loesch-/Reparaturfunktion.
- Kurven und Linien: Track-History, COG/SOG-Verlauf, Windkurven, Tiefenkurve, Laylines, Routenlegs, Cross-Track-Korridor und echte Tiefen-/Konturlinien.
- Navigation Safety: Echte Safety Contour aus Tiefenobjekten, Flachwasser-Warnung, CPA/TCPA-Alarm-Prioritaeten und MOB-Flow mit klarer Ruecknavigation.
- Testing: JVM-Tests fuer Geometrie/Parser, Screenshot-Tests fuer Widgets, Emulator-Flows fuer Onboarding/Karte/MOB, Hardware-nahe Tests fuer UDP/BLE/GPS.
- Monetarisierung: Play Billing, Entitlements, Trial, Offline-Feature-Limits, Pro-Abo, Fleet/Commercial-Lizenz und Lizenzserver-Strategie.

## Karten und Kurven

Damit wirklich alle Seekarten und die Kurven sauber angezeigt werden koennen, fehlen diese Bausteine:

- S-57/S-101 Datenmodell: Vollstaendigere Objektklassifikation, Attribute, Soundings, Depth Areas, Depth Contours, Lights, Buoys, Beacons, Restricted Areas und SCAMIN je Zoom.
- S-52-inspirierte, nicht zertifizierte Styling-Schicht: Symbolisierung, Farbschema fuer Tag/Nacht, Prioritaeten, Textlabel-Regeln und sicherheitsrelevante Overlays.
- Multi-Scale Selection: CATALOG.031/coverage-basiertes Nachladen, Zellpriorisierung nach Position/Zoom und saubere Cache-Invalidierung.
- Tiefenlogik: Ableitung von Safety Depth, Shallow/Deep Contour, Tiefenflaechenfarben und Alarmzonen aus realen ENC-Objekten.
- Kurven-Renderer: Linienvereinfachung, Segmentierung, Antimeridian-Handling, Label-Platzierung und Performance-Budget fuer Track, Route, Laylines und Konturlinien.
- Offline Provider: Raster-MBTiles-Sideloading ist angebunden; Vector-MBTiles, GeoPackage-Rendering, Tile-Directories und GeoJSON muessen noch dieselbe Provider-Abstraktion verwenden.
- Lizenzmodule: S-63/C-Map/oeSENC duerfen erst als bezahltes Produkt erscheinen, wenn Entitlement, Verschluesselung, Zertifikate/Permits, Vendor-Pfad und Vertragslage geklaert sind.
- Free Provider: QMAP DE und OpenSeaMap/OSM sind online angebunden, aber nicht offline-fest, nicht amtlich und nicht fuer Navigation. OSM darf nicht per Bulk-Prefetch oder als kommerzielles Seekartenpaket behandelt werden.

## Monetarisierungsmodell

Empfohlene Stufen:

- Free: Simulator, Basis-Dashboard, begrenzte Widgets, Online-/Open-Chart-Ansicht, kein kommerzielles Kartenversprechen.
- Pro: Offline-Pakete, volle Widget-Konfiguration, Routen/Tracks, AIS-CPA, MOB, Ankerwache, Laylines, Trendkurven.
- Navigator: Erweiterte Kartenpakete, Safety-Contour-Features, erweiterte Alarme, Export/Import, Bootprofile.
- Fleet/Commercial: Mehrere Boote, Support, Diagnosepakete, MDM-/Tablet-Setup, Lizenzverwaltung.

Wichtig: Kostenpflichtige Kartenanbieter nicht mit App-Abo verwechseln. App-Features koennen abgerechnet werden, Kartenlizenzen brauchen getrennte Rechte und Preismodelle.

Aktueller Implementierungsstand: Die Entitlement-Policy existiert als getestete Domainlogik fuer `Free`, `Pro`, `Navigator` und `Fleet`. Ein Billing-Katalog definiert aktive App-Abo-Produkt-IDs, ein aktives first-party Premium-Kartenpaket als Play-`INAPP` und inaktive externe Kartenlizenz-Platzhalter. Eine Feature-Access-Policy mappt Widgets/Funktionen auf die benoetigten App-Features. Das Play-Billing-SDK ist angebunden und ein Gateway kann App-Abos sowie One-Time-In-App-Produkte wiederherstellen/acknowledgen; der Restore-Mapper schaltet nur verifizierte `PURCHASED`-Records frei. Kauf-UI, Play-Console-Produkte, Trial-Regeln, Backend-Receipt-Validierung, UI-Freischaltung, Premium-Pack-Downloadauslieferung und harte Runtime-Enforcement-Callsites sind noch offen. Kartenanbieter werden nur ueber separate lizenzierte Provider-IDs freigeschaltet, nicht automatisch ueber eine App-Stufe.

## Release-Gates

Ein Release darf nur raus, wenn alle Punkte erfuellt sind:

- `./scripts/seafox-product-check.sh --ci` laeuft gruen.
- `./scripts/seafox-product-check.sh --ci --release-r8` laeuft vor jedem Store-Kandidaten gruen.
- Mindestens ein Emulator-Smoke-Test fuer App-Start, Simulator, Chart, MOB und Settings ist dokumentiert.
- Manuelle Device-QA auf mindestens einem Tablet und einem Smartphone ist erledigt.
- Crash-freier Beta-Lauf mit Testpersonen ist dokumentiert.
- Datenschutz, Impressum, Safety Disclaimer und Store Listing sind fertig.
- `docs/SAFETY_DISCLAIMER_DRAFT.md` und `docs/PRIVACY_POLICY_DRAFT.md` sind juristisch finalisiert und in App/Store uebernommen.
- Release-Signing und Rollback-Plan sind vorhanden.
- `docs/RELEASE_CHECKLIST.md` ist fuer den konkreten Kandidaten abgearbeitet.
- Karten-/Provider-Lizenzstatus ist fuer beworbene Features geklaert.

## Naechste Umsetzungsreihenfolge

1. Pipeline stabilisieren: Wrapper, CI, Produkt-Check, erste Tests.
2. UI/UX Produktmodus bauen: Onboarding, Statusleiste, Fullscreen Chart, Bottom Sheets.
3. Kartenkern weiter haerten: echte Safety-Contour-Visualisierung, Konturlinien, Offline-Paketverwaltung und Karten-Fixtures.
4. Testabdeckung erweitern: Parser, Screenshot, Emulator, NMEA-Replay, Hardware-nahe Autopilot-Bench-Tests.
5. Monetarisierung anbinden: Billing, Entitlements, Trial, Lizenz- und Datenschutztexte.
6. Beta ausrollen: TestFlight-Equivalent ueber Play Internal Testing, Feedback-Loop, Crash-Triage.
