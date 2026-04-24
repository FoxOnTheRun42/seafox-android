package com.seafox.nmea_dashboard.data

data class WidgetMetadata(
    val kind: WidgetKind,
    val title: String,
    val defaultDataKeys: List<String> = emptyList(),
    val defaultWidthPx: Float,
    val defaultHeightPx: Float,
    val defaultMinGridUnits: Pair<Int, Int> = 1 to 1,
    val helpLines: List<String> = emptyList(),
)

enum class WidgetCatalogSectionType {
    NAVIGATION,
    ENERGY_AND_TANKS,
    POWER,
    SAFETY,
    SYSTEM,
}

data class WidgetCatalogEntry(
    val kind: WidgetKind,
    val menuLabel: String,
)

data class WidgetCatalogSection(
    val id: String,
    val title: String,
    val type: WidgetCatalogSectionType,
    val entries: List<WidgetCatalogEntry>,
)

private val WIDGET_METADATA: Map<WidgetKind, WidgetMetadata> = mapOf(
    WidgetKind.BATTERY to WidgetMetadata(
        kind = WidgetKind.BATTERY,
        title = "Batterie",
        defaultDataKeys = listOf("battery_soc", "battery", "battery_level", "soc"),
        defaultWidthPx = 300f,
        defaultHeightPx = 170f,
        defaultMinGridUnits = 2 to 4,
        helpLines = listOf(
            "Der Prozentwert im Batteriesymbol zeigt den Ladezustand.",
            "Der Leistungswert unten links zeigt Laden (+W) oder Entladen (-W).",
            "Bei Lithium erscheint zusätzlich ΔZelle als Zellspannungs-Differenz in mV.",
            "Im Menü \"Batterietyp\" legst du fest, ob Bleisäure oder Lithium angezeigt wird."
        ),
    ),
    WidgetKind.WATER_TANK to WidgetMetadata(
        kind = WidgetKind.WATER_TANK,
        title = "Wassertank",
        defaultDataKeys = listOf("water_tank", "water", "tank_level", "fresh_water"),
        defaultWidthPx = 260f,
        defaultHeightPx = 180f,
        defaultMinGridUnits = 2 to 4,
        helpLines = listOf(
            "Die Prozentzahl zeigt den Füllstand des Wassertanks.",
            "Der Balken im Tank steigt oder fällt mit dem Füllstand.",
            "Die Darstellung ist auf Frischwasser (blau) ausgelegt.",
        ),
    ),
    WidgetKind.BLACK_WATER_TANK to WidgetMetadata(
        kind = WidgetKind.BLACK_WATER_TANK,
        title = "Schwarzwasser",
        defaultDataKeys = listOf("black_water_tank", "black_water", "waste_black", "black_tank"),
        defaultWidthPx = 260f,
        defaultHeightPx = 180f,
        defaultMinGridUnits = 2 to 4,
        helpLines = listOf(
            "Die Prozentzahl zeigt den Füllstand des Schwarzwassertanks.",
            "Der Tankbalken visualisiert den aktuellen Füllstand.",
            "Je höher der Wert, desto voller der Tank.",
        ),
    ),
    WidgetKind.GREY_WATER_TANK to WidgetMetadata(
        kind = WidgetKind.GREY_WATER_TANK,
        title = "Grauwasser",
        defaultDataKeys = listOf("grey_water_tank", "gray_water_tank", "grey_water", "gray_water"),
        defaultWidthPx = 260f,
        defaultHeightPx = 180f,
        defaultMinGridUnits = 2 to 4,
        helpLines = listOf(
            "Die Prozentzahl zeigt den Füllstand des Grauwassertanks.",
            "Der Tankbalken visualisiert den aktuellen Füllstand.",
            "Je höher der Wert, desto voller der Tank.",
        ),
    ),
    WidgetKind.WIND to WidgetMetadata(
        kind = WidgetKind.WIND,
        title = "Wind",
        defaultDataKeys = listOf("wind_speed", "windangle", "wind_angle"),
        defaultWidthPx = 320f,
        defaultHeightPx = 220f,
        defaultMinGridUnits = 8 to 7,
        helpLines = listOf(
            "Der Pfeil zeigt die Windrichtung relativ zum Boot.",
            "Blau kennzeichnet im Wind-Widget den wahren Wind (TWA/TWS).",
            "Die roten/grünen Bögen markieren den eingestellten Wendebereich.",
            "Unten links steht die Windrichtung relativ zu Nord (°/N).",
            "Unten rechts wird die Windgeschwindigkeit angezeigt.",
            "In \"Wind-Einstellungen\" konfigurierst du Anzeigeelemente, Einheit und Wendewinkel."
        ),
    ),
    WidgetKind.COMPASS to WidgetMetadata(
        kind = WidgetKind.COMPASS,
        title = "Kompass",
        defaultDataKeys = listOf(
            "heading",
            "autopilot_heading",
            "navigation.course_over_ground_true",
            "course_over_ground_true",
            "course_over_ground",
        ),
        defaultWidthPx = 280f,
        defaultHeightPx = 280f,
        defaultMinGridUnits = 8 to 7,
        helpLines = listOf(
            "Das Kompass-Widget zeigt Kurs (Heading) und Kurs über Grund (COG).",
            "Der blaue Zeiger ist der aktuelle Heading-Wert.",
            "Der gelbe Zeiger ist der Kurs über Grund.",
            "N, E, S, W sind als Richtungsmarken eingezeichnet."
        ),
    ),
    WidgetKind.KARTEN to WidgetMetadata(
        kind = WidgetKind.KARTEN,
        title = "Karten",
        defaultDataKeys = listOf(
            "position.latitude",
            "position.longitude",
            "navigation.position.latitude",
            "navigation.position.longitude",
            "navigation.course_over_ground_true",
            "course_over_ground",
            "course",
            "heading",
            "sog",
            "navigation.speed_over_ground"
        ),
        defaultWidthPx = 360f,
        defaultHeightPx = 300f,
        defaultMinGridUnits = 8 to 8,
        helpLines = listOf(
            "Dieses Widget ist der neue, getrennte Kartenpfad für NOAA-Karten.",
            "Als erste Basis nutzt es die offizielle NOAA-Darstellung statt der bisherigen seaCHART-Vektorlogik.",
            "Der vorhandene NOAA-Kartenbereich wird direkt als Online-Karte geladen.",
        ),
    ),
    WidgetKind.GPS to WidgetMetadata(
        kind = WidgetKind.GPS,
        title = "GPS",
        defaultDataKeys = listOf(
            "navigation.position.latitude",
            "position.latitude",
            "latitude",
            "lat",
            "navigation.position.longitude",
            "position.longitude",
            "longitude",
            "lon",
            "lng",
            "navigation.course_over_ground_true",
            "course_over_ground",
            "cog",
            "navigation.speed_over_ground",
            "speed_over_ground",
            "sog",
            "heading",
            "autopilot_heading",
            "gps_altitude_m",
            "altitude_m",
            "altitude",
            "gps_satellites",
            "satellites",
            "gps_hdop",
            "gps_fix_quality"
        ),
        defaultWidthPx = 360f,
        defaultHeightPx = 240f,
        defaultMinGridUnits = 8 to 8,
        helpLines = listOf(
            "Das GPS-Widget zeigt Position, Kurs, Fahrt, Höhe und Fix-Infos.",
            "LAT/LON werden als Grad mit Himmelsrichtungen angezeigt.",
            "Kurs ist der Kurs über Grund (COG), SOG die Geschwindigkeit über Grund.",
            "Satelliten und HDOP helfen bei der Qualitätseinschätzung."
        ),
    ),
    WidgetKind.TEMPERATURE to WidgetMetadata(
        kind = WidgetKind.TEMPERATURE,
        title = "Temperatur",
        defaultDataKeys = listOf(
            "temperature_sensor_1",
            "temperature_sensor_2",
            "temperature_sensor_3",
            "temperature_sensor_4",
            "temperature_sensor_5",
            "temperature_sensor_6",
            "temperature_sensor_7",
            "temperature_sensor_8",
            "temperature_sensor_9",
            "temperature_sensor_10",
        ),
        defaultWidthPx = 360f,
        defaultHeightPx = 300f,
        defaultMinGridUnits = 6 to 6,
        helpLines = listOf(
            "Das Temperatur-Widget zeigt bis zu 10 NMEA2000-Temperatursensoren.",
            "Namen der Sensoren kannst du im Widget-Menü frei anpassen.",
            "Mit der Einheit wählst du zwischen °C und °F."
        ),
    ),
    WidgetKind.AIS to WidgetMetadata(
        kind = WidgetKind.AIS,
        title = "AIS",
        defaultDataKeys = listOf(
            "ais_mmsi",
            "mmsi",
            "ais_distance_nm",
            "ais_range_nm",
            "ais_relative_distance_nm",
            "ais_relative_bearing_deg",
            "ais_relative_bearing",
            "ais_bearing",
            "ais_bearing_deg",
            "ais_target_bearing",
            "ais_cpa_nm",
            "ais_time_to_cpa_min",
            "ais_time_to_cpa",
            "ais_cog",
            "ais_sog",
            "ais_heading",
            "ais_latitude",
            "ais_longitude",
            "ais_target_latitude",
            "ais_target_longitude",
            "ais_nav_status"
        ),
        defaultWidthPx = 360f,
        defaultHeightPx = 240f,
        defaultMinGridUnits = 8 to 8,
        helpLines = listOf(
            "Das AIS-Widget zeigt Daten zu einem erkannten AIS-Ziel.",
            "Wird kein AIS-Datensatz empfangen, bleibt die Anzeige als Platzhalter bestehen.",
            "Erwartete Felder sind MMSI, Distanz (NM), CPA, CPA-Zeit, Kurs und Geschwindigkeit.",
            "Bei mehreren Zielen zeigt der aktuelle Datensatz im Stream den letzten bekannten AIS-Wert."
        ),
    ),
    WidgetKind.DALY_BMS to WidgetMetadata(
        kind = WidgetKind.DALY_BMS,
        title = "DALY BMS (Beta)",
        defaultDataKeys = listOf(
            "daly_total_voltage",
            "daly_current",
            "daly_power",
            "daly_state_of_charge",
            "daly_cell_count",
            "daly_capacity_remaining",
            "daly_temperature_1",
            "daly_battery_state",
            "daly_connection_status",
            "daly_last_update",
        ),
        defaultWidthPx = 340f,
        defaultHeightPx = 220f,
        defaultMinGridUnits = 4 to 4,
        helpLines = listOf(
            "DALY-BMS ist Beta; automatische BLE-Verbindungen sind in dieser Produktstufe deaktiviert.",
            "Das Widget zeigt nur Werte an, die bereits als Telemetrie im Dashboard ankommen.",
            "Für direkte DALY-BLE-Nutzung braucht es später eine explizite Gerätefreigabe und Fehlerdiagnose.",
            "Zeigt Spannungen, Strom, SOC, Zell- und Temperaturdaten sowie letzte gültige Rohframe-Informationen.",
        ),
    ),
    WidgetKind.AUTOPILOT to WidgetMetadata(
        kind = WidgetKind.AUTOPILOT,
        title = "Autopilot",
        defaultDataKeys = listOf("autopilot_heading", "heading", "rudder", "autopilot_mode"),
        defaultWidthPx = 320f,
        defaultHeightPx = 220f,
        defaultMinGridUnits = 8 to 7,
        helpLines = listOf(
            "Oben wählst du den Modus WIND, STBY oder KURS.",
            "Links siehst du Ist-Kurs (groß) und Sollkurs (darunter, farblich hervorgehoben).",
            "Rechts zeigt Ruder den aktuellen und AVR den gemittelten Ruderausschlag.",
            "Mit ⇄ löst du ein Wenden auf den eingestellten Wendewinkel aus.",
            "Mit -1/-10 und +1/+10 änderst du den Sollkurs schrittweise.",
            "In \"Autopilot-Einstellungen\" konfigurierst du Zielgerät und Gateway."
        ),
    ),
    WidgetKind.ENGINE_RPM to WidgetMetadata(
        kind = WidgetKind.ENGINE_RPM,
        title = "Motordrehzahl",
        defaultDataKeys = listOf("engine_rpm", "rpm", "engine_rpm1"),
        defaultWidthPx = 270f,
        defaultHeightPx = 170f,
        defaultMinGridUnits = 2 to 2,
        helpLines = listOf(
            "Die Zahl zeigt die aktuelle Motordrehzahl in rpm.",
            "Der Balken darunter visualisiert die Drehzahl relativ zum Maximalbereich."
        ),
    ),
    WidgetKind.ECHOSOUNDER to WidgetMetadata(
        kind = WidgetKind.ECHOSOUNDER,
        title = "Deep",
        defaultDataKeys = listOf("water_depth_m", "water_depth", "depth_m"),
        defaultWidthPx = 320f,
        defaultHeightPx = 220f,
        defaultMinGridUnits = 8 to 7,
        helpLines = listOf(
            "Das Deep-Widget zeigt die aktuelle Wassertiefe an.",
            "Der Alarm wird aus dem Wert \"Mindesttiefe\" plus einem dynamischen Anteil aus der Tiefenänderung berechnet.",
            "Der Punkt „Tiefendynamik\" steuert den Zusatzsicherheitsabstand: bei schnellerer Abwärtsbewegung wird die Alarmschwelle frühzeitiger nach oben gesetzt.",
            "Beispiel: Steigt die Tiefe pro Sekunde deutlich ab, erhöht sich der effektive Alarm, um Vorwarnzeit zu schaffen.",
            "Bei gleichmäßiger Fahrt ohne zusätzliche Abwärtsbewegung liegt die Grenze praktisch bei der eingestellten Mindesttiefe.",
            "Im Untermenü kannst du Mindesttiefe, Tiefendynamik, Einheit und Alarmton einstellen."
        ),
    ),
    WidgetKind.LOG to WidgetMetadata(
        kind = WidgetKind.LOG,
        title = "Speed",
        defaultDataKeys = listOf(
            "navigation.speed_over_ground",
            "speed_over_ground",
            "sog",
            "speed",
            "navigation.speed_through_water",
            "speed_through_water",
            "stw",
            "water_speed",
        ),
        defaultWidthPx = 320f,
        defaultHeightPx = 220f,
        defaultMinGridUnits = 8 to 7,
        helpLines = listOf(
            "Das Speed-Widget zeigt die aktuelle Fahrtgeschwindigkeit.",
            "Die Werte Top-/Min-Speed beziehen sich auf den einstellbaren Zeitraum.",
            "Tagesmeilen und Trip werden aus den empfangenen Geschwindigkeitswerten berechnet.",
            "Im Widget-Menü kannst du zwischen GPS (SOG) und Log (STW) wechseln.",
            "SOG = Speed over Ground = GPS-basierte Fahrt über Grund.",
            "STW = Speed through Water = Fahrt durchs Wasser (Log-/Rudersensor)."
        ),
    ),
    WidgetKind.SYSTEM_PERFORMANCE to WidgetMetadata(
        kind = WidgetKind.SYSTEM_PERFORMANCE,
        title = "System",
        defaultDataKeys = emptyList(),
        defaultWidthPx = 360f,
        defaultHeightPx = 250f,
        defaultMinGridUnits = 6 to 6,
        helpLines = listOf(
            "Das System-Widget zeigt aktuelle CPU- und RAM-Auslastung der App an.",
            "Es berechnet zusätzlich eine geschätzte Widget-Last nach Auslastungs-Anteil.",
            "Höhere Prozentwerte bedeuten typischerweise mehr Rechenaufwand auf dem Tablet.",
            "Die Werte sind Richtwerte und helfen bei der Optimierung von Widget-Anzahl und Layout."
        ),
    ),
    WidgetKind.ANCHOR_WATCH to WidgetMetadata(
        kind = WidgetKind.ANCHOR_WATCH,
        title = "Ankerwache",
        defaultDataKeys = listOf(
            "anchor_chain_length",
            "anchor_chain",
            "chain_length",
            "anchor_line_length",
            "position.latitude",
            "position.longitude",
            "navigation.position.latitude",
            "navigation.position.longitude",
        ),
        defaultWidthPx = 320f,
        defaultHeightPx = 220f,
        defaultMinGridUnits = 6 to 6,
        helpLines = listOf(
            "Das Ankerwache-Widget überwacht Kettenlänge und Ankerposition.",
            "Ist ein Kettenwert vorhanden, wird die Überwachung automatisch aktiviert.",
            "Lege den Ankerpunkt automatisch beim Erreichen von >5 m Kette fest.",
            "Alarm bei Abstand über Kettenlänge + einstellbarem Korrekturbereich.",
        )
    ),
    WidgetKind.NMEA_PGN to WidgetMetadata(
        kind = WidgetKind.NMEA_PGN,
        title = "PGN Empfang",
        defaultDataKeys = listOf(
            "n2k_source",
            "n2k_pgn",
            "n2k_payload_len",
            "n2k_payload_hex"
        ),
        defaultWidthPx = 360f,
        defaultHeightPx = 240f,
        defaultMinGridUnits = 6 to 6,
        helpLines = listOf(
            "Das Widget zeigt die letzten NMEA2000-PGN-Meldungen auf einen Blick.",
            "Unter Quellen werden erkannte Sender inkl. PGN-Liste und letzter Meldungszeit angezeigt.",
            "Für jedes neue NMEA2000-Paket mit gültiger Quelle und PGN wird der Datensatz aktualisiert.",
            "Unbekannte PGNs werden als reine Nummer angezeigt.",
        ),
    ),
    WidgetKind.NMEA0183 to WidgetMetadata(
        kind = WidgetKind.NMEA0183,
        title = "NMEA0183 Empfang",
        defaultDataKeys = listOf(
            "nmea0183_raw_line",
            "nmea0183_sentence",
            "nmea0183_sentence_full",
            "nmea0183_category",
            "nmea_sentence",
            "nmea_type",
        ),
        defaultWidthPx = 360f,
        defaultHeightPx = 280f,
        defaultMinGridUnits = 6 to 6,
        helpLines = listOf(
            "Das Widget zeigt empfangene NMEA0183-Sätze im Klartext.",
            "Für jeden Satz werden Satztyp, Quelle, Kategorie, Rohzeile und aufbereitete Felder angezeigt.",
            "Zusätzlich findest du hier die zuletzt empfangenen Sätze mit Empfangszeit.",
        ),
    ),
)

private val WIDGET_SECTIONS: List<WidgetCatalogSection> = listOf(
    WidgetCatalogSection(
        id = "navigation",
        title = "Navigation",
        type = WidgetCatalogSectionType.NAVIGATION,
        entries = listOf(
            WidgetCatalogEntry(WidgetKind.WIND, "Wind"),
            WidgetCatalogEntry(WidgetKind.COMPASS, "Kompass"),
            WidgetCatalogEntry(WidgetKind.KARTEN, "Karten"),
            WidgetCatalogEntry(WidgetKind.LOG, "Speed"),
            WidgetCatalogEntry(WidgetKind.GPS, "GPS"),
            WidgetCatalogEntry(WidgetKind.NMEA_PGN, "PGN Empfang"),
            WidgetCatalogEntry(WidgetKind.AUTOPILOT, "Autopilot"),
            WidgetCatalogEntry(WidgetKind.ECHOSOUNDER, "Deep"),
            WidgetCatalogEntry(WidgetKind.TEMPERATURE, "Temperatur"),
            WidgetCatalogEntry(WidgetKind.NMEA_PGN, "PGN Empfang"),
        ),
    ),
    WidgetCatalogSection(
        id = "safety",
        title = "Sicherheit",
        type = WidgetCatalogSectionType.SAFETY,
        entries = listOf(
            WidgetCatalogEntry(WidgetKind.AIS, "AIS"),
            WidgetCatalogEntry(WidgetKind.ANCHOR_WATCH, "Ankerwache"),
        ),
    ),
    WidgetCatalogSection(
        id = "system",
        title = "System",
        type = WidgetCatalogSectionType.SYSTEM,
        entries = listOf(
            WidgetCatalogEntry(WidgetKind.NMEA0183, "NMEA0183 Empfang"),
            WidgetCatalogEntry(WidgetKind.SYSTEM_PERFORMANCE, "Systemauslastung"),
            WidgetCatalogEntry(WidgetKind.DALY_BMS, "DALY BMS (Beta)"),
        ),
    ),
    WidgetCatalogSection(
        id = "energy",
        title = "Energie und Tankstände",
        type = WidgetCatalogSectionType.ENERGY_AND_TANKS,
        entries = listOf(
            WidgetCatalogEntry(WidgetKind.WATER_TANK, "Wassertank 1"),
            WidgetCatalogEntry(WidgetKind.BLACK_WATER_TANK, "Schwarzwasser"),
            WidgetCatalogEntry(WidgetKind.GREY_WATER_TANK, "Grauwasser"),
            WidgetCatalogEntry(WidgetKind.BATTERY, "Batterie"),
        ),
    ),
    WidgetCatalogSection(
        id = "engine",
        title = "Antrieb",
        type = WidgetCatalogSectionType.POWER,
        entries = listOf(
            WidgetCatalogEntry(WidgetKind.ENGINE_RPM, "Motordrehzahl"),
        ),
    ),
)

private val WIDGET_BLUEPRINT: WidgetCatalogBlueprint = WidgetCatalogBlueprint(
    definitions = WIDGET_SECTIONS.flatMap { section ->
        section.entries.mapNotNull { entry ->
            widgetMetadata(entry.kind)?.let { metadata ->
                WidgetDefinition(
                    metadata = metadata,
                    sectionType = section.type,
                    menuLabel = entry.menuLabel,
                )
            }
        }
    },
)

private val WIDGET_MENU_LABEL_BY_KIND: Map<WidgetKind, String> = WIDGET_SECTIONS
    .flatMap { section -> section.entries }
    .associate { it.kind to it.menuLabel }

private fun canonicalWidgetKind(kind: WidgetKind): WidgetKind = when (kind) {
    WidgetKind.SEA_CHART,
    WidgetKind.SEA_CHART_PIXEL -> WidgetKind.KARTEN
    else -> kind
}

fun widgetCatalogSections(): List<WidgetCatalogSection> = WIDGET_SECTIONS

fun widgetMetadata(kind: WidgetKind): WidgetMetadata = WIDGET_METADATA[canonicalWidgetKind(kind)]
    ?: throw IllegalArgumentException("Widget-Metadaten nicht vorhanden: $kind")

fun widgetTitleForKind(kind: WidgetKind): String = widgetMetadata(kind).title

fun widgetMenuLabelForKind(kind: WidgetKind): String = WIDGET_MENU_LABEL_BY_KIND[kind] ?: widgetTitleForKind(kind)

fun widgetDefaultDataKeys(kind: WidgetKind): List<String> = widgetMetadata(kind).defaultDataKeys

fun widgetDefaultSizePx(kind: WidgetKind): Pair<Float, Float> = with(widgetMetadata(kind)) {
    defaultWidthPx to defaultHeightPx
}

fun widgetDefaultMinGridUnits(kind: WidgetKind): Pair<Int, Int> = widgetMetadata(kind).defaultMinGridUnits

fun widgetHelpLines(kind: WidgetKind): List<String> = widgetMetadata(kind).helpLines

fun widgetDefinitions(): List<WidgetDefinition> = WIDGET_BLUEPRINT.definitions

fun widgetCatalogBlueprint(): WidgetCatalogBlueprint = WIDGET_BLUEPRINT
