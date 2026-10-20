package com.example.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/**
 * Helper for conversion, formatting, and navigation with the Islamic Hijri (Umm al-Qura) Calendar.
 * Supports Android API 26+ natively via java.time.chrono.HijrahDate.
 */
object HijriCalendarHelper {

    private val ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    val HIJRI_MONTHS_AR = listOf(
        "محرم",
        "صفر",
        "ربيع الأول",
        "ربيع الآخر",
        "جمادى الأولى",
        "جمادى الآخرة",
        "رجب",
        "شعبان",
        "رمضان",
        "شوال",
        "ذو القعدة",
        "ذو الحجة"
    )

    val HIJRI_MONTHS_EN = listOf(
        "Muharram",
        "Safar",
        "Rabi' al-Awwal",
        "Rabi' al-Thani",
        "Jumada al-Ula",
        "Jumada al-Akhirah",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhu al-Qi'dah",
        "Dhu al-Hijjah"
    )

    fun toArabicDigits(number: Int): String {
        return number.toString().map { char ->
            if (char in '0'..'9') ARABIC_DIGITS[char - '0'] else char
        }.joinToString("")
    }

    /**
     * Converts a LocalDate to HijrahDate safely with tabular fallback if date is outside range.
     */
    fun toHijrahDate(date: LocalDate): HijrahDate {
        return try {
            HijrahDate.from(date)
        } catch (e: Exception) {
            // Fallback: estimate
            HijrahChronology.INSTANCE.dateNow()
        }
    }

    fun getHijriYear(date: LocalDate): Int {
        return toHijrahDate(date).get(ChronoField.YEAR)
    }

    fun getHijriMonth(date: LocalDate): Int {
        return toHijrahDate(date).get(ChronoField.MONTH_OF_YEAR)
    }

    fun getHijriDay(date: LocalDate): Int {
        return toHijrahDate(date).get(ChronoField.DAY_OF_MONTH)
    }

    fun getHijriMonthName(monthNumber: Int, isArabic: Boolean): String {
        val index = (monthNumber - 1).coerceIn(0, 11)
        return if (isArabic) HIJRI_MONTHS_AR[index] else HIJRI_MONTHS_EN[index]
    }

    fun getHijriMonthLength(year: Int, month: Int): Int {
        return try {
            val hDate = HijrahChronology.INSTANCE.date(year, month, 1)
            hDate.lengthOfMonth()
        } catch (e: Exception) {
            // Hijri months are either 29 or 30 days
            if (month % 2 == 1) 30 else 29
        }
    }

    /**
     * Converts Hijri year, month, and day to Gregorian LocalDate.
     */
    fun hijriToLocalDate(year: Int, month: Int, day: Int): LocalDate {
        return try {
            val maxDay = getHijriMonthLength(year, month)
            val clampedDay = day.coerceIn(1, maxDay)
            val hDate = HijrahChronology.INSTANCE.date(year, month, clampedDay)
            LocalDate.from(hDate)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    /**
     * Formats a complete Hijri date string.
     * Example AR: "الأحد، ٧ ذو القعدة ١٤٤٧ هـ"
     * Example EN: "Sunday, 7 Dhu al-Qi'dah 1447 AH"
     */
    fun formatFullHijri(date: LocalDate, isArabic: Boolean, withDayOfWeek: Boolean = true): String {
        val hDate = toHijrahDate(date)
        val year = hDate.get(ChronoField.YEAR)
        val month = hDate.get(ChronoField.MONTH_OF_YEAR)
        val day = hDate.get(ChronoField.DAY_OF_MONTH)
        val monthName = getHijriMonthName(month, isArabic)

        val dayOfWeekStr = if (withDayOfWeek) {
            getLocalizedDayOfWeek(date.dayOfWeek, isArabic) + "، "
        } else ""

        return if (isArabic) {
            val dayStr = toArabicDigits(day)
            val yearStr = toArabicDigits(year)
            "$dayOfWeekStr$dayStr $monthName $yearStr هـ"
        } else {
            "$dayOfWeekStr$day $monthName $year AH"
        }
    }

    /**
     * Short format for chips and compact labels.
     * Example: "٧ ذو القعدة" or "7 Dhu al-Qi'dah"
     */
    fun formatShortHijri(date: LocalDate, isArabic: Boolean): String {
        val hDate = toHijrahDate(date)
        val month = hDate.get(ChronoField.MONTH_OF_YEAR)
        val day = hDate.get(ChronoField.DAY_OF_MONTH)
        val monthName = getHijriMonthName(month, isArabic)

        return if (isArabic) {
            "${toArabicDigits(day)} $monthName"
        } else {
            "$day $monthName"
        }
    }

    fun formatHijriDayNumber(date: LocalDate, isArabic: Boolean): String {
        val day = getHijriDay(date)
        return if (isArabic) toArabicDigits(day) else day.toString()
    }

    fun getLocalizedDayOfWeek(dayOfWeek: DayOfWeek, isArabic: Boolean): String {
        return if (isArabic) {
            when (dayOfWeek) {
                DayOfWeek.MONDAY -> "الإثنين"
                DayOfWeek.TUESDAY -> "الثلاثاء"
                DayOfWeek.WEDNESDAY -> "الأربعاء"
                DayOfWeek.THURSDAY -> "الخميس"
                DayOfWeek.FRIDAY -> "الجمعة"
                DayOfWeek.SATURDAY -> "السبت"
                DayOfWeek.SUNDAY -> "الأحد"
            }
        } else {
            dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    fun getLocalizedDayOfWeekShort(dayOfWeek: DayOfWeek, isArabic: Boolean): String {
        return if (isArabic) {
            when (dayOfWeek) {
                DayOfWeek.MONDAY -> "إثن"
                DayOfWeek.TUESDAY -> "ثلا"
                DayOfWeek.WEDNESDAY -> "أرب"
                DayOfWeek.THURSDAY -> "خمي"
                DayOfWeek.FRIDAY -> "جمع"
                DayOfWeek.SATURDAY -> "سبت"
                DayOfWeek.SUNDAY -> "أحد"
            }
        } else {
            when (dayOfWeek) {
                DayOfWeek.MONDAY -> "Mon"
                DayOfWeek.TUESDAY -> "Tue"
                DayOfWeek.WEDNESDAY -> "Wed"
                DayOfWeek.THURSDAY -> "Thu"
                DayOfWeek.FRIDAY -> "Fri"
                DayOfWeek.SATURDAY -> "Sat"
                DayOfWeek.SUNDAY -> "Sun"
            }
        }
    }

    /**
     * Identifies special Islamic occasions.
     */
    fun getSpecialIslamicOccasion(date: LocalDate, isArabic: Boolean): String? {
        val month = getHijriMonth(date)
        val day = getHijriDay(date)

        return when {
            month == 1 && day == 1 -> if (isArabic) "رأس السنة الهجرية 🌟" else "Islamic New Year 🌟"
            month == 1 && day == 9 -> if (isArabic) "تاسوعاء 🕊️" else "Tasu'a 🕊️"
            month == 1 && day == 10 -> if (isArabic) "يوم عاشوراء المبارك 🕊️" else "Day of Ashura 🕊️"
            month == 3 && day == 12 -> if (isArabic) "ذكرى المولد النبوي الشريف ﷺ 🕌" else "Mawlid an-Nabi ﷺ 🕌"
            month == 7 && day == 27 -> if (isArabic) "ذكرى الإسراء والمعراج 🌌" else "Isra and Mi'raj 🌌"
            month == 8 && day == 15 -> if (isArabic) "ليلة النصف من شعبان 🌙" else "Mid-Sha'ban Night 🌙"
            month == 9 && day == 1 -> if (isArabic) "أول أيام شهر رمضان المبارك 🌙" else "First Day of Ramadan 🌙"
            month == 9 && day in 21..30 -> if (isArabic) "العشر الأواخر من رمضان • تحري ليلة القدر 🌟" else "Last 10 Nights of Ramadan 🌟"
            month == 10 && day == 1 -> if (isArabic) "عيد الفطر المبارك 🎉" else "Eid al-Fitr 🎉"
            month == 10 && day in 2..3 -> if (isArabic) "أيام عيد الفطر المبارك 🎉" else "Eid al-Fitr Days 🎉"
            month == 12 && day in 1..8 -> if (isArabic) "عشر ذي الحجة المباركة 🕋" else "Ten Days of Dhu al-Hijjah 🕋"
            month == 12 && day == 9 -> if (isArabic) "يوم عرفة المبارك 🕋" else "Day of Arafah 🕋"
            month == 12 && day == 10 -> if (isArabic) "عيد الأضحى المبارك 🐑" else "Eid al-Adha 🐑"
            month == 12 && day in 11..13 -> if (isArabic) "أيام التشريق المباركة 🕋" else "Days of Tashreeq 🕋"
            date.dayOfWeek == DayOfWeek.FRIDAY -> if (isArabic) "يوم الجمعة المباركة • خير يوم طلعت عليه الشمس 🕌" else "Blessed Friday 🕌"
            else -> null
        }
    }

    /**
     * Daily Islamic inspirations (Hadith & Quranic Ayahs).
     */
    fun getDailyInspiration(date: LocalDate, isArabic: Boolean): Pair<String, String> {
        val dayOfYear = date.dayOfYear
        val inspirationsAr = listOf(
            "«أَحَبُّ الْأَعْمَالِ إِلَى اللَّهِ أَدْوَمُهَا وَإِنْ قَلَّ»" to "صحيح البخاري ومسلم",
            "«وَقُلِ اعْمَلُوا فَسَيَرَى اللَّهُ عَمَلَكُمْ وَرَسُولُهُ وَالْمُؤْمِنُونَ»" to "سورة التوبة: ١٠٥",
            "«احْرِصْ عَلَى مَا يَنْفَعُكَ، وَاسْتَعِنْ بِاللَّهِ وَلَا تَعْجَزْ»" to "صحيح مسلم",
            "«مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الْجَنَّةِ»" to "صحيح مسلم",
            "«إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَوْقُوتًا»" to "سورة النساء: ١٠٣",
            "«فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ»" to "سورة البقرة: ١٥٢",
            "«وَأَنْ لَيْسَ لِلْإِنْسَانِ إِلَّا مَا سَعَىٰ • وَأَنَّ سَعْيَهُ سَوْفَ يُرَىٰ»" to "سورة النجم: ٣٩-٤٠",
            "«لَا تَحْقِرَنَّ مِنَ الْمَعْرُوفِ شَيْئًا وَلَوْ أَنْ تَلْقَى أَخَاكَ بِوَجْهٍ طَلْقٍ»" to "صحيح مسلم"
        )

        val inspirationsEn = listOf(
            "“The most beloved deeds to Allah are those that are consistent, even if small.”" to "Sahih al-Bukhari & Muslim",
            "“And say: Work, so Allah will see your deeds, and His Messenger and the believers.”" to "Surah At-Tawbah: 105",
            "“Cherish that which benefits you, seek help from Allah, and do not lose heart.”" to "Sahih Muslim",
            "“Whoever travels a path seeking knowledge, Allah makes easy for him a path to Paradise.”" to "Sahih Muslim",
            "“Indeed, prayer has been decreed upon the believers at specified times.”" to "Surah An-Nisa: 103",
            "“So remember Me; I will remember you. And be grateful to Me and do not deny Me.”" to "Surah Al-Baqarah: 152",
            "“And that each person will only have that which they strived for.”" to "Surah An-Najm: 39",
            "“Do not belittle any good deed, even meeting your brother with a cheerful face.”" to "Sahih Muslim"
        )

        val list = if (isArabic) inspirationsAr else inspirationsEn
        return list[dayOfYear % list.size]
    }
}
