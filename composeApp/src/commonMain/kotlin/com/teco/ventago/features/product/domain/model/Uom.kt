package com.teco.ventago.features.product.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Uom(
    val code: String,
    val nameEs: String,
    val category: String,
    val symbol: String,
    val comment: String? = null
) {
    // Length
    UM("um", "Micrómetro", "Length", "µm", "10^-6 m"),
    MM("mm", "Milímetro", "Length", "mm", "10^-3 m"),
    CM("cm", "Centímetro", "Length", "cm", "10^-2 m"),
    DM("dm", "Decímetro", "Length", "dm", "10^-1 m"),
    M("m", "Metro", "Length", "m"),
    DAM("dam", "Decámetro", "Length", "dam", "10 m"),
    HM("hm", "Hectómetro", "Length", "hm", "100 m"),
    KM("km", "Kilómetro", "Length", "km", "1000 m"),
    MI("mi", "Milla internacional", "Length", "mi", "1609.3 m"),
    NMI("nmi", "Milla náutica", "Length", "nmi", "1.852 km"),
    IN("in", "Pulgada", "Length", "in", "2.54 cm"),
    FT("ft", "Pie", "Length", "ft", "12 in = 30.48 cm"),
    YD("yd", "Yarda", "Length", "yd", "3 ft = 91.44 cm"),

    // Area
    MM2("mm2", "Milímetro cuadrado", "Area", "mm²"),
    CM2("cm2", "Centímetro cuadrado", "Area", "cm²"),
    DM2("dm2", "Decímetro cuadrado", "Area", "dm²"),
    M2("m2", "Metro cuadrado", "Area", "m²"),
    HA("ha", "Hectárea", "Area", "ha", "10,000 m²"),
    KM2("km2", "Kilómetro cuadrado", "Area", "km²"),
    ACRE("acre", "Acre", "Area", "acre", "4046.8726 m²"),
    MI2("mi2", "Milla cuadrada", "Area", "mi²"),
    PING("ping", "Ping", "Area", "Ping", "≈3.3 m²"),
    IN2("in2", "Pulgada cuadrada", "Area", "in²"),
    FT2("ft2", "Pie cuadrado", "Area", "ft²"),
    YD2("yd2", "Yarda cuadrada", "Area", "yd²"),

    // Volume (metric)
    UL("ul", "Microlitro", "Volume", "µl", "10^-6 l"),
    ML("ml", "Mililitro", "Volume", "ml", "10^-3 l"),
    CL("cl", "Centilitro", "Volume", "cl", "10^-2 l"),
    DL("dl", "Decilitro", "Volume", "dl", "10^-1 l"),
    L("l", "Litro", "Volume", "l"),
    DAL("dal", "Decalitro", "Volume", "dal", "10 l"),
    HL("hl", "Hectolitro", "Volume", "hl", "100 l"),
    KL("kl", "Kilolitro", "Volume", "kl", "1000 l"),
    MM3("mm3", "Milímetro cúbico", "Volume", "mm³", "10^-6 l"),
    CM3("cm3", "Centímetro cúbico", "Volume", "cm³", "10^-3 l"),
    DM3("dm3", "Decímetro cúbico", "Volume", "dm³", "1 l"),
    M3("m3", "Metro cúbico", "Volume", "m³", "1000 l"),

    // Volume (US/UK liquids)
    FL_OZ("fl_oz", "Onza líquida EUA", "Volume", "fl oz", "29.5735 ml"),
    GILL("gill", "Gill (US)", "Volume", "gill", "118.294 ml"),
    PINT("pint", "Pinta líquida EUA", "Volume", "pt", "473.176 ml"),
    QUART("quart", "Cuarto EUA", "Volume", "qt", "0.946 l"),
    GAL("gal", "Galón EUA", "Volume", "gal", "3.785 l"),
    FL_OZ_UK("fl_oz_uk", "Onza líquida UK", "Volume", "fl oz (UK)", "28.413 ml"),
    GILL_UK("gill_uk", "Gill UK", "Volume", "gill (UK)", "142.065 ml"),
    PINT_UK("pint_uk", "Pinta UK", "Volume", "pt (UK)", "568.261 ml"),
    QUART_UK("quart_uk", "Cuarto UK", "Volume", "qt (UK)", "1.136 l"),
    GAL_UK("gal_uk", "Galón UK", "Volume", "gal (UK)", "4.546 l"),

    // Volume solids
    IN3("in3", "Pulgada cúbica", "Volume", "in³", "16.387 cm³"),
    FT3("ft3", "Pie cúbico", "Volume", "ft³", "28.316 dm³"),
    YD3("yd3", "Yarda cúbica", "Volume", "yd³", "764.554 dm³"),

    // Mass
    UG("ug", "Microgramo", "Mass", "µg", "10^-6 g"),
    MG("mg", "Miligramo", "Mass", "mg", "10^-3 g"),
    CG("cg", "Centigramo", "Mass", "cg", "10^-2 g"),
    DG("dg", "Decigramo", "Mass", "dg", "10^-1 g"),
    G("g", "Gramo", "Mass", "g"),
    DAG("dag", "Decagramo", "Mass", "dag", "10 g"),
    HG("hg", "Héctogramo", "Mass", "hg", "100 g"),
    KG("kg", "Kilogramo", "Mass", "kg"),
    T("t", "Tonelada", "Mass", "t", "1000 kg"),
    TON("ton", "Tonelada corta (US)", "Mass", "ton", "907.184 kg"),
    LTN("LTN", "Tonelada larga (UK)", "Mass", "LTN", "1016.046 kg"),
    TN("TN", "Tonelada de carga", "Mass", "TN", "t o m³; el mayor"),
    GR("gr", "Grain", "Mass", "gr", "≈64.79 mg"),
    DR("dr", "Dram", "Mass", "dr", "≈1.772 g"),
    OZ("oz", "Onza", "Mass", "oz", "28.35 g"),
    LB("lb", "Libra", "Mass", "lb", "453.592 g"),
    ST("st", "Stone", "Mass", "st", "14 lb"),
    QR("qr", "Quarter", "Mass", "qr", "28 lb"),
    CWT("cwt", "Hundredweight", "Mass", "cwt", "50.8 kg"),
    OZT("ozt", "Onza troy", "Mass", "ozt", "31.103 g"),
    LBT("lbt", "Libra troy", "Mass", "lbt", "373.24 g"),
    DWT("dwt", "Pennyweight", "Mass", "dwt", "1.55 g"),
    CT("ct", "Quilate", "Mass", "ct", "200 mg"),

    // Counting / packaging
    UND("und", "Unidad", "Counting", "und"),
    PERSONA("Persona", "Persona", "Counting", "Persona"),
    PERSONAS("Personas", "Personas", "Counting", "Personas"),
    CABEZA("Cabeza", "Cabeza", "Counting", "Cabeza"),
    GRUPO("Grupo", "Grupo", "Counting", "Grupo"),
    BOBINA("Bobina", "Bobina", "Counting", "Bobina"),
    DECENA("Decena", "Decena", "Counting", "Decena"),
    DOCENA("Docena", "Docena", "Counting", "Docena"),
    CIENTO("Ciento", "Ciento", "Counting", "Ciento"),
    MILLAR("Millar", "Millar", "Counting", "Millar"),
    VIAJE("Viaje", "Viaje", "Counting", "Viaje"),
    VISITA("Visita", "Visita", "Counting", "Visita"),
    PAQUETE("Paquete", "Paquete", "Counting", "Paquete"),
    KIT("Kit", "Kit", "Counting", "Kit"),
    BARRICA("Barrica", "Barrica", "Counting", "Barrica"),
    BARRIL("Barril", "Barril", "Counting", "Barril"),
    BLANCO("Blanco", "Blanco", "Counting", "Blanco"),
    BOLSO("Bolso", "Bolso", "Counting", "Bolso"),
    CAJA("Caja", "Caja", "Counting", "Caja"),
    CESTA("Cesta", "Cesta", "Counting", "Cesta"),

    // Heat
    BTU("BTU", "Unidad térmica británica", "Heat", "BTU", "≈1055.06 J"),

    // Informatics (subset)
    BIT("bit", "Bit", "Informatics", "bit"),
    BIT_S("bit_s", "Bits por segundo", "Informatics", "bit/s"),
    KBIT("kbit", "Kilobit", "Informatics", "kbit"),
    KBIT_S("kbit_s", "Kilobit por segundo", "Informatics", "kbit/s"),
    MBIT("Mbit", "Megabit", "Informatics", "Mbit"),
    MBIT_S("Mbit_s", "Megabit por segundo", "Informatics", "Mbit/s"),
    KBYTE("kbyte", "Kilobyte", "Informatics", "kbyte"),
    MBYTE("Mbyte", "Megabyte", "Informatics", "Mbyte"),
    GBYTE("Gbyte", "Gigabyte", "Informatics", "Gbyte"),
    KIBYTE("Kibyte", "Kibibyte", "Informatics", "Kibyte"),
    DPI("dpi", "Puntos por pulgada", "Informatics", "dpi"),

    // Communications
    LINEAS_ACCESO("LineasAcceso", "Líneas de acceso", "Communications", "Líneas acceso"),
    LINEAS_SERVICIO("LineasServicio", "Líneas en servicio", "Communications", "Líneas servicio"),
    LLAMADA("Llamada", "Llamada", "Communications", "Llamada"),
    MML("MML", "Minuto y medio por llamada", "Communications", "MML"),
    PUERTO("Puerto", "Puerto de telecomunicaciones", "Communications", "Puerto"),

    // Power / Energy
    W("W", "Watt", "Power", "W"),
    KW("kW", "Kilowatt", "Power", "kW"),
    MW("MW", "Megawatt", "Power", "MW"),
    GW("GW", "Gigawatt", "Power", "GW"),
    TW("TW", "Terawatt", "Power", "TW"),
    WH("Wh", "Watt hora", "Power", "Wh"),
    KWH("kWh", "Kilowatt hora", "Power", "kWh"),
    MWH("MWh", "Megawatt hora", "Power", "MWh"),
    GWH("GWh", "Gigawatt hora", "Power", "GWh"),
    TWH("TWh", "Terawatt hora", "Power", "TWh"),
    VA("VA", "Voltio-amperio", "Power", "VA"),
    KVA("kVA", "Kilovoltio-amperio", "Power", "kVA"),
    MVA("MVA", "Megavoltio-amperio", "Power", "MVA"),

    // Textile / Misc
    DENIER("Denier", "Denier", "Textile", "Denier"),
    LBB("lbb", "Libra de bateo", "Textile", "lbb"),
    MADEJA("Madeja", "Madeja", "Textile", "Madeja"),

    // Time
    ANO_COMUN("AnoComun", "Año común", "Time", "Año común"),
    MES("Mes", "Mes", "Time", "Mes"),
    DIA("Dia", "Día", "Time", "Día"),
    HORA("Hora", "Hora", "Time", "Hora"),
    MIN("Min", "Minuto", "Time", "Min"),
    SEG("Seg", "Segundo", "Time", "Seg"),

    // Work
    SEMANA("Semana", "Semana", "Work", "Semana"),
    ACTIVIDAD("Actividad", "Actividad", "Work", "Actividad"),
    HORA_TRABAJO("HoraTrabajo", "Hora de trabajo", "Work", "Hora trabajo"),
    HORA_EXTRA("HoraExtra", "Hora extra", "Work", "Hora extra"),
    MES_TRABAJO("MesTrabajo", "Mes de trabajo", "Work", "Mes trabajo"),
    WORK_PERSONAS("WorkPersonas", "Personas", "Work", "Personas"),
    PUESTO("Puesto", "Puesto de trabajo", "Work", "Puesto"),
}

object UomRegistry {

    // Fast lookup by canonical code
    private val codeIndex: Map<String, Uom> = Uom.entries.associateBy { it.code }

    // Aliases -> canonical code (lowercased, stripped)
    private val aliases: Map<String, String> = mapOf(
        // Length
        "um" to "um", "micrometro" to "um",
        "mm" to "mm", "cm" to "cm", "dm" to "dm", "m" to "m", "dam" to "dam", "hm" to "hm", "km" to "km",
        "mi" to "mi", "milla" to "mi", "millas" to "mi",
        "nmi" to "nmi", "millanautica" to "nmi",
        "in" to "in", "pulgada" to "in", "pul" to "in",
        "ft" to "ft", "pie" to "ft", "yd" to "yd", "yarda" to "yd",

        // Area
        "mm2" to "mm2", "cm2" to "cm2", "dm2" to "dm2", "m2" to "m2", "ha" to "ha", "km2" to "km2",
        "acre" to "acre", "mi2" to "mi2", "ping" to "ping", "in2" to "in2", "ft2" to "ft2", "yd2" to "yd2",

        // Volume metric
        "ul" to "ul", "ml" to "ml", "cl" to "cl", "dl" to "dl", "l" to "l", "dal" to "dal", "hl" to "hl", "kl" to "kl",
        "mm3" to "mm3", "cm3" to "cm3", "dm3" to "dm3", "m3" to "m3",

        // Volume US
        "floz" to "fl_oz", "flou" to "fl_oz", "onza" to "fl_oz",
        "gill" to "gill", "pint" to "pint", "pt" to "pint", "quart" to "quart", "qt" to "quart", "gal" to "gal",

        // Volume UK
        "flozuk" to "fl_oz_uk", "flouzuk" to "fl_oz_uk",
        "gilluk" to "gill_uk", "pintuk" to "pint_uk", "quartuk" to "quart_uk", "galuk" to "gal_uk",

        // Volume solids
        "in3" to "in3", "ft3" to "ft3", "yd3" to "yd3",

        // Mass
        "ug" to "ug", "mg" to "mg", "cg" to "cg", "dg" to "dg", "g" to "g",
        "dag" to "dag", "hg" to "hg", "kg" to "kg", "t" to "t",
        "ton" to "ton", "ltn" to "LTN", "tn" to "TN",
        "gr" to "gr", "dr" to "dr", "oz" to "oz", "lb" to "lb", "st" to "st", "qr" to "qr", "cwt" to "cwt",
        "ozt" to "ozt", "lbt" to "lbt", "dwt" to "dwt", "ct" to "ct",

        // Counting
        "und" to "und", "unidad" to "und",
        "persona" to "Persona", "personas" to "Personas",
        "cabeza" to "Cabeza", "grupo" to "Grupo", "bobina" to "Bobina",
        "decena" to "Decena", "docena" to "Docena", "ciento" to "Ciento", "millar" to "Millar",
        "viaje" to "Viaje", "visita" to "Visita", "paquete" to "Paquete", "kit" to "Kit",
        "barrica" to "Barrica", "barril" to "Barril", "blanco" to "Blanco",
        "bolso" to "Bolso", "caja" to "Caja", "cesta" to "Cesta",

        // Heat
        "btu" to "BTU",

        // Informatics (subset)
        "bit" to "bit", "bit/s" to "bit_s", "bitsseg" to "bit_s",
        "kbit" to "kbit", "kbit/s" to "kbit_s",
        "mbit" to "Mbit", "mbit/s" to "Mbit_s",
        "kbyte" to "kbyte", "mbyte" to "Mbyte", "gbyte" to "Gbyte",
        "kibyte" to "Kibyte", "dpi" to "dpi",

        // Communications
        "lineasacceso" to "LineasAcceso",
        "lineasservicio" to "LineasServicio", "líneasservicio" to "LineasServicio",
        "llamada" to "Llamada", "mml" to "MML", "puerto" to "Puerto",

        // Power
        "w" to "W", "kw" to "kW", "mw" to "MW", "gw" to "GW", "tw" to "TW",
        "wh" to "Wh", "kwh" to "kWh", "mwh" to "MWh", "gwh" to "GWh", "twh" to "TWh",
        "va" to "VA", "kva" to "kVA", "mva" to "MVA",

        // Textile / Time / Work
        "denier" to "Denier", "lbb" to "lbb", "madeja" to "Madeja",
        "anocomun" to "AnoComun", "mes" to "Mes", "dia" to "Dia",
        "hora" to "Hora", "min" to "Min", "seg" to "Seg",
        "semana" to "Semana", "actividad" to "Actividad",
        "horatrabajo" to "HoraTrabajo", "horaextra" to "HoraExtra",
        "mestrabajo" to "MesTrabajo", "puesto" to "Puesto"
    )

    fun all(): List<Uom> = Uom.entries

    fun byCode(code: String): Uom? = codeIndex[code]

    /**
     * Normalize user/provider input into a canonical UOM code.
     * Returns null if it doesn’t match any known code.
     */
    fun normalize(input: String?): Uom? {
        if (input == null) return null
        var s = input.trim().lowercase()
        s = s
            .replace("µ", "u")
            .replace("²", "2")
            .replace("³", "3")
            .replace("_", "")
            .replace(".", "")
            .replace(" ", "")

        // alias first
        aliases[s]?.let { return codeIndex[it] }

        // maybe already canonical (case-sensitive codes exist, so try both)
        codeIndex[s]?.let { return it }
        codeIndex[input]?.let { return it } // raw

        return null
    }
}