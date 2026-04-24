# seaFOX QA Matrix

Stand: 2026-04-24

Diese Matrix ist der Produktvertrag zwischen Entwicklung, Design, QA und Release. Jede Zeile braucht vor Store-Verkauf einen Owner und einen reproduzierbaren Nachweis.

| Bereich | Szenario | Mindestpruefung | Automatisierung |
| --- | --- | --- | --- |
| Start | App startet kalt ohne gespeicherten State | Launcher oeffnet Dashboard, keine Crashs | `scripts/seafox-device-smoke.sh` |
| Onboarding | Erststart ohne Berechtigungen | GPS/Netzwerk/Karten/Simulator werden erklaert | Compose/UI test |
| Simulator | Sim EIN/AUS | Werte bewegen sich, Widgets aktualisieren sichtbar | Emulator smoke |
| NMEA UDP | Port 41449 empfaengt JSON/Key-Value | Wind, Batterie, GPS und AIS aktualisieren | JVM parser + device replay |
| Dashboard | Seite erstellen, umbenennen, loeschen | State bleibt nach Neustart erhalten | UI test |
| Widgets | Drag, Resize, Lock/Edit | Keine kaputten Layouts auf Phone/Tablet | Screenshot test |
| Widget Presets | Segelboot/Motorboot/Anker | Sinnvolle Default-Layouts | Manual + screenshot |
| Datenschutz | Private Backups und manueller Export | Sensible Bootsdaten verlassen die App nicht automatisch | JVM policy + UI test |
| Support | Diagnosepaket erstellen | Router-Host und sensible Daten sind standardmaessig redigiert | JVM policy + manual export |
| Boot-Autostart | Boot, Unlock und interner Delayed Launch | App startet nur nach bewusstem Opt-in | JVM policy + device boot test |
| GPS | Keine Berechtigung, grobe/fine Berechtigung | Klare Statusmeldung und Recovery | Emulator permission test |
| SeaChart | Karte oeffnet ohne Online-Daten | Offline-Style/Land-Fallback sichtbar | Emulator smoke |
| NOAA/S-57 | ENC-Zelle laden | Depth Areas, Contours, Soundings, Buoys sichtbar | Fixture test + screenshot |
| Kartenprovider | Persistierte Provider-Settings | C-Map/S-63 werden ohne Lizenz/Implementierung nicht heimlich aktiv | JVM registry/settings test |
| Free Provider | QMAP DE, OpenSeaMap und OSM-Fallback | QMAP DE nutzt freie Raster-Tiles; OpenSeaMap erzwingt Seamarks auf OSM-Basis; OSM ist kein Seekartenprodukt | JVM free-provider contract + manual network test |
| MBTiles/GeoPackage Sideload | Lokales Paket ueber Dateiauswahl importieren | Raster-MBTiles wird validiert, kopiert und offline angezeigt; Vector-MBTiles/GeoPackage werden nicht als renderbar beworben | JVM contract + emulator smoke |
| OpenSeaMap | Seezeichen Overlay | Layer toggelt und laedt korrekt | Manual network test |
| Kurven | Track, Route, Laylines, COG/SOG, Tiefenkurve | Linien sind stabil, lesbar und performant | Geo JVM + screenshot |
| Safety Contour | Bootprofil + Safety Depth | Warnung basiert auf echten Kartendaten | Fixture test |
| AIS | CPA/TCPA | Bedrohungen klassifiziert, Linien/Marker korrekt | JVM + replay |
| MOB | MOB ausloesen/loeschen | Marker, Link, Ruecknavigation, Alarm sichtbar | Emulator smoke |
| Ankerwache | Radius, Alarm, Reset | Alarm priorisiert und quittierbar | Emulator/device |
| Autopilot | Kommandos und Status | Keine versehentlichen kritischen Aktionen | Manual hardware bench |
| BLE BMS | Daly Verbindung | Scan, Connect, Werte, Fehlerstatus | Device bench |
| Offline | Flugmodus/keine Verbindung | App bleibt bedienbar, klare Offline-Texte | Emulator smoke |
| Entitlements | Free/Pro/Navigator/Fleet | App-Stufen schalten nur App-Features frei, keine Kartenrechte | JVM policy + Billing catalog |
| Premium Chart Pack | seaFOX first-party Kartenpaket als Play INAPP | Verifizierter Kauf setzt nur `ownedChartPackIds`; ohne lokale Paketdatei bleibt Status `incomplete`; C-Map/S-63 bleiben unberuehrt | JVM Billing mapper + package status |
| Feature Access | Widgets und Premium-Funktionen | Free/Pro/Navigator/Fleet liefern klare allow/deny-Entscheidungen | JVM policy |
| Low Storage | Kartenpaket Download bei wenig Speicher | Sauberer Abbruch, keine defekten Pakete | Manual |
| Rotation | Portrait/Landscape | Keine Datenverluste, keine abgeschnittenen Controls | Screenshot test |
| Theme | Tag/Nacht/Sonnenlicht | Kontrast und Lesbarkeit im Cockpit | Manual + screenshot |
| Performance | 30 Minuten Simulation | Kein Memory Leak, keine dauerhaften Janks | Profiler run |
| Release | Signed build installieren | Version, Icon, Permissions, Store-Texte korrekt | Release checklist |
| Release R8 | Minifizierter Release-Build | ProGuard/R8 laeuft ohne fehlende Klassen oder Shrinker-Bruch | `--release-r8` gate |
| Crash Reporting | Lokaler Crash-Artefakt | Absturz erzeugt privaten, auswertbaren Bericht ohne Cloud-Upload | JVM formatter + device crash drill |

## Aktuelle Abdeckung

- Vorhanden: `./scripts/seafox-product-check.sh --ci` fuer statische Projektgesundheit, Compile, JVM-Tests und Lint.
- Vorhanden: `./scripts/seafox-product-check.sh --ci --release-r8` fuer Release-Minify/R8 ohne signiertes Artefakt.
- Vorhanden: `./scripts/seafox-device-smoke.sh` fuer nicht-destruktiven adb-Smoke mit optionaler Installation einer vorhandenen APK, App-Start, Screenshot, UI-Dump und Logcat-Capture.
- Vorhanden: JVM-Tests fuer `GeoCalc`, Boot-Autostart-Policy, Autopilot-Safety-Gate, Backup-Privacy, Chart-Provider-Verfuegbarkeit, Free-Raster-Provider-Vertraege, MBTiles/GeoPackage-Sideload-Dateivertraege, Premium-Chart-Pack-Status, persistierte SeaChart-Provider-Normalisierung inklusive JSON-Parsing, Hazard-Depth-Filter, Safety-Contour-Policy, Entitlement-Policy, Feature-Access-Policy, Billing-Katalog, Billing-Restore-Mapping, lokalen Crash-Report-Formatter und Support-Diagnose-Export.
- Vorhanden: Erststart-Onboarding und Fullscreen-Chart im Compose-Code; noch ohne Emulator-/Device-Nachweis.
- Hinweis: Das Device-Smoke-Script fuehrt keine Gradle-Tasks aus und loest damit keine Buildnummern-Inkremente aus; eine APK muss bei Bedarf bereits vorhanden sein.
- Fehlend: Echte Emulator-/Device-Nachweise im aktuellen Workspace, solange kein adb-faehiges Device angeschlossen ist.
- Fehlend: Compose Screenshot-Tests, NMEA-Replay-Fixtures, Karten-Fixtures, Hardware-Bench-Protokolle.

## Definition of Done fuer neue Features

- Jedes neue Feature hat mindestens einen Eintrag in dieser Matrix.
- Kritische Rechenlogik bekommt JVM-Tests.
- UI-Features bekommen mindestens einen Emulator- oder Screenshot-Nachweis.
- Kartenfeatures bekommen ein kleines Fixture oder eine reproduzierbare Testzelle.
- Marine-Safety-Features brauchen manuelle Device-QA plus klaren Disclaimer.
