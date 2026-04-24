package com.seafox.nmea_dashboard.ui.widgets.chart.s57

/**
 * S-57 Object Class codes and their IHO names.
 * Only includes the most important nautical object types.
 */
object S57ObjectCodes {

    private val codeToName = mapOf(
        1 to "ADMARE",   // Administration Area
        2 to "AIRARE",   // Airport Area
        3 to "ACHBRT",   // Anchor Berth
        4 to "ACHARE",   // Anchorage Area
        5 to "BCNCAR",   // Beacon Cardinal
        6 to "BCNISD",   // Beacon Isolated Danger
        7 to "BCNLAT",   // Beacon Lateral
        8 to "BCNSAW",   // Beacon Safe Water
        9 to "BCNSPP",   // Beacon Special Purpose
        10 to "BERTHS",  // Berth
        11 to "CAUSWY",  // Causeway
        12 to "BRIDGE",  // Bridge
        13 to "BUISGL",  // Building Single
        14 to "BUAARE",  // Built-up Area
        15 to "CBLARE",  // Cable Area
        16 to "CBLOHD",  // Cable Overhead
        17 to "BOYCAR",  // Buoy Cardinal
        18 to "BOYINB",  // Buoy Installation
        19 to "BOYISD",  // Buoy Isolated Danger
        20 to "BOYLAT",  // Buoy Lateral
        21 to "BOYSAW",  // Buoy Safe Water
        22 to "BOYSPP",  // Buoy Special Purpose
        23 to "CANALS",  // Canal
        24 to "CTSARE",  // Cargo Transhipment Area
        25 to "CGUSTA",  // Coastguard Station
        26 to "CHKPNT",  // Checkpoint
        27 to "COALNE",  // Coastline
        28 to "CONZNE",  // Contiguous Zone
        29 to "CONVYR",  // Conveyor
        30 to "CTNARE",  // Caution Area
        31 to "CRANES",  // Crane
        32 to "CURONE",  // Current Non-gravitational
        33 to "DAMCON",  // Dam
        34 to "DAYMAR",  // Daymark
        35 to "DWRTCL",  // Deep Water Route Centerline
        36 to "DWRTPT",  // Deep Water Route Part
        37 to "DISMAR",  // Distance Mark
        38 to "DOCARE",  // Dock Area (incorrectly 39 above)
        39 to "DRGARE",  // Dredged Area
        40 to "DRYDOC",  // Dry Dock
        41 to "DYKCON",  // Dyke
        42 to "DEPARE",  // Depth Area
        43 to "DEPCNT",  // Depth Contour
        44 to "DUNARE",  // Dune Area (not DRYDOC)
        45 to "DMPGRD",  // Dumping Ground (fix: was 46)
        46 to "DMPGRD",  // Dumping Ground (keep both for compat)
        47 to "ELFARE",  // Elevated Ice Feature
        48 to "EXEZNE",  // Exclusive Economic Zone
        49 to "FAIRWY",  // Fairway
        50 to "FLODOC",  // Floating Dock
        51 to "FERYRT",  // Ferry Route
        52 to "FNCLNE",  // Fence Line
        53 to "FSHFAC",  // Fishing Facility
        54 to "GATCON",  // Gate
        55 to "GRIDRN",  // Gridiron
        56 to "HRBARE",  // Harbour Area (note: some specs use 64)
        57 to "HRBFAC",  // Harbour Facility
        58 to "FOGSIG",  // Fog Signal
        59 to "FSHGRD",  // Fishing Ground
        60 to "FSHZNE",  // Fishery Zone
        61 to "ICEARE",  // Ice Area
        62 to "ICNARE",  // Incineration Area
        63 to "ISTZNE",  // Inshore Traffic Zone
        64 to "HRBARE",  // Harbour Area
        65 to "HRBFAC",  // Harbour Facility
        66 to "HULKES",  // Hulk
        67 to "ICEARE",  // Ice Area
        68 to "ICNARE",  // Incineration Area
        69 to "LAKARE",  // Lake
        70 to "LNDELT",  // Land Delta (not standard?)
        71 to "LNDARE",  // Land Area
        72 to "LNDELV",  // Land Elevation
        73 to "LNDRGN",  // Land Region
        74 to "LNDMRK",  // Landmark
        75 to "LIGHTS",  // Light
        76 to "LITFLT",  // Light Float
        77 to "LITVES",  // Light Vessel
        78 to "LOCMAG",  // Local Magnetic Anomaly
        79 to "LOKBSN",  // Lock Basin
        80 to "LOGPON",  // Log Pond
        81 to "MAGVAR",  // Magnetic Variation
        82 to "MARCUL",  // Marine Farm/Culture
        83 to "MIPARE",  // Military Practice Area
        84 to "MORFAC",  // Mooring Facility (note: some specs use 86)
        85 to "MIPARE",  // Military Practice Area
        86 to "MORFAC",  // Mooring Facility
        87 to "NAVLNE",  // Navigation Line
        88 to "NMKARE",  // No-mark Area (non-standard)
        89 to "OBSTRN",  // Obstruction
        90 to "OSPARE",  // Offshore Production Area
        91 to "OFSPLF",  // Offshore Platform
        92 to "PIPARE",  // Pipeline Area
        93 to "PIPOHD",  // Pipeline Overhead
        94 to "PIPSOL",  // Pipeline Submarine/on Land
        95 to "PILBOP",  // Pilot Boarding Place
        96 to "PILPNT",  // Pile
        97 to "PRDARE",  // Production/Storage Area
        98 to "PYLONS",  // Pylon/Bridge Support
        99 to "RADLNE",  // Radar Line
        100 to "PRCARE", // Precautionary Area
        101 to "RADRFL", // Radar Reflector
        102 to "RADRNG", // Radar Range
        103 to "RESARE", // Restricted Area
        104 to "RETRFL", // Retro-reflector
        105 to "RIVERS", // River
        106 to "RMPARE", // Ramp Area
        107 to "ROADWY", // Roadway
        108 to "RUNWAY", // Runway
        109 to "RTPBCN", // Radar Transponder Beacon
        110 to "RDOCAL", // Radio Calling-in Point
        111 to "SEAARE", // Sea Area
        112 to "SBDARE", // Seabed Area
        113 to "SILTNK", // Silo/Tank
        114 to "SLCONS", // Shoreline Construction
        115 to "SLOGRD", // Sloping Ground
        116 to "SLOTOP", // Slope Top Line
        117 to "SNDWAV", // Sand Waves
        118 to "SPLARE", // Sea-Plane Landing Area
        119 to "SPRING", // Spring
        120 to "STSLNE", // Straight Territorial Sea Baseline
        121 to "SOUNDG", // Sounding
        122 to "SWPARE", // Swept Area
        123 to "TESARE", // Territorial Sea Area
        124 to "TIDEWY", // Tideway
        125 to "T_HMON", // Tide - Harmonic Prediction
        126 to "T_NHMN", // Tide - Non-harmonic Prediction
        127 to "T_TIMS", // Tide - Time Series
        128 to "TIDEWY", // Tideway (alt)
        129 to "TOPMAR", // Top Mark
        130 to "TSELNE", // TSS - Traffic Separation Line
        131 to "TSSBND", // TSS Boundary
        132 to "TSSCRS", // TSS Crossing
        133 to "TSSLPT", // TSS Lane Part
        134 to "TSSRON", // TSS Roundabout
        135 to "VEGATN", // Vegetation
        136 to "WATTUR", // Water Turbulence
        137 to "WATFAL", // Waterfall
        138 to "WEDKLP", // Weed/Kelp
        139 to "WRECKS", // Wreck (alt)
        140 to "TS_PAD", // Tidal Stream Panel Data
        141 to "TS_PNH", // Tidal Stream Non-harmonic
        142 to "TS_PRH", // Tidal Stream Predicted Harmonic
        143 to "TSSLPT", // TSS Lane Part (alt)
        144 to "TSSRON", // TSS Roundabout (alt)
        145 to "TSSBND", // TSS Boundary (alt)
        146 to "TSSCRS", // TSS Crossing (alt)
        147 to "NOTMRK", // Notice Mark
        148 to "TUNNEL", // Tunnel
        149 to "UNKOBJ", // Unknown Object
        150 to "LNDMRK", // Landmark (alt)
        151 to "WATFAL", // Waterfall (alt)
        152 to "WATTUR", // Water Turbulence (alt)
        153 to "UWTROC", // Underwater Rock
        154 to "TS_PAD", // Tidal Stream Panel Data
        155 to "TS_PNH", // Tidal Stream Non-harmonic Prediction
        156 to "TS_PRH", // Tidal Stream Harmonic Prediction
        157 to "TS_FEB", // Tidal Stream - Flood/Ebb
        158 to "TS_TIS", // Tidal Stream - Time Series
        159 to "WRECKS", // Wreck
        300 to "M_COVR", // Coverage
        301 to "M_NSYS", // Navigational System
        302 to "M_QUAL", // Quality of Data
        305 to "M_SDAT", // Sounding Datum
        306 to "M_ACCY", // Accuracy of Data
        308 to "M_CSCL", // Compilation Scale
        309 to "M_HOPA", // Horizontal Position Accuracy
        400 to "C_AGGR", // Aggregation
        401 to "C_ASSO", // Association
        402 to "C_STAC", // Stacked On/Off
    )

    fun nameForCode(code: Int): String = codeToName[code] ?: "UNK_$code"

    /** Object codes that should be rendered as nautical features */
    val NAVIGATIONAL_CODES = setOf(
        // Topography
        "COALNE", "LNDARE", "LAKARE", "RIVERS", "SLCONS", "BRIDGE",
        "CAUSWY", "DAMCON", "DYKCON", "CANALS", "TUNNEL",
        // Hydrography
        "DEPARE", "DEPCNT", "SOUNDG", "DRGARE", "SBDARE", "SWPARE",
        "SNDWAV", "SPRING",
        // Dangers
        "OBSTRN", "WRECKS", "UWTROC", "WATTUR",
        // Aids to Navigation
        "BOYCAR", "BOYLAT", "BOYSAW", "BOYSPP", "BOYISD", "BOYINB",
        "BCNCAR", "BCNLAT", "BCNSAW", "BCNSPP", "BCNISD",
        "LIGHTS", "LITFLT", "LITVES", "FOGSIG", "TOPMAR", "DAYMAR",
        "RTPBCN", "RADRFL", "RDOCAL", "NOTMRK", "LNDMRK",
        // Regulated Areas
        "ACHARE", "ACHBRT", "RESARE", "CTNARE", "MIPARE", "PRCARE",
        "DMPGRD", "FSHZNE", "FSHGRD", "ISTZNE", "OSPARE", "PILBOP",
        // Traffic
        "NAVLNE", "FAIRWY", "TSSLPT", "TSSRON", "TSSBND", "TSSCRS",
        "TSELNE", "DWRTCL", "DWRTPT", "FERYRT",
        // Port facilities
        "HRBARE", "HRBFAC", "MORFAC", "BERTHS", "DOCARE", "DRYDOC",
        "FLODOC", "LOKBSN", "CRANES", "PYLONS", "OFSPLF",
        // Pipelines & cables
        "PIPARE", "PIPSOL", "PIPOHD", "CBLARE", "CBLOHD",
        // Other
        "BUISGL", "VEGATN", "PRDARE", "ROADWY", "RUNWAY",
        "LNDRGN", "LNDELV", "SEAARE", "BUAARE",
    )
}
