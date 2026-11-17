package com.noor.core.location

import com.noor.core.prayer.Coordinates

data class City(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val countryEn: String,
    val countryAr: String,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String,
) {
    val coordinates: Coordinates get() = Coordinates(latitude, longitude)
    fun name(arabic: Boolean) = if (arabic) nameAr else nameEn
    fun country(arabic: Boolean) = if (arabic) countryAr else countryEn

    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return nameEn.lowercase().contains(q) || nameAr.contains(q) ||
            countryEn.lowercase().contains(q) || countryAr.contains(q)
    }
}

/** Built-in list of cities for manual selection (coordinates of city centres). */
object Cities {
    val all: List<City> = listOf(
        City("najaf", "Najaf", "النجف الأشرف", "Iraq", "العراق", 31.9960, 44.3148, "Asia/Baghdad"),
        City("karbala", "Karbala", "كربلاء المقدسة", "Iraq", "العراق", 32.6160, 44.0249, "Asia/Baghdad"),
        City("baghdad", "Baghdad (Kadhimiya)", "بغداد (الكاظمية)", "Iraq", "العراق", 33.3800, 44.3400, "Asia/Baghdad"),
        City("samarra", "Samarra", "سامراء", "Iraq", "العراق", 34.1983, 43.8741, "Asia/Baghdad"),
        City("basra", "Basra", "البصرة", "Iraq", "العراق", 30.5085, 47.7804, "Asia/Baghdad"),
        City("qom", "Qom", "قم المقدسة", "Iran", "إيران", 34.6399, 50.8759, "Asia/Tehran"),
        City("mashhad", "Mashhad", "مشهد المقدسة", "Iran", "إيران", 36.2970, 59.6062, "Asia/Tehran"),
        City("tehran", "Tehran", "طهران", "Iran", "إيران", 35.6892, 51.3890, "Asia/Tehran"),
        City("isfahan", "Isfahan", "أصفهان", "Iran", "إيران", 32.6546, 51.6680, "Asia/Tehran"),
        City("shiraz", "Shiraz", "شيراز", "Iran", "إيران", 29.5918, 52.5837, "Asia/Tehran"),
        City("beirut", "Beirut", "بيروت", "Lebanon", "لبنان", 33.8938, 35.5018, "Asia/Beirut"),
        City("tyre", "Tyre", "صور", "Lebanon", "لبنان", 33.2704, 35.2038, "Asia/Beirut"),
        City("damascus", "Damascus", "دمشق", "Syria", "سوريا", 33.5138, 36.2765, "Asia/Damascus"),
        City("kuwait", "Kuwait City", "مدينة الكويت", "Kuwait", "الكويت", 29.3759, 47.9774, "Asia/Kuwait"),
        City("manama", "Manama", "المنامة", "Bahrain", "البحرين", 26.2235, 50.5876, "Asia/Bahrain"),
        City("qatif", "Qatif", "القطيف", "Saudi Arabia", "السعودية", 26.5652, 49.9964, "Asia/Riyadh"),
        City("hofuf", "Al-Ahsa (Hofuf)", "الأحساء (الهفوف)", "Saudi Arabia", "السعودية", 25.3833, 49.5867, "Asia/Riyadh"),
        City("madinah", "Madinah", "المدينة المنورة", "Saudi Arabia", "السعودية", 24.4672, 39.6111, "Asia/Riyadh"),
        City("makkah", "Makkah", "مكة المكرمة", "Saudi Arabia", "السعودية", 21.4225, 39.8262, "Asia/Riyadh"),
        City("riyadh", "Riyadh", "الرياض", "Saudi Arabia", "السعودية", 24.7136, 46.6753, "Asia/Riyadh"),
        City("doha", "Doha", "الدوحة", "Qatar", "قطر", 25.2854, 51.5310, "Asia/Qatar"),
        City("dubai", "Dubai", "دبي", "United Arab Emirates", "الإمارات", 25.2048, 55.2708, "Asia/Dubai"),
        City("abudhabi", "Abu Dhabi", "أبوظبي", "United Arab Emirates", "الإمارات", 24.4539, 54.3773, "Asia/Dubai"),
        City("muscat", "Muscat", "مسقط", "Oman", "عُمان", 23.5880, 58.3829, "Asia/Muscat"),
        City("sanaa", "Sana'a", "صنعاء", "Yemen", "اليمن", 15.3694, 44.1910, "Asia/Aden"),
        City("baku", "Baku", "باكو", "Azerbaijan", "أذربيجان", 40.4093, 49.8671, "Asia/Baku"),
        City("istanbul", "Istanbul", "إسطنبول", "Türkiye", "تركيا", 41.0082, 28.9784, "Europe/Istanbul"),
        City("cairo", "Cairo", "القاهرة", "Egypt", "مصر", 30.0444, 31.2357, "Africa/Cairo"),
        City("karachi", "Karachi", "كراتشي", "Pakistan", "باكستان", 24.8607, 67.0011, "Asia/Karachi"),
        City("lahore", "Lahore", "لاهور", "Pakistan", "باكستان", 31.5204, 74.3587, "Asia/Karachi"),
        City("islamabad", "Islamabad", "إسلام آباد", "Pakistan", "باكستان", 33.6844, 73.0479, "Asia/Karachi"),
        City("kabul", "Kabul", "كابل", "Afghanistan", "أفغانستان", 34.5553, 69.2075, "Asia/Kabul"),
        City("lucknow", "Lucknow", "لكناو", "India", "الهند", 26.8467, 80.9462, "Asia/Kolkata"),
        City("hyderabad", "Hyderabad", "حيدر آباد", "India", "الهند", 17.3850, 78.4867, "Asia/Kolkata"),
        City("mumbai", "Mumbai", "مومباي", "India", "الهند", 19.0760, 72.8777, "Asia/Kolkata"),
        City("dhaka", "Dhaka", "دكا", "Bangladesh", "بنغلاديش", 23.8103, 90.4125, "Asia/Dhaka"),
        City("kualalumpur", "Kuala Lumpur", "كوالالمبور", "Malaysia", "ماليزيا", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
        City("jakarta", "Jakarta", "جاكرتا", "Indonesia", "إندونيسيا", -6.2088, 106.8456, "Asia/Jakarta"),
        City("lagos", "Lagos", "لاغوس", "Nigeria", "نيجيريا", 6.5244, 3.3792, "Africa/Lagos"),
        City("daressalaam", "Dar es Salaam", "دار السلام", "Tanzania", "تنزانيا", -6.7924, 39.2083, "Africa/Dar_es_Salaam"),
        City("london", "London", "لندن", "United Kingdom", "المملكة المتحدة", 51.5074, -0.1278, "Europe/London"),
        City("manchester", "Manchester", "مانشستر", "United Kingdom", "المملكة المتحدة", 53.4808, -2.2426, "Europe/London"),
        City("paris", "Paris", "باريس", "France", "فرنسا", 48.8566, 2.3522, "Europe/Paris"),
        City("berlin", "Berlin", "برلين", "Germany", "ألمانيا", 52.5200, 13.4050, "Europe/Berlin"),
        City("hamburg", "Hamburg", "هامبورغ", "Germany", "ألمانيا", 53.5511, 9.9937, "Europe/Berlin"),
        City("stockholm", "Stockholm", "ستوكهولم", "Sweden", "السويد", 59.3293, 18.0686, "Europe/Stockholm"),
        City("amsterdam", "Amsterdam", "أمستردام", "Netherlands", "هولندا", 52.3676, 4.9041, "Europe/Amsterdam"),
        City("newyork", "New York", "نيويورك", "United States", "الولايات المتحدة", 40.7128, -74.0060, "America/New_York"),
        City("dearborn", "Dearborn", "ديربورن", "United States", "الولايات المتحدة", 42.3223, -83.1763, "America/Detroit"),
        City("houston", "Houston", "هيوستن", "United States", "الولايات المتحدة", 29.7604, -95.3698, "America/Chicago"),
        City("losangeles", "Los Angeles", "لوس أنجلوس", "United States", "الولايات المتحدة", 34.0522, -118.2437, "America/Los_Angeles"),
        City("toronto", "Toronto", "تورونتو", "Canada", "كندا", 43.6532, -79.3832, "America/Toronto"),
        City("sydney", "Sydney", "سيدني", "Australia", "أستراليا", -33.8688, 151.2093, "Australia/Sydney"),
        City("melbourne", "Melbourne", "ملبورن", "Australia", "أستراليا", -37.8136, 144.9631, "Australia/Melbourne"),
    )

    fun byId(id: String?): City? = all.firstOrNull { it.id == id }
}
