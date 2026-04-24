package com.seafox.nmea_dashboard.data

data class WidgetDefinition(
    val metadata: WidgetMetadata,
    val sectionType: WidgetCatalogSectionType,
    val menuLabel: String,
)

data class WidgetCatalogBlueprint(
    val definitions: List<WidgetDefinition>,
)
