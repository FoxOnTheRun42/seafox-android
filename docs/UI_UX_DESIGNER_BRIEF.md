# seaFOX UI/UX Designer Brief

Stand: 2026-04-24

Ziel dieses Dokuments: Eine UI/UX-Designerin oder ein UI/UX-Designer soll aus dieser Beschreibung ein vollstaendiges, umsetzbares Figma-Konzept fuer seaFOX erstellen koennen. Die Beschreibung ist absichtlich konkret: Layout, visuelle Sprache, Komponenten, Zustaende, Interaktionen, Dialoge, Widget-Details, Responsiveness und Safety-Anforderungen.

## 1. Produktbild

seaFOX ist ein Android-Borddisplay fuer Boote. Die App zeigt NMEA2000-/NMEA0183-nahe Telemetrie, Seekarten, AIS, Autopilot-Status, Wind, Tiefe, Tanks, Batterie und Systemdaten in frei platzierbaren Widgets.

Die App soll sich anfuehlen wie ein professionelles Marine-Cockpit: ruhig, hochwertig, praezise, technisch, sicher und unter Stress lesbar. Sie darf nicht wie ein generisches Android-Dashboard, eine Crypto-App, ein Gaming-HUD oder eine dekorative Landingpage wirken.

Kernversprechen der UI:

- Ich sehe auf einen Blick, ob Boot, Datenquelle, GPS, Karte, AIS und Alarme in Ordnung sind.
- Ich kann wichtige Werte auf einem wackelnden Boot mit einem Finger treffen.
- Ich kann zwischen Dashboard, Karte, Alarmen und Einstellungen wechseln, ohne mich zu verlieren.
- Die UI macht klar, was eine Anzeige ist, was ein echter Alarm ist und was eine riskante Aktion ist.
- Autopilot- und Safety-Funktionen fuehlen sich bewusst und kontrolliert an, niemals beilaufig.

## 2. Zielgruppen und Nutzungskontext

Primaere Nutzer:

- Bootseigner mit Android-Tablet als Cockpit-Display.
- Segler, Motorbootfahrer, Charter-Skipper, technische Nutzer mit NMEA-Router.
- Nutzer, die NMEA-Daten, Offlinekarten und Instrumente selbst konfigurieren.

Nutzungssituationen:

- Tablet im Cockpit, Querformat oder Hochformat.
- Helles Sonnenlicht, Nachtfahrt, Regen, Handschuhe, nasse Finger.
- Blickdauer oft unter 2 Sekunden.
- Bedienung mit einer Hand.
- Stoerungen durch Bewegung, Vibration, Motor, Wind.

Design-Konsequenzen:

- Kontrast und Ziffernlesbarkeit sind wichtiger als dekorative Feinheit.
- Touch-Ziele mindestens 48dp, sicherheitskritische Aktionen 56-64dp.
- Keine kleinteiligen Textlisten als primaere Bedienung fuer Cockpit-Funktionen.
- Keine farbliche Bedeutung ohne Form/Text/Signal; Alarme muessen auch ohne Farbwahrnehmung erkennbar sein.

## 3. Visuelle Leitidee

Leitmotiv: Premium Marine Instrument.

Materialien:

- Matte Graphitflaechen.
- Mineralglas-/Frosted-Glass-Anmutung fuer Panels.
- Sehr feine Hairlines, keine schweren Box-Rahmen.
- Subtile Karten-/Bathymetrie-Textur im Hintergrund.
- Code-native Skalen, Linien, Icons und Instrumentmarken fuer gestochen scharfe Darstellung.

Atmosphaere:

- Ruhig, teuer, funktional.
- Keine Neon-Ueberladung.
- Keine grossen dekorativen Verlaeufe, keine Orbs, keine Spielerei.
- Daten stehen visuell ueber Rahmen, Schatten und Schmuck.

Designsprache:

- Formen eher technisch und kompakt: 4dp bis 8dp Radius.
- Widget-Panels maximal 8dp Radius; keine stark abgerundeten Karten.
- Klare horizontale und vertikale Achsen.
- Instrumente wirken wie eingelassen, nicht wie schwebende Web-Cards.
- Icons als klare Line-Icons, nicht als Emoji.

## 4. Farbwelt

Die bestehende Token-Welt soll erhalten und verfeinert werden.

### 4.1 Dark Theme

Primaerer Hintergrund:

- Deep Ocean: `#061018`
- Obsidian: `#0A1118`
- Graphite: `#111B24`
- Raised Graphite: `#172532`

Flaechen:

- Dashboard Dark: `#061018`
- Top Bar Dark: `#0A121A` mit ca. 95% Deckkraft
- Surface Dark: `#162430` mit ca. 90% Deckkraft
- Raised Surface Dark: `#1B2B38` mit ca. 95% Deckkraft

Text:

- Haupttext: `#EAF7FF`
- Sekundaertext: `#9FB3C3`
- Deaktiviert: Haupttext mit 35-45% Deckkraft

Linien:

- Hairline Dark: `#BFEFFF` mit 20-35% Deckkraft
- Panel-Kanten nicht weiss; sie sollen wie Licht auf Glas wirken.

Akzente:

- Signal Cyan: `#64D9FF`
- Soft Cyan: `#9DEBFF`
- Brass: `#D7B46A`
- Coral: `#FF6B5F`
- Emerald: `#47E0A0`

### 4.2 Light Theme

Primaerer Hintergrund:

- Dashboard Light: `#EAF0F1`
- Porcelain: `#F8FAF8`
- Mist: `#F2F6F7`

Flaechen:

- Top Bar Light: `#F7FAF9` mit ca. 97% Deckkraft
- Surface Light: `#FFFFFF` mit ca. 97% Deckkraft
- Raised Surface Light: `#FFFFFF`

Text:

- Haupttext: `#101820`
- Sekundaertext: `#5C6F7D`
- Slate: `#2D3D4A`

Linien:

- Hairline Light: `#60717A` mit 20-35% Deckkraft

### 4.3 Semantik

- Cyan = Daten, Signal, aktive Auswahl, navigierbare Aktion.
- Brass = Edit-Modus, aktive Transformation, Warnung niedriger Prioritaet, Kurs-/COG-Akzent.
- Coral = echter Alarm, Loeschen, Risiko, Safety-Blocker.
- Emerald = stabil, verbunden, positiv, innerhalb sicherer Grenzen.
- Grau/Slate = inaktiv, unbekannt, noch keine Daten.

Coral darf nur fuer echte Warnung, kritische Aktionen oder alarmierte Widgets genutzt werden. Cyan darf nicht fuer Alarme verwendet werden.

## 5. Typografie

Die App hat mehrere Schriftoptionen. Default soll eine technische, aber gut lesbare Cockpit-Typografie sein.

Vorhandene Fonts:

- Orbitron Variable: technischer Instrument-Look, aktuell Default.
- Electrolize: gut lesbare technoide Alternative.
- Dot Gothic 16 / PT Mono: monospaced/terminalartig.
- Futura/System-Fallback: neutraler, ruhiger Look.

Empfehlung:

- Primaerer UI-Font: Electrolize oder Futura/System fuer Menues, Dialoge und Fliesstext.
- Primaerer Instrument-Font: Orbitron fuer grosse Zahlen, Kurswerte, Heading, SOG, Tiefe.
- Monospace nur fuer PGNs, Rohdaten, IDs, NMEA0183-Saetze, Diagnostik.

Typo-Skala:

- Cockpit-Zahlen gross: 44-72sp je nach Widgetgroesse.
- Sekundaere Messwerte: 22-34sp.
- Widget-Titel: 13-16sp, Semibold, max. 1 Zeile.
- Menue-/Dialogtext: 14sp, Line Height 16-18sp.
- Hilfetext/Meta: 11-12sp.
- Status-Chips: 11-13sp.

Regeln:

- Keine negative Laufweite.
- Keine viewport-basierten Fontgroessen.
- Zahlen sollen tabellarisch wirken; wechselnde Werte duerfen Layout nicht springen lassen.
- Lange Widget-Titel ellipsieren, nie umbrechen, wenn dadurch das Instrument verrutscht.
- Einheiten kleiner als Werte darstellen, aber unmittelbar am Wert halten.

## 6. Raster, Masse und Spacing

Basis:

- 4dp als Feineinheit.
- 8dp als Standardabstand.
- 16dp als Panel-Innenabstand fuer groessere Flaechen.
- 24dp fuer groessere Gruppenabstaende.

Touch:

- Minimum: 48dp x 48dp.
- Kritische Aktionen: 56dp x 56dp oder breiter.
- Widget-Resize-Griffe: visuell subtil, aber mit 64-84dp Trefferflaeche.

Radii:

- Menue-Inputs: 4dp.
- Buttons/Chips: 6-8dp.
- Dialoge: 8dp.
- Widget-Panels: 8dp aussen, 6dp innen.
- Keine pillenfoermigen Textbuttons, ausser Status-Chips mit sehr kurzem Text.

Dashboard-Grid:

- Default-Snap: 2.5% der Dashboard-Breite.
- Einstellbarer Bereich: 0.5% bis 20%.
- Grid nur im Edit-Modus sichtbar, mit 8-15% Deckkraft.
- Widgets schnappen beim Loslassen auf das Raster.

## 7. App-Struktur

Primaere Ebenen:

1. Erststart/Safety-Onboarding.
2. Dashboard mit Seiten-Pager und Top-Bar.
3. Frei platzierbare Widgets.
4. Widget-Menues und Widget-Hilfe.
5. System-/Darstellungs-/Datenquellen-Einstellungen.
6. Karten- und Offlinekarten-Flows.
7. Alarm-, MOB-, Autopilot- und Safety-Bestaetigungen.

Die App soll immer zuerst das echte Arbeitscockpit zeigen, keine Landingpage.

## 8. Erststart/Safety-Onboarding

Darstellung:

- Modal ueber dunklem, leicht abgedunkeltem Dashboard-Hintergrund.
- Dialog max. 560dp breit, auf Phone 24dp Seitenabstand.
- Hintergrund: Raised Graphite `#1B2B38`, 8dp Radius, 1dp Cyan-Hairline mit 25% Deckkraft.
- Titel: "seaFOX Erststart", 16sp Semibold.
- Text in kurzen Safety-Zeilen mit Icon links und Text rechts.

Inhalte:

- Safety: seaFOX ist ein Marine-Assistent, kein zertifiziertes ECDIS.
- Autopilot: Kommandos sind gesperrt, bis Safety Gate bewusst armiert und jeder Befehl bestaetigt wird.
- Datenschutz: Backups bleiben standardmaessig privat; MMSI, Route, MOB und Router-Hosts sind sensibel.
- Datenquelle: Live-NMEA ueber WLAN Router oder Simulator.
- Karten: C-Map/S-63 bleiben verborgen/gesperrt, bis Lizenz und Implementierung bereit sind.

Buttons:

- Primaer: "Verstanden"
- Sekundaer: "Simulator starten"
- Beide 48dp Mindesthoehe.
- "Simulator starten" darf nicht aggressiv wirken; Cyan Outline oder Textbutton.

## 9. Hauptscreen: Dashboard

Der Hauptscreen besteht aus:

- Top-Bar / Instrument-Rail oben.
- Dashboard-Hintergrund darunter.
- Horizontal swipebarer Seiten-Pager.
- Widgets als frei positionierte Instrumente.
- Optionaler Status-Rail fuer GPS/NMEA/AIS/Karte/Alarm.

### 9.1 Hintergrund

Dark Theme:

- Vollflaechig Deep Ocean.
- Darueber sehr subtile bathymetrische Linien oder Kartenkonturen mit 4-8% Deckkraft.
- Optional feine Vignette: oben/unten minimal dunkler.
- Keine grossen Verlaeufe, keine dekorativen Lichtkugeln.

Light Theme:

- Warmes Off-White/Chart-Paper-Gefuehl.
- Sehr subtile Kartentextur, nicht gelblich oder vintage.
- Instrumente muessen immer staerker als Hintergrund sein.

### 9.2 Top-Bar / Instrument-Rail

Hoehe:

- Phone Portrait: 48-56dp.
- Tablet Landscape: 56-64dp.

Layout:

- Links: Menu-Icon 40-48dp Trefferflaeche.
- Neben Menu: Version klein und muted, z. B. `(1.2.3)`.
- Mitte: Aktueller Seitenname, z. B. "Navigation", klickbar.
- Rechts: Version/Build oder optional Status-Cluster.

Optik:

- Hintergrund: glasige, horizontale Flaeche mit `premiumTopBarBrush`.
- Unterkante: 1dp Hairline.
- Keine starke Schattenkante.
- Titel exakt mittig, auch wenn links/rechts unterschiedliche Breite haben.

Interaktion:

- Tip auf Seitenname oeffnet "Seite auswaehlen".
- Tip auf Menu oeffnet Hauptmenu.
- Seitennamen max. 18-22 Zeichen, sonst Ellipsis.

### 9.3 Status-Rail

Empfohlene Erweiterung unter oder innerhalb der Top-Bar:

- GPS: Fix/No Fix + Satelliten/HDOP.
- NMEA: Live/Simulation/Offline + Datenalter.
- AIS: Anzahl Ziele + Alarmstatus.
- Karte: Provider + Online/Offline.
- Alarm: Normal/Muted/Active.

Darstellung:

- Kleine Chips, 28-32dp hoch.
- Icon links, kurzer Text rechts.
- Normal: muted outline.
- Aktiv/verbunden: Emerald.
- Simulation: Brass.
- Alarm: Coral, blinkt nicht dauerhaft, sondern pulsiert langsam nur bei neuem Alarm.

## 10. Hauptmenu

Das Menu ist ein kompaktes Cockpit-Menu, aber nicht winzig.

Hauptpunkte:

- Seiten
- Widgets
- Bootsdaten
- Darstellung
- Alarmeinstellungen
- System
- App beenden

Design:

- Keine Emoji-Icons. Ersetzen durch konsistente 18-20dp Line-Icons.
- Menu-Panel max. 320dp breit auf Phone, 360dp auf Tablet.
- Hintergrund wie Top-Bar, Radius 8dp, 1dp Hairline.
- Zeilenhoehe mindestens 40dp, besser 44dp.
- Text 14sp.
- Trennlinien 1dp mit 12-18% Deckkraft.

Menue-Hierarchie:

- Unterpunkte mit Chevron rechts.
- Zurueck-Zeile oben mit Linkspfeil.
- Aktive Seite mit kleinem Cyan-Punkt oder linkem Balken, nicht nur Bullet-Zeichen.

## 11. Seitenverwaltung

Screens/Dialogs:

- Seite auswaehlen.
- Neue Seite.
- Seite umbenennen.

Design:

- Seite auswaehlen als Bottom Sheet auf Phone, zentrierter Dialog auf Tablet.
- Jede Seite als 44-48dp Zeile.
- Aktive Seite: Cyan linke Linie 3dp plus Semibold.
- Neue Seite: Input "Seitenname", 48dp Hoehe, 4dp Radius.
- Umbenennen identisch, mit bestehendem Namen vorausgefuellt.

Leere App:

- Hintergrund bleibt das echte Dashboard.
- Zentraler Empty State:
  - dezentes Kompass-/Fadenkreuz-Icon 96-132dp.
  - Titel: "Noch keine Seite vorhanden".
  - Text: "Lege eine Navigationsseite an und platziere deine Instrumente."
  - Button: "Erste Seite erstellen".
- Empty State nicht in eine grosse Card setzen; Inhalt frei auf Hintergrund mit guter Lesbarkeit.

## 12. Widget-Bibliothek

Kategorien:

- Navigation: Wind, Kompass, Karten, Speed, GPS, PGN Empfang, Autopilot, Deep, Temperatur.
- Sicherheit: AIS, Ankerwache.
- System: NMEA0183 Empfang, Systemauslastung, DALY BMS (Beta).
- Energie und Tankstaende: Wassertank 1, Schwarzwasser, Grauwasser, Batterie.
- Antrieb: Motordrehzahl.

Darstellung:

- Auf Phone als Bottom Sheet mit 80-90% Hoehe.
- Auf Tablet als Dialog 560-640dp breit.
- Kategorien als Accordion.
- Kategoriezeilen 48dp hoch, Icon + Titel + Count + Chevron.
- Widget-Zeilen 48dp hoch mit Icon, Label, kurzer Datenhinweis.
- "Beta" als kleiner Brass-Chip.
- Nach Auswahl wird Widget platziert und Sheet geschlossen.

Widget-Zeilen sollten nicht wie Marketing-Karten aussehen. Es ist eine Werkzeugliste.

## 13. Widget-Panel Grundform

Jedes Widget besteht aus:

- Aussenrahmen 1dp Hairline.
- Panel-Hintergrund: transparentes Glas oder gefuellte Flaeche je nach Einstellung.
- Header 36-40dp hoch.
- Inhalt darunter mit 4-12dp Innenabstand.

Normalzustand:

- Rahmen: Cyan-Hairline 20-35% im Dark Theme, Slate-Hairline im Light Theme.
- Header: dezenter horizontaler Glass-Bereich.
- Titel links, Menue-Icon rechts.
- Kein starker Drop Shadow.

Filled Style:

- Flaeche leicht deckender, damit Widgets auf heller/komplexer Karte lesbar bleiben.

Border Style:

- Flaeche transparenter, nur Rahmen und Header geben Struktur.

Alarmzustand:

- Rahmen 1.5-2dp Coral.
- Header mit Coral-Verlauf/Overlay.
- Optional Mute-Button im Header.
- Alarmstatus muss sofort erkennbar sein.

Edit-Zustand:

- Rahmen Brass.
- Zentraler Bewegungsbereich sichtbar: Cyan-Kreis mit niedriger Deckkraft.
- Countdown sichtbar: "4s", "3s", ...
- Resize-Zonen in vier Ecken als subtile Cyan-Dreiecke.
- Loeschen als Coral-X oben mittig, Trefferflaeche min. 48dp.

## 14. Widget-Interaktion

Standard:

- Tip auf Widget-Menue-Icon oeffnet Widget-Menue.
- Tip/Drag auf normale Flaeche verschiebt nicht sofort, damit versehentliches Beruehren keine Layouts zerstoert.

Edit/Move:

- Long Press ca. 2 Sekunden im zentralen Bewegungsbereich aktiviert Edit-Modus.
- Nach Aktivierung kann das Widget verschoben werden.
- Nach 4 Sekunden Inaktivitaet verlässt das Widget Edit/Transform und snappt.
- Bei erneutem Touch innerhalb des Countdowns bleibt Edit aktiv.

Resize:

- Vier Eckgriffe.
- Trefferflaeche 64-84dp.
- Visuell nur feine Dreiecke oder Corner-Linien, keine schweren Griffe.
- Resize erfolgt in Grid-Schritten.

Loeschen:

- Im Edit-Zustand sichtbar.
- Im Widget-Menue als "Widget entfernen".
- Kritischer Button Coral, mit Bestaetigung bei Safety-relevanten Widgets optional.

## 15. Widget-Menue

Grundstruktur:

- Kein grosser Titel; der Kontext ist das Widget.
- Oben immer "Name" mit Input.
- Danach "Hilfe" mit Button "Oeffnen".
- Danach widget-spezifische Einstellungen.
- Unten "Widget entfernen".
- Footer: "Speichern" und "Abbrechen".

Design:

- Phone: Bottom Sheet, nicht winziger AlertDialog.
- Tablet: zentrierter Dialog.
- Max. Breite 600dp.
- Scrollbarer Inhalt.
- Controls immer rechtsbündig ausgerichtet, Labels links.
- Sliders mit Wertanzeige rechts.
- Switches nicht auf 0.5x skalieren; stattdessen kompakte, aber treffbare Switch-Komponente.

## 16. Einzelne Widgets

### 16.1 Karten / SeaChart

Zweck:

- Zentrale Navigationskarte mit Bootssymbol, Position, Heading, COG, Track, AIS, Guard Zone, Laylines, Safety Contour und Offlinekartenstatus.

Look:

- Karte nutzt volle Widgetflaeche ohne dekorative Innenkarte.
- Controls als kleine Glas-Chips ueber der Karte.
- Bootssymbol klar, mittig/positionsgetreu, mit Heading-Spitze.
- Heading Line: Cyan.
- COG Vector: Brass oder Soft Cyan, vom Heading klar unterscheidbar.
- Track: dezente Cyan-Linie mit 60-70% Deckkraft.
- Predictor: gestaffelte Marker mit Zeitlabels, wenn aktiviert.
- AIS-Ziele: kleine Dreiecke/Schiffssymbole; riskante Ziele Coral.
- MOB: Coral Marker mit eindeutiger Beschriftung "MOB".
- Safety Contour: Coral/Brass je nach Risiko; nicht mit dekorativen Tiefenlinien verwechseln.

Karten-Menue:

- Karte/Provider.
- Offlinekarten laden.
- Dokumentation.
- Kartendetails.
- Datengrundlage.
- AIS-Overlay.
- GRIB-Overlay.
- OpenSeaMap Overlay.
- Heading-Linie + Laenge.
- COG-Vektor + Zeitraum.
- Praediktor + Zeitraum + Markenabstand + Labels.
- Bootssymbol + Groesse.
- Track + Dauer + Intervall.
- Guard Zone + innerer/aeusserer Radius + Sektor Start/Ende.
- Laylines + Wendewinkel + Laenge.
- Safety Contour + Sicherheitstiefe.
- Kurslinie + Bearing/Distanz, wenn vorhanden.

Karten-Status:

- Provider-Name sichtbar: NOAA, S-57, OpenSeaMap etc.
- Nicht verfuegbare/lizenzierte Provider nicht wie klickbare Optionen darstellen.
- C-Map/S-63 nur als gesperrt/erklaert, nicht als kaufbares Versprechen.

### 16.2 Wind

Look:

- Rundes Instrument mit Boot-Achse.
- Pfeil fuer Windrichtung relativ zum Boot.
- Nordbezug optional als feine Markierung.
- True Wind und Apparent Wind farblich/linienstilistisch getrennt.
- Wendewinkel als rote/gruene Boegen.
- Windgeschwindigkeit unten rechts gross.
- Windrichtung/Grad unten links.
- Min/Max als kleine Werte oder Balken, nicht gleich wichtig wie aktueller Wert.

Einstellungen:

- Min/Max Messzeitraum.
- Min/Max basiert auf wahrer/scheinbarer Wind.
- Windrichtung zum Boot.
- Windrichtung zu Nord.
- Windgeschwindigkeit.
- Einheit: kn, m/s, km/h etc. je nach vorhandener Option.
- Wendewinkel.

### 16.3 Kompass

Look:

- Kreis mit klaren Kardinalpunkten N/E/S/W.
- Heading-Zeiger Cyan.
- COG-Zeiger Brass/Gelb.
- Aktueller Kurs als grosse Zahl in der Mitte.
- Keine ueberladene Skala; Hauptmarken alle 30 Grad, Feinmarken alle 10 Grad.

### 16.4 GPS

Look:

- Datenblock statt Rundinstrument.
- LAT/LON gross genug, 2 Zeilen.
- COG, SOG, Heading als drei gleich breite Felder.
- Satelliten/HDOP/Fix Quality als Statuszeile.
- Kein Fix: Werte gedimmt, Status Coral/Brass.

### 16.5 Speed / Log

Look:

- Primaerwert SOG/STW gross.
- Quelle sichtbar: GPS (SOG) oder Geber (STW).
- Verlauf/Zeitraum als kleine Trendlinie.
- Trip/Distanz nicht wichtiger als aktuelle Geschwindigkeit.

Einstellungen:

- Datengrundlage.
- Speed-Zeitraum 1-120 min.

### 16.6 AIS

Look:

- Radar-/Plan-View mit eigenem Boot in der Mitte.
- Ziele als Dreiecke, Ausrichtung nach COG/Heading.
- CPA/TCPA Werte prominent, wenn kritisch.
- Keine Ziele: ruhiger Empty State "Keine AIS-Ziele".
- Kritisches Ziel: Coral, zusaetzlicher Ring/Line-to-CPA.

Einstellungen:

- CPA Abstand.
- CPA-Zeit.
- Sichtbarkeit global.
- Sichtbarkeit je MMSI.
- Ausrichtung: Nord oben / Schiffausrichtung oben.
- Schriftgroesse.
- Kollisionsalarm stumm/aktiv als Status, nicht versteckt.

### 16.7 Autopilot

Look:

- Modus STBY/KURS/WIND klar als segmented control.
- Heading/Target Heading gross.
- Rudder Angle als Balken oder Arc.
- Durchschnittlicher Rudder Angle kleiner.
- Ziel +/- Controls gross und symmetrisch.
- Tack-Funktion nur sichtbar/aktiv, wenn Daten sinnvoll sind.

Safety:

- Wenn Safety Gate nicht armiert: Befehlsbuttons gedimmt, Hinweis sichtbar.
- Armieren nur im Einstellungsdialog mit bewusstem Toggle plus erklaertem Risiko.
- Jeder Befehl braucht Bestaetigung.
- Coral nur fuer Blocker/Fehler, Brass fuer "nicht armiert".

### 16.8 Deep / Echosounder

Look:

- Tiefe als riesiger Wert, Einheit nah am Wert.
- Mindesttiefe als horizontale Linie/Marker.
- Alarmzone unterhalb Mindesttiefe Coral.
- Dynamik/Trend als kleiner Pfeil oder delta.

Einstellungen:

- Mindesttiefe.
- Tiefendynamik.
- Tiefeneinheit: m/inch.
- Alarmton mit Test.

### 16.9 Ankerwache

Look:

- Kreis/Ring um Ankerpunkt.
- Bootposition relativ zum Anker.
- Toleranzradius klar.
- Kettenlaenge sichtbar.
- Monitoring an/aus als grosser Status.
- Alarm bei Abweichung: Coral Ring, Mute sichtbar.

Einstellungen:

- Monitoring aktiv.
- Ankerkettensensor verwenden.
- Quelle.
- Kettenlaenge / Kalibrierung.
- Masseinheit.
- Kettenstaerke.
- Kettentaschen der Ankerwinde.
- Toleranz in Prozent.
- Reset auf Voreinstellung.
- Alarmton + Test.

### 16.10 Batterie

Look:

- Batteriekontur oder Batterie-Bar mit Ladezustand in Prozent.
- Spannung, Strom, Leistung getrennt.
- Laden positiv Emerald, Entladen neutral/Cyan oder Brass je nach Schwere.
- Lithium: Zellspannungsdifferenz sichtbar.

Einstellungen:

- Batterietyp Bleisaeure.
- Batterietyp Lithium.

### 16.11 Tanks

Varianten:

- Wassertank: Frischwasser, Cyan/Blau.
- Grauwasser: Slate/Grau.
- Schwarzwasser: dunkler Slate/Brass-Warnung bei hoch.

Look:

- Fuellstand als vertikaler oder horizontaler Tank.
- Prozentzahl gross.
- Bei hohem Schwarzwasser/Grauwasser nicht sofort Coral, sondern Brass; Coral nur bei kritischem Schwellenwert.

### 16.12 Temperatur

Look:

- Bis zu 10 Sensoren als kompakte Liste.
- Jeder Sensor: Name links, Wert rechts.
- Auffaellige Werte mit Brass/Coral je nach spaeterer Schwelle.

Einstellungen:

- Einheit Celsius/Fahrenheit.
- Sensornamen 1-10 editierbar.

### 16.13 Motordrehzahl

Look:

- RPM als grosses Instrument.
- Arc/Tachometer mit ruhigem Bereich, Warnbereich Brass/Coral.
- Digitaler Wert immer sichtbar.

### 16.14 NMEA PGN Empfang

Look:

- Technische Diagnoseanzeige.
- Liste letzter PGNs mit Label, Quelle, Alter.
- Monospace fuer Nummern.
- Status "empfangen seit / zuletzt".

### 16.15 NMEA0183 Empfang

Look:

- Letzte Saetze kompakt.
- Kategorie/Typ farblich als Chip.
- Rohsatz in Monospace, aber abgeschnitten/scrollbar.

### 16.16 Systemauslastung

Look:

- CPU, RAM, Heap.
- Progressbars.
- Geschätzte Widget-Last je Widget.
- Lastfarben: Cyan normal, Emerald niedrig/ok, Brass erhoeht, Coral kritisch.

### 16.17 DALY BMS (Beta)

Look:

- Beta-Chip im Header.
- Verbindungstatus prominent.
- SOC, Spannung, Strom, Temperatur.
- Debug-/BLE-Details in Unterbereich, nicht primär.

## 17. Dialoge und Bottom Sheets

Dialogtypen:

- Safety/Onboarding: modal, nicht dismissbar ausser ueber Aktion.
- Einstellungen: dismissbar, mit Speichern/Abbrechen.
- Warnung/Fehler: klare Ursache, klare naechste Aktion.
- Download/Offlinekarten: Fortschritt, Dateigroesse, Status, Retry/Cancel.
- Loeschen: Coral Primaeraktion, Abbrechen daneben.

Phone:

- Einstellungen bevorzugt als Bottom Sheet.
- Max. 90% Hoehe.
- Drag handle oben 36dp breit.
- Sticky Footer fuer Speichern/Abbrechen.

Tablet:

- Zentrierter Dialog.
- Breite 560-720dp je nach Komplexitaet.
- Max. Hoehe 80% Viewport.

Controls:

- Text Input: 48dp Hoehe, 4dp Radius.
- Sliders: immer mit Wertanzeige.
- Radio/Segmented Controls fuer exklusive Auswahl.
- Switches fuer binaere Optionen.
- Dropdowns fuer lange Optionslisten.
- Keine sichtbaren Erklaertexte fuer simple Controls; Hilfetexte nur bei Safety, Datenschutz, Router, Kartenlizenz.

## 18. System- und Einstellungsbereiche

### 18.1 Darstellung

Inhalte:

- Hell/Dunkel.
- Hochformat/Querformat.
- Widget Style: Rahmen / Hintergrundfarbe.
- Hintergrundhelligkeit.
- Schriftart.
- Widget-Schriftgroesse.
- Rastergroesse.

Design:

- Als "Appearance" Sheet mit Gruppen.
- Hell/Dunkel als 2er segmented control.
- Layout als 2er segmented control mit Icons.
- Widget Style als Radio-Karten/Row, aber nicht als dekorative Card.
- Sliders mit aktuellem Wert rechts.

### 18.2 Bootsdaten

Felder:

- Laenge (m)
- Breite (m)
- Name
- Heimathafen
- MMSI
- Tiefgang (m)
- Typ: Motorboot / Segelboot

Design:

- Zwei Spalten auf Tablet, eine Spalte auf Phone.
- MMSI nur Zahlen.
- Typ als segmented control mit Boot-Icons.
- Tiefgang mit Safety-Hinweis, weil er fuer Safety Contour relevant ist.

### 18.3 WLAN Router

Felder:

- Router IP/Host.
- Port.
- Protokoll TCP/UDP.
- NMEA Quelle Live Daten/Simulation.
- Hilfe.

Design:

- Status oben: Live verbunden, Simulation aktiv, keine Daten, letzter Empfang.
- Host/Port als technische Felder.
- TCP/UDP als segmented control.
- Live/Simulation als prominentere Auswahl, weil sie Produktzustand veraendert.

### 18.4 Datenschutz & Bootmodus

Inhalte:

- Backup-Privatsphaere.
- Kiosk-/Boot-Modus.
- Autostart nach Geraetestart.

Design:

- Datenschutzoptionen als Radio-Liste mit Beschreibung.
- Autostart als Switch mit Warntext.
- Default-Zustand muss klar "aus/privateOnly" sein.

### 18.5 Alarmeinstellungen

Inhalte:

- Alarmlautstaerke.
- Wiederholungsintervall.
- Ansage aktivieren.
- Stimmauswahl.
- Testalarm.

Design:

- Lautstaerke Slider 0-200%, Wert rechts.
- Wiederholung 2-10s, Slider oder Stepper.
- Stimme als segmented control.
- Testalarm als klarer Button mit Sound-Icon.

## 19. Karten-Download und Offlinekarten

Download-Dialog:

- Provider/Katalog-Titel.
- Beschreibung der Quelle.
- Regionliste.
- Dateigroesse, falls bekannt.
- Speicherort/Status.
- Fortschrittsbalken mit Prozent und Bytes.
- Statusphasen: Pruefen, Laden, Entpacken, Validieren, Fertig.
- Fehler: Ursache + Retry + Datei loeschen, wenn sinnvoll.

Bestehende Daten:

- Wenn Daten vorhanden sind: "Bereits geladen", Datum, Groesse, Aktualisieren, Loeschen.
- Loeschen braucht Bestaetigung.

Lizenzstatus:

- Gesperrte Provider nicht in normaler Liste als aktive Auswahl.
- Falls sichtbar: Lock-Icon, Text "Lizenz/Implementierung nicht verfuegbar".

## 20. Safety, Alarme und kritische Aktionen

Alarmhierarchie:

- Info: Cyan/Muted.
- Hinweis: Brass.
- Warnung: Brass + Icon.
- Kritisch: Coral + Rahmen/Header + optional Ton.

MOB:

- MOB-Aktion muss gross, eindeutig und schwer versehentlich auszuloesen sein.
- Nach Ausloesen: Karte zeigt MOB Marker, Kurs/Distanz zurueck, Clear MOB nur mit Bestaetigung.

Autopilot:

- Safety Gate nicht im Hauptfluss verstecken.
- Armierter Zustand muss sichtbar sein.
- Befehl senden nie ohne explizite Bestaetigung.

Loeschen:

- "Daten loeschen" und "Widget entfernen" visuell klar riskant.
- App-Daten loeschen immer mit Bestaetigung.

## 21. Responsiveness

Phone Portrait:

- Top-Bar 48-56dp.
- Widget-Menues als Bottom Sheets.
- Dashboard kann gescrollt/gewischt werden, Widgets bleiben frei positioniert.
- Texte in Controls duerfen nicht ueberlappen; lange Labels umbrechen oder kuerzen.

Phone Landscape:

- Top-Bar kompakt.
- Status-Chips ggf. horizontal scrollbar.
- Dialoge max. 80% Hoehe.

Tablet Landscape:

- Primaerer Zielmodus.
- Dashboard wirkt wie Cockpit-Panel.
- Widgets koennen dichter stehen.
- Menues koennen als Dialog oder seitliches Sheet erscheinen.

Tablet Portrait:

- Grosse Zahlen beibehalten.
- Widget-Bibliothek und Einstellungen als breitere Bottom Sheets oder Dialoge.

## 22. Accessibility und Lesbarkeit

Kontrast:

- Hauptwerte mindestens WCAG AA gegen Panel.
- Kritische Werte mit Farbe + Icon/Form.

Touch:

- Kein aktives Element unter 48dp Trefferflaeche.
- Kleine visuelle Icons duerfen groessere unsichtbare Trefferflaechen haben.

Bewegung:

- Keine permanenten schnellen Animationen.
- Alarm-Puls langsam und sparsam.
- Karten- und Instrumentwerte duerfen aktualisieren, aber Layout darf nicht springen.

Text:

- Keine langen Erklaerparagraphen im Cockpit.
- Safety-/Router-/Privacy-Texte kurz und konkret.
- Deutsche UI-Texte konsistent; "Widget" verwenden, nicht "Container".

## 23. Iconografie

Stil:

- Monoline, 1.75-2dp Stroke.
- Abgerundete Linienenden, aber technische Form.
- Icons 18-24dp.
- Fuer Menues keine Emoji.

Benötigte Icons:

- Menu.
- Seiten.
- Widget.
- Bootsdaten.
- Darstellung.
- Alarm.
- System.
- Power/App beenden.
- GPS.
- NMEA.
- AIS.
- Karte.
- Download.
- Lock.
- Warnung.
- Mute/Unmute.
- Autopilot.
- Anker.
- Batterie.
- Tank.
- Temperatur.
- Motor.
- Wind.
- Tiefe.

## 24. Figma-Handoff

Anzulegen:

- Cover: seaFOX Premium Marine Cockpit.
- Design Tokens: Farben, Typo, Spacing, Radius, Elevation/Hairlines.
- Components:
  - TopBar.
  - StatusChip.
  - MenuRow.
  - Dialog.
  - BottomSheet.
  - TextInput.
  - SegmentedControl.
  - SliderRow.
  - SwitchRow.
  - WidgetFrame.
  - WidgetHeader.
  - EditHandles.
  - AlarmState.
  - ChartOverlayChip.
- Screens:
  - Erststart.
  - Dashboard Dark mit Widgets.
  - Dashboard Light mit Widgets.
  - Empty State.
  - Hauptmenu.
  - Seitenverwaltung.
  - Widget-Bibliothek.
  - Widget-Menue Wind.
  - Widget-Menue Karte.
  - Widget-Menue AIS.
  - Widget-Menue Autopilot Safety.
  - Darstellung.
  - WLAN Router.
  - Datenschutz & Bootmodus.
  - Alarmeinstellungen.
  - Offlinekarten Download.
  - Alarmzustand.
  - MOB Zustand.
- Breakpoints:
  - 393x852 Phone Portrait.
  - 852x393 Phone Landscape.
  - 800x1280 Tablet Portrait.
  - 1280x800 Tablet Landscape.

Alle Komponenten muessen Dark und Light Varianten haben. Alle Safety-/Alarm-Komponenten brauchen Normal, Warning, Critical, Disabled und Muted States.

## 25. Akzeptanzkriterien

Ein Design ist passend, wenn:

- Der erste Blick wie ein echtes Marine-Instrument wirkt.
- Dashboard und Karte sofort als Hauptprodukt erkennbar sind.
- Der Seitenname in der Top-Bar zentral und klar lesbar ist.
- Die Widgets dichter als normale Mobile-Cards, aber nicht gedrungen wirken.
- Werte, Einheiten und Status nicht um Aufmerksamkeit konkurrieren.
- Alarme sofort erkennbar sind, ohne das ganze Interface chaotisch zu machen.
- Safety-relevante Aktionen bewusst und bestaetigt wirken.
- Phone und Tablet nicht nur skaliert, sondern ergonomisch angepasst sind.
- Es keine dekorativen Flaechen gibt, die keine Funktion oder Lesbarkeit verbessern.
- Ein Entwickler aus Figma Groessen, Farben, Zustaende und Interaktionen eindeutig ablesen kann.

## 26. Was nicht gewuenscht ist

- Keine Landingpage.
- Keine heroartigen Marketingbereiche.
- Keine grossen Illustrationskarten.
- Keine generischen Material-Design-Listen ohne Marine-Charakter.
- Keine Emoji-Menueicons.
- Keine neonlastige Sci-Fi-Aesthetik.
- Keine zu runden Web-App-Cards.
- Keine unklaren Safety-Zustaende.
- Keine versteckten Autopilot-Risiken.
- Keine kleine, schwer treffbare Cockpit-Bedienung.

