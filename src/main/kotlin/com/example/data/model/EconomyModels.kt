package com.example.data.model

data class CategoryItem(
    val name: String,
    val colorHex: String,
    val isCustom: Boolean = false
)

enum class FocusPrize(
    val id: String,
    val title: String,
    val colorHex: String,
    val badgeTag: String,
    val minMinutes: Int,
    val sellPrice: Int,
    val description: String
) {
    COPPER(
        id = "copper",
        title = "Bronze Ore",
        colorHex = "#CD7F32",
        badgeTag = "BRONZE",
        minMinutes = 10,
        sellPrice = 20,
        description = "Mined from 10+ min focus sessions"
    ),
    IRON(
        id = "iron",
        title = "Iron Ingot",
        colorHex = "#94A3B8",
        badgeTag = "IRON",
        minMinutes = 30,
        sellPrice = 50,
        description = "Mined from 30+ min focus sessions"
    ),
    GOLD(
        id = "gold",
        title = "Gold Bar",
        colorHex = "#F59E0B",
        badgeTag = "GOLD",
        minMinutes = 60,
        sellPrice = 120,
        description = "Mined from 1+ hour focus sessions"
    ),
    DIAMOND(
        id = "diamond",
        title = "Diamond",
        colorHex = "#0284C7",
        badgeTag = "DIAMOND",
        minMinutes = 120,
        sellPrice = 300,
        description = "Mined from 2+ hour focus sessions"
    ),
    EMERALD(
        id = "emerald",
        title = "Emerald",
        colorHex = "#10B981",
        badgeTag = "EMERALD",
        minMinutes = 300,
        sellPrice = 1000,
        description = "Mined from 5+ hour focus sessions"
    );

    val icon: String get() = badgeTag

    companion object {
        fun forDurationMinutes(minutes: Int): FocusPrize? {
            return when {
                minutes >= EMERALD.minMinutes -> EMERALD
                minutes >= DIAMOND.minMinutes -> DIAMOND
                minutes >= GOLD.minMinutes -> GOLD
                minutes >= IRON.minMinutes -> IRON
                minutes >= COPPER.minMinutes -> COPPER
                else -> null
            }
        }

        fun nextTier(minutes: Int): FocusPrize? {
            return when {
                minutes < COPPER.minMinutes -> COPPER
                minutes < IRON.minMinutes -> IRON
                minutes < GOLD.minMinutes -> GOLD
                minutes < DIAMOND.minMinutes -> DIAMOND
                minutes < EMERALD.minMinutes -> EMERALD
                else -> null
            }
        }
    }
}

data class PrizeInventory(
    val copper: Int = 0,
    val iron: Int = 0,
    val gold: Int = 0,
    val diamond: Int = 0,
    val emerald: Int = 0
) {
    fun getCount(prize: FocusPrize): Int = when (prize) {
        FocusPrize.COPPER -> copper
        FocusPrize.IRON -> iron
        FocusPrize.GOLD -> gold
        FocusPrize.DIAMOND -> diamond
        FocusPrize.EMERALD -> emerald
    }

    fun totalItems(): Int = copper + iron + gold + diamond + emerald

    fun totalValue(): Int =
        copper * FocusPrize.COPPER.sellPrice +
        iron * FocusPrize.IRON.sellPrice +
        gold * FocusPrize.GOLD.sellPrice +
        diamond * FocusPrize.DIAMOND.sellPrice +
        emerald * FocusPrize.EMERALD.sellPrice
}
