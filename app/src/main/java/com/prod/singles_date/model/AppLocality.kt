package com.prod.singles_date.model

/** City localities. Stored as lowercase ids in Firestore. */
object AppLocality {
    // Bangalore
    const val WHITEFIELD = "whitefield"
    const val HSR = "hsr"
    const val KORAMANGALA = "koramangala"
    const val ELECTRONIC_CITY = "electronic_city"
    const val INDIRANAGAR = "indiranagar"

    val BANGALORE_ALL = listOf(WHITEFIELD, HSR, KORAMANGALA, ELECTRONIC_CITY, INDIRANAGAR)

    // Pune
    const val BANER = "baner"
    const val HINJEWADI = "hinjewadi"
    const val KOTHRUD = "kothrud"
    const val VIMAN_NAGAR = "viman_nagar"

    val PUNE_ALL = listOf(BANER, HINJEWADI, KOTHRUD, VIMAN_NAGAR)

    // Hyderabad
    const val HITEC_CITY = "hitec_city"
    const val GACHIBOWLI = "gachibowli"
    const val KUKATPALLY = "kukatpally"
    const val MADHAPUR = "madhapur"

    val HYDERABAD_ALL = listOf(HITEC_CITY, GACHIBOWLI, KUKATPALLY, MADHAPUR)

    // Chennai
    const val OMR = "omr"
    const val ANNA_NAGAR = "anna_nagar"
    const val T_NAGAR = "t_nagar"

    val CHENNAI_ALL = listOf(OMR, ANNA_NAGAR, T_NAGAR)

    // Mumbai
    const val ANDHERI = "andheri"
    const val BKC = "bkc"
    const val POWAI = "powai"

    val MUMBAI_ALL = listOf(ANDHERI, BKC, POWAI)

    // Delhi
    const val GURGAON = "gurgaon"
    const val NOIDA = "noida"
    const val CP = "cp"

    val DELHI_ALL = listOf(GURGAON, NOIDA, CP)

    val ALL_IDS = BANGALORE_ALL + PUNE_ALL + HYDERABAD_ALL + CHENNAI_ALL + MUMBAI_ALL + DELHI_ALL

    fun displayName(localityId: String): String = when (localityId) {
        WHITEFIELD -> "Whitefield"
        HSR -> "HSR"
        KORAMANGALA -> "Koramangala"
        ELECTRONIC_CITY -> "Electronic City"
        INDIRANAGAR -> "Indiranagar"
        BANER -> "Baner"
        HINJEWADI -> "Hinjewadi"
        KOTHRUD -> "Kothrud"
        VIMAN_NAGAR -> "Viman Nagar"
        HITEC_CITY -> "Hitec City"
        GACHIBOWLI -> "Gachibowli"
        KUKATPALLY -> "Kukatpally"
        MADHAPUR -> "Madhapur"
        OMR -> "OMR"
        ANNA_NAGAR -> "Anna Nagar"
        T_NAGAR -> "T Nagar"
        ANDHERI -> "Andheri"
        BKC -> "BKC"
        POWAI -> "Powai"
        GURGAON -> "Gurgaon"
        NOIDA -> "Noida"
        CP -> "Connaught Place"
        else -> localityId.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    fun isValidForCity(cityId: String, localityId: String): Boolean {
        if (localityId.isBlank()) return true
        return localityId in localitiesForCity(cityId)
    }

    fun localitiesForCity(cityId: String): List<String> = when (cityId) {
        AppCity.BANGALORE -> BANGALORE_ALL
        AppCity.PUNE -> PUNE_ALL
        AppCity.HYDERABAD -> HYDERABAD_ALL
        AppCity.CHENNAI -> CHENNAI_ALL
        AppCity.MUMBAI -> MUMBAI_ALL
        AppCity.DELHI -> DELHI_ALL
        else -> emptyList()
    }
}
