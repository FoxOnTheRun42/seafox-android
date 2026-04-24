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
| Datenschutz | Private Backups und Support-Share | Sensible Bootsdaten verlassen die App nur nach bewusster Nutzeraktion; oeffentliche Shares sind redigiert | JVM policy + UI test |
| Support | Diagnose-Share starten | Consent-Dialog sichtbar; cache-basierte JSON wird per FileProvider/Sharesheet geteilt; Router-Host, MMSI, Route und MOB sind standardmaessig redigiert; kein automatischer Upload oder Backend-Triage | JVM policy + UI/manual share |
| Boot-Autostart | Boot, Unlock und interner Delayed Launch | App startet nur nach bewusstem Opt-in | JVM policy + device boot test |
| GPS | Keine Berechtigung, grobe/fine Berechtigung | Klare Statusmeldung und Recovery | Emulator permission test |
| SeaChart | Karte oeffnet ohne Online-Daten | Offline-Style/Land-Fallback sichtbar | Emulator smoke |
| NOAA/S-57 | ENC-Zelle laden | Depth Areas, Contours, Soundings, Buoys sichtbar | Fixture test + screenshot |
| S-57 Renderer Skeleton | Layer-Rollen, SCAMIN, SOUNDG, oeSENC-Grenze | Plain S-57 bleibt Beta; oeSENC/S-63 wird nicht als lesbar oder renderbar markiert | JVM renderer skeleton + GeoJSON fixture |
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
| Play Purchase Mapping | Google Play `Purchase` zu internem Record | Play-Daten starten immer `unverified`, Produkt-IDs werden normalisiert, pending bleibt pending | JVM mapper |
| Billing Validation JSON | Backend-Antwort zu Token-Decision | Unbekannte Statuswerte bleiben `unverified`; Response serialisiert keine Tokens zurueck | JVM parser |
| Billing Restore | Play Restore + Servervalidierung | Fehlende oder abgelehnte Serverantwort schaltet nichts frei; nur verified purchased Records erreichen Entitlements | JVM coordinator + mapper |
| Billing Runtime Restore | Abo & Karten im Datenmenue | Play-Restore schreibt den lokalen Snapshot nur nach kompletter Validation; pending/unverified ueberschreibt bestehende Freischaltungen nicht | JVM applier + UI manual |
| Billing Validation HTTP | Receipt-POST an Backend | Request enthaelt nur Validation-Inputs; leerer Endpoint gibt sichere `unverified`-Decision | JVM client contract |
| Premium Chart Pack | seaFOX first-party Kartenpaket als Play INAPP | Verifizierter Kauf setzt nur `ownedChartPackIds`; ohne lokale Paketdatei bleibt Status `incomplete`; C-Map/S-63 bleiben unberuehrt | JVM Billing coordinator/mapper + package status |
| Feature Access | Widgets und Premium-Funktionen | Free/Pro/Navigator/Fleet liefern klare allow/deny-Entscheidungen | JVM policy |
| Runtime Entitlement Gate | Premium-Widgets hinzufuegen | Gesperrte Widgets werden vor Erstellung blockiert; Fehlermeldung nennt benoetigte Stufe und trennt Kartenpakete von App-Features | JVM runtime gate + UI manual |
| Low Storage | Kartenpaket Download bei wenig Speicher | Sauberer Abbruch, keine defekten Pakete | Manual |
| Rotation | Portrait/Landscape | Keine Datenverluste, keine abgeschnittenen Controls | Screenshot test |
| Theme | Tag/Nacht/Sonnenlicht | Kontrast und Lesbarkeit im Cockpit | Manual + screenshot |
| Performance | 30 Minuten Simulation | Kein Memory Leak, keine dauerhaften Janks | Profiler run |
| Release | Signed build installieren | Version, Icon, Permissions, Store-Texte korrekt | Release checklist |
| Release R8 | Minifizierter Release-Build | ProGuard/R8 laeuft ohne fehlende Klassen oder Shrinker-Bruch | `--release-r8` gate |
| Crash Reporting | Lokaler Crash-Artefakt | Absturz erzeugt privaten, auswertbaren Bericht ohne Cloud-Upload; Support-Diagnose teilt nur Count/letzten Zeitpunkt, keine Stacktraces | JVM formatter/inventory + device crash drill |

## Aktuelle Abdeckung

- Vorhanden: `./scripts/seafox-product-check.sh --ci` fuer statische Projektgesundheit, Compile, JVM-Tests und Lint.
- Vorhanden: `./scripts/seafox-product-check.sh --ci --release-r8` fuer Release-Minify/R8 ohne signiertes Artefakt.
- Vorhanden: `./scripts/seafox-device-smoke.sh` fuer nicht-destruktiven adb-Smoke mit optionaler Installation einer vorhandenen APK, App-Start, Screenshot, UI-Dump und Logcat-Capture.
- Vorhanden: JVM-Tests fuer `GeoCalc`, Boot-Autostart-Policy, Autopilot-Safety-Gate, Backup-Privacy, Chart-Provider-Verfuegbarkeit, Free-Raster-Provider-Vertraege, MBTiles/GeoPackage-Sideload-Dateivertraege, S-57-Renderer-Skeleton/SCAMIN/SOUNDG, oeSENC-Nichtunterstuetzung, Premium-Chart-Pack-Status, persistierte SeaChart-Provider-Normalisierung inklusive JSON-Parsing, Hazard-Depth-Filter, Safety-Contour-Policy, Entitlement-Policy, Feature-Access-Policy, Runtime-Entitlement-Gate, Billing-Katalog, Play-Purchase-Mapping, Billing-Validation-JSON, Billing-Validation-HTTP-Contract, Billing-Restore-Coordinator/Mapping, Billing-Runtime-Restore-Applier, lokalen Crash-Report-Formatter/Inventory und Support-Diagnose-Export.
- Dokumentierter Produktvertrag: Der user-facing Support-Diagnose-Share nutzt App-Cache, FileProvider und Android-Sharesheet erst nach Consent; er ist standardmaessig redigiert und bleibt ohne automatischen Upload oder Backend-Triage.
- Vorhanden: Erststart-Onboarding und Fullscreen-Chart im Compose-Code; noch ohne Emulator-/Device-Nachweis.
- Hinweis: Das Device-Smoke-Script fuehrt keine Gradle-Tasks aus und loest damit keine Buildnummern-Inkremente aus; eine APK muss bei Bedarf bereits vorhanden sein.
- Fehlend: Echte Emulator-/Device-Nachweise im aktuellen Workspace, solange kein adb-faehiges Device angeschlossen ist.
- Fehlend: Emulator-/Device-Nachweis fuer Support-Diagnose-Share, insbesondere Consent-Copy, `content://`-URI statt oeffentlicher Datei, Default-Redaction fuer Router-Host/MMSI/Route/MOB und kein unbeabsichtigter Upload.
- Fehlend: Compose Screenshot-Tests, NMEA-Replay-Fixtures, Karten-Fixtures, Hardware-Bench-Protokolle.

## Definition of Done fuer neue Features

- Jedes neue Feature hat mindestens einen Eintrag in dieser Matrix.
- Kritische Rechenlogik bekommt JVM-Tests.
- UI-Features bekommen mindestens einen Emulator- oder Screenshot-Nachweis.
- Kartenfeatures bekommen ein kleines Fixture oder eine reproduzierbare Testzelle.
- Support-/Privacy-Features brauchen Consent-Copy, Default-Redaction und einen Nachweis, dass sie ohne impliziten Upload oder Backend-Triage funktionieren.
- Marine-Safety-Features brauchen manuelle Device-QA plus klaren Disclaimer.
