package com.example.util

/**
 * Handles localization exclusively for the default built-in categories
 * ("Health", "Focus", "Mind", "Habit", "Other").
 * Custom user-created categories remain in their original custom name.
 */
object CategoryLocalization {
    private val MAIN_CATEGORIES_LOWER = setOf(
        "health", "focus", "mind", "habit", "other", "productivity",
        "الصحة", "التركيز", "الذهن", "العادة", "أخرى", "الإنتاجية"
    )

    fun isMainCategory(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        return name.trim().lowercase() in MAIN_CATEGORIES_LOWER
    }

    fun getCanonicalCategoryKey(name: String?): String {
        if (name.isNullOrBlank()) return "Other"
        return when (name.trim().lowercase()) {
            "health", "الصحة" -> "Health"
            "focus", "التركيز" -> "Focus"
            "mind", "الذهن" -> "Mind"
            "habit", "العادة" -> "Habit"
            "productivity", "الإنتاجية" -> "Focus"
            "other", "أخرى" -> "Other"
            "all", "الكل" -> "All"
            else -> name.trim()
        }
    }

    fun getLocalizedCategoryName(name: String?, isArabic: Boolean): String {
        if (name.isNullOrBlank()) return if (isArabic) "أخرى" else "Other"
        val clean = name.trim().lowercase()
        if (isArabic) {
            return when (clean) {
                "health", "الصحة" -> "الصحة"
                "focus", "التركيز" -> "التركيز"
                "mind", "الذهن" -> "الذهن"
                "habit", "العادة" -> "العادة"
                "other", "أخرى" -> "أخرى"
                "productivity", "الإنتاجية" -> "الإنتاجية"
                "all", "الكل" -> "الكل"
                else -> name.trim()
            }
        } else {
            return when (clean) {
                "health", "الصحة" -> "Health"
                "focus", "التركيز" -> "Focus"
                "mind", "الذهن" -> "Mind"
                "habit", "العادة" -> "Habit"
                "other", "أخرى" -> "Other"
                "productivity", "الإنتاجية" -> "Productivity"
                "all", "الكل" -> "All"
                else -> name.trim()
            }
        }
    }

    fun getMainCategoryColorHex(name: String?): String {
        return when (getCanonicalCategoryKey(name).lowercase()) {
            "health" -> "#10B981"
            "focus" -> "#6366F1"
            "mind" -> "#14B8A6"
            "habit" -> "#EC4899"
            else -> "#F97316"
        }
    }
}
