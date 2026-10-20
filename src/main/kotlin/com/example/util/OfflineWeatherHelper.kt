package com.example.util

import android.content.Context
import java.time.LocalDate
import java.time.Month
import kotlin.math.cos

data class OfflineWeather(
    val condition: String,
    val emoji: String,
    val tempCelsius: Int,
    val tempFahrenheit: Int,
    val highCelsius: Int,
    val lowCelsius: Int,
    val seasonName: String,
    val tip: String
)

object OfflineWeatherHelper {

    fun getWeatherForDate(date: LocalDate): OfflineWeather {
        val dayOfYear = date.dayOfYear
        val month = date.month

        // Base annual climatological solar curve
        // Peak mid-summer around day 200 (~26°C avg), low mid-winter around day 15 (~2°C avg)
        val normalizedAngle = (dayOfYear - 200) * 2.0 * Math.PI / 365.0
        val baseMeanTemp = 14.0 + 12.0 * cos(normalizedAngle) // Ranges from ~2°C in Jan to ~26°C in July

        // Deterministic day-to-day minor pseudo variation based on day
        val variance = ((date.toEpochDay() * 7 + 13) % 9 - 4).toInt() // -4 to +4
        val currentTempC = (baseMeanTemp + variance).toInt().coerceIn(-15, 42)
        val highC = currentTempC + 4
        val lowC = currentTempC - 4
        val tempF = (currentTempC * 9 / 5) + 32

        val (season, condition, emoji, tip) = when (month) {
            Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> {
                if (currentTempC <= 0) {
                    Quadruple("Winter", "Crisp Frost & Snow", "❄️", "Stay warm and hydrated while tackling your goals!")
                } else {
                    Quadruple("Winter", "Cool & Clear Winter Day", "🌤️", "Brisk fresh air—great for energized focus.")
                }
            }
            Month.MARCH, Month.APRIL, Month.MAY -> {
                if (dayOfYear % 3 == 0) {
                    Quadruple("Spring", "Fresh Spring Breeze", "🍃", "Invigorating weather for new habits and routines.")
                } else {
                    Quadruple("Spring", "Sunny & Blooming", "☀️", "Bright daylight—ideal for morning sunlight habits.")
                }
            }
            Month.JUNE, Month.JULY, Month.AUGUST -> {
                if (currentTempC > 27) {
                    Quadruple("Summer", "Warm & Sunny Skies", "☀️", "Keep cool and keep a water bottle close!")
                } else {
                    Quadruple("Summer", "Pleasant Summer Day", "🌤️", "Great natural light for focused deep work.")
                }
            }
            Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> {
                if (dayOfYear % 2 == 0) {
                    Quadruple("Autumn", "Mild Autumn Breeze", "🍂", "Clear, golden days—perfect for steady focus.")
                } else {
                    Quadruple("Autumn", "Partly Cloudy & Crisp", "⛅", "Cozy atmosphere to reflect and schedule your day.")
                }
            }
        }

        return OfflineWeather(
            condition = condition,
            emoji = emoji,
            tempCelsius = currentTempC,
            tempFahrenheit = tempF,
            highCelsius = highC,
            lowCelsius = lowC,
            seasonName = season,
            tip = tip
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
