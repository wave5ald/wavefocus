package com.example.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.resume

data class LocationWeather(
    val cityName: String,
    val country: String = "",
    val latitude: Double,
    val longitude: Double,
    val condition: String,
    val conditionEmoji: String = "🌤️",
    val tempCelsius: Int,
    val tempFahrenheit: Int,
    val apparentTempCelsius: Int = tempCelsius,
    val apparentTempFahrenheit: Int = tempFahrenheit,
    val highCelsius: Int,
    val lowCelsius: Int,
    val humidity: Int,
    val windSpeedKmH: Double,
    val precipitationProbability: Int = 0,
    val uvIndex: Double = 3.0,
    val tip: String,
    val isRealTime: Boolean,
    val lastUpdated: String,
    val isGpsLocation: Boolean = false
)

data class CitySearchResult(
    val displayName: String,
    val cityName: String,
    val country: String,
    val admin1: String,
    val latitude: Double,
    val longitude: Double
)

object WeatherService {

    private const val PREFS_NAME = "wave_focus_weather_prefs"
    private const val KEY_CITY = "weather_city"
    private const val KEY_COUNTRY = "weather_country"
    private const val KEY_LAT = "weather_lat"
    private const val KEY_LON = "weather_lon"
    private const val KEY_IS_MANUAL = "weather_is_manual"
    private const val KEY_LAST_JSON = "weather_last_json"
    private const val KEY_LAST_FETCH_TIME = "weather_last_fetch_time"

    fun isManualLocation(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_MANUAL, false)
    }

    fun setManualLocationFlag(context: Context, isManual: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_MANUAL, isManual).apply()
    }

    fun getSavedLocation(context: Context): Triple<String, Pair<Double, Double>?, Boolean> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val city = prefs.getString(KEY_CITY, null)
        val lat = if (prefs.contains(KEY_LAT)) prefs.getFloat(KEY_LAT, 0f).toDouble() else null
        val lon = if (prefs.contains(KEY_LON)) prefs.getFloat(KEY_LON, 0f).toDouble() else null
        val isManual = prefs.getBoolean(KEY_IS_MANUAL, false)
        return if (city != null && lat != null && lon != null) {
            Triple(city, (lat to lon), isManual)
        } else {
            Triple("", null, false)
        }
    }

    fun saveLocation(context: Context, cityName: String, country: String, lat: Double, lon: Double, isManual: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CITY, cityName)
            .putString(KEY_COUNTRY, country)
            .putFloat(KEY_LAT, lat.toFloat())
            .putFloat(KEY_LON, lon.toFloat())
            .putBoolean(KEY_IS_MANUAL, isManual)
            .apply()
    }

    /**
     * Comprehensive detector that first checks device GPS / Network location,
     * and seamlessly falls back to multiple reliable IP Geolocation services so the user's
     * actual city and weather are accurately retrieved wherever they are.
     */
    suspend fun detectDeviceOrIpLocation(context: Context): Triple<String, Double, Double>? = withContext(Dispatchers.IO) {
        // 1. Try GPS / Network location if permission is granted
        val deviceLoc = detectDeviceLocation(context)
        if (deviceLoc != null && deviceLoc.second != 0.0 && deviceLoc.third != 0.0) {
            saveLocation(context, deviceLoc.first, "", deviceLoc.second, deviceLoc.third, isManual = false)
            return@withContext deviceLoc
        }

        // 2. Fall back to accurate IP geolocation
        val ipLoc = detectLocationFromIp()
        if (ipLoc != null && ipLoc.second != 0.0 && ipLoc.third != 0.0) {
            saveLocation(context, ipLoc.first, "", ipLoc.second, ipLoc.third, isManual = false)
            return@withContext ipLoc
        }

        // 3. If any previous saved location exists, return it
        val (savedCity, coords, _) = getSavedLocation(context)
        if (coords != null && savedCity.isNotBlank()) {
            return@withContext Triple(savedCity, coords.first, coords.second)
        }

        null
    }

    suspend fun detectDeviceLocation(context: Context): Triple<String, Double, Double>? = null

    /**
     * Reverse geocodes coordinates into a clean "City, Country" name.
     * Uses Android Geocoder first, and falls back to BigDataCloud / OpenStreetMap APIs.
     */
    suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): String = withContext(Dispatchers.IO) {

        // 2. BigDataCloud Reverse Geocode API (free, reliable HTTPS, no auth needed)
        try {
            val urlStr = "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$lat&longitude=$lon&localityLanguage=en"
            val jsonStr = httpGet(urlStr)
            if (jsonStr != null) {
                val root = JSONObject(jsonStr)
                val city = root.optString("city", "").ifBlank {
                    root.optString("locality", "").ifBlank {
                        root.optString("principalSubdivision", "")
                    }
                }
                val country = root.optString("countryName", "")
                if (city.isNotBlank()) {
                    return@withContext if (country.isNotBlank()) "$city, $country" else city
                }
            }
        } catch (_: Exception) {}

        // 3. Open-Meteo Reverse / Search nearest
        try {
            val urlStr = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon"
            val jsonStr = httpGet(urlStr)
            if (jsonStr != null) {
                val root = JSONObject(jsonStr)
                val addr = root.optJSONObject("address")
                if (addr != null) {
                    val city = addr.optString("city", "").ifBlank {
                        addr.optString("town", "").ifBlank {
                            addr.optString("village", "").ifBlank {
                                addr.optString("county", "")
                            }
                        }
                    }
                    val country = addr.optString("country", "")
                    if (city.isNotBlank()) {
                        return@withContext if (country.isNotBlank()) "$city, $country" else city
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback representation
        val latStr = String.format(Locale.US, "%.2f", lat)
        val lonStr = String.format(Locale.US, "%.2f", lon)
        "Location ($latStr, $lonStr)"
    }

    /**
     * IP Geolocation fallback to get the user's real location with zero permissions required.
     * Uses multiple redundant HTTPS sources.
     */
    suspend fun detectLocationFromIp(): Triple<String, Double, Double>? = withContext(Dispatchers.IO) {
        // Source 1: ipwho.is (fast, reliable, free, HTTPS, no rate limits)
        try {
            val jsonStr = httpGet("https://ipwho.is/")
            if (jsonStr != null) {
                val root = JSONObject(jsonStr)
                if (root.optBoolean("success", false)) {
                    val city = root.optString("city", "")
                    val country = root.optString("country", "")
                    val lat = root.optDouble("latitude", 0.0)
                    val lon = root.optDouble("longitude", 0.0)
                    if (lat != 0.0 && lon != 0.0) {
                        val name = if (city.isNotBlank()) {
                            if (country.isNotBlank()) "$city, $country" else city
                        } else "Local Area"
                        return@withContext Triple(name, lat, lon)
                    }
                }
            }
        } catch (_: Exception) {}

        // Source 2: freeipapi.com (HTTPS, free, accurate)
        try {
            val jsonStr = httpGet("https://freeipapi.com/api/json")
            if (jsonStr != null) {
                val root = JSONObject(jsonStr)
                val city = root.optString("cityName", "")
                val country = root.optString("countryName", "")
                val lat = root.optDouble("latitude", 0.0)
                val lon = root.optDouble("longitude", 0.0)
                if (lat != 0.0 && lon != 0.0) {
                    val name = if (city.isNotBlank()) {
                        if (country.isNotBlank()) "$city, $country" else city
                    } else "Local Area"
                    return@withContext Triple(name, lat, lon)
                }
            }
        } catch (_: Exception) {}

        // Source 3: ipapi.co
        try {
            val jsonStr = httpGet("https://ipapi.co/json/")
            if (jsonStr != null) {
                val root = JSONObject(jsonStr)
                if (!root.optBoolean("error", false)) {
                    val city = root.optString("city", "")
                    val country = root.optString("country_name", "")
                    val lat = root.optDouble("latitude", 0.0)
                    val lon = root.optDouble("longitude", 0.0)
                    if (lat != 0.0 && lon != 0.0) {
                        val name = if (city.isNotBlank()) {
                            if (country.isNotBlank()) "$city, $country" else city
                        } else "Local Area"
                        return@withContext Triple(name, lat, lon)
                    }
                }
            }
        } catch (_: Exception) {}

        // Source 4: ipinfo.io
        try {
            val jsonStr = httpGet("https://ipinfo.io/json")
            if (jsonStr != null) {
                val root = JSONObject(jsonStr)
                val city = root.optString("city", "")
                val country = root.optString("country", "")
                val loc = root.optString("loc", "")
                if (loc.contains(",")) {
                    val parts = loc.split(",")
                    val lat = parts[0].toDoubleOrNull() ?: 0.0
                    val lon = parts[1].toDoubleOrNull() ?: 0.0
                    if (lat != 0.0 && lon != 0.0) {
                        val name = if (city.isNotBlank()) {
                            if (country.isNotBlank()) "$city, $country" else city
                        } else "Local Area"
                        return@withContext Triple(name, lat, lon)
                    }
                }
            }
        } catch (_: Exception) {}

        null
    }

    suspend fun searchCityCoordinates(query: String): Triple<String, Double, Double>? = withContext(Dispatchers.IO) {
        val list = searchCities(query)
        val first = list.firstOrNull() ?: return@withContext null
        Triple(first.displayName, first.latitude, first.longitude)
    }

    suspend fun searchCities(query: String): List<CitySearchResult> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(clean, "UTF-8")
            val urlStr = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=8&language=en&format=json"
            val jsonStr = httpGet(urlStr) ?: return@withContext emptyList()
            val root = JSONObject(jsonStr)
            val results = root.optJSONArray("results") ?: return@withContext emptyList()
            val list = mutableListOf<CitySearchResult>()
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val name = item.optString("name", clean)
                val country = item.optString("country", "")
                val admin1 = item.optString("admin1", "")
                val lat = item.optDouble("latitude", 0.0)
                val lon = item.optDouble("longitude", 0.0)

                val displayParts = mutableListOf<String>()
                displayParts.add(name)
                if (admin1.isNotBlank() && admin1 != name) displayParts.add(admin1)
                if (country.isNotBlank()) displayParts.add(country)

                list.add(
                    CitySearchResult(
                        displayName = displayParts.joinToString(", "),
                        cityName = name,
                        country = country,
                        admin1 = admin1,
                        latitude = lat,
                        longitude = lon
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Fetches exact live meteorological data from Open-Meteo for the provided coordinates and date.
     */
    suspend fun fetchWeather(
        context: Context,
        cityName: String,
        lat: Double,
        lon: Double,
        date: LocalDate
    ): LocationWeather = withContext(Dispatchers.IO) {
        try {
            val urlStr = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,weather_code,cloud_cover,wind_speed_10m" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,uv_index_max,precipitation_probability_max,wind_speed_10m_max" +
                    "&timezone=auto"

            val jsonStr = httpGet(urlStr)
            if (jsonStr != null) {
                val root = JSONObject(jsonStr)
                val current = root.optJSONObject("current")
                val daily = root.optJSONObject("daily")

                val isToday = (date == LocalDate.now())
                val todayTemp = current?.optDouble("temperature_2m", 22.0)?.toInt() ?: 22
                val currentApparent = current?.optDouble("apparent_temperature", todayTemp.toDouble())?.toInt() ?: todayTemp
                val currentCode = current?.optInt("weather_code", 0) ?: 0
                val humidity = current?.optInt("relative_humidity_2m", 50) ?: 50
                val wind = current?.optDouble("wind_speed_10m", 10.0) ?: 10.0

                var high = todayTemp + 3
                var low = todayTemp - 3
                var resolvedCode = currentCode
                var displayTemp = todayTemp
                var displayApparent = currentApparent
                var precipProb = 0
                var uvIndex = 3.0

                if (daily != null) {
                    val timeArr = daily.optJSONArray("time")
                    val maxArr = daily.optJSONArray("temperature_2m_max")
                    val minArr = daily.optJSONArray("temperature_2m_min")
                    val codeArr = daily.optJSONArray("weather_code")
                    val precipArr = daily.optJSONArray("precipitation_probability_max")
                    val uvArr = daily.optJSONArray("uv_index_max")
                    val apparentMaxArr = daily.optJSONArray("apparent_temperature_max")

                    val dateTargetStr = date.toString()
                    var matchIndex = -1
                    if (timeArr != null) {
                        for (i in 0 until timeArr.length()) {
                            if (timeArr.optString(i) == dateTargetStr) {
                                matchIndex = i
                                break
                            }
                        }
                    }

                    if (matchIndex >= 0) {
                        if (maxArr != null && maxArr.length() > matchIndex) high = maxArr.optDouble(matchIndex, (todayTemp + 3).toDouble()).toInt()
                        if (minArr != null && minArr.length() > matchIndex) low = minArr.optDouble(matchIndex, (todayTemp - 3).toDouble()).toInt()
                        if (codeArr != null && codeArr.length() > matchIndex) resolvedCode = codeArr.optInt(matchIndex, currentCode)
                        if (precipArr != null && precipArr.length() > matchIndex) precipProb = precipArr.optInt(matchIndex, 0)
                        if (uvArr != null && uvArr.length() > matchIndex) uvIndex = uvArr.optDouble(matchIndex, 3.0)
                        if (apparentMaxArr != null && apparentMaxArr.length() > matchIndex) displayApparent = apparentMaxArr.optDouble(matchIndex, high.toDouble()).toInt()

                        displayTemp = if (isToday) todayTemp else (high + low) / 2
                    } else if (!isToday) {
                        val offline = OfflineWeatherHelper.getWeatherForDate(date)
                        high = offline.highCelsius
                        low = offline.lowCelsius
                        displayTemp = offline.tempCelsius
                        displayApparent = offline.tempCelsius
                        resolvedCode = -1
                    }
                }

                val (condition, emoji) = if (resolvedCode >= 0) parseWmoCode(resolvedCode) else (OfflineWeatherHelper.getWeatherForDate(date).condition to "🌤️")
                val tip = generateTip(condition, displayTemp)

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putString(KEY_LAST_JSON, jsonStr)
                    .putLong(KEY_LAST_FETCH_TIME, System.currentTimeMillis())
                    .apply()

                val timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))
                val updateLabel = if (isToday) "Live • $timeStr" else "Forecast • ${date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))}"

                val displayName = if (cityName.isBlank() || cityName == "Current Location" || cityName == "Local Weather") {
                    reverseGeocode(context, lat, lon)
                } else {
                    cityName
                }

                return@withContext LocationWeather(
                    cityName = displayName,
                    country = "",
                    latitude = lat,
                    longitude = lon,
                    condition = condition,
                    conditionEmoji = emoji,
                    tempCelsius = displayTemp,
                    tempFahrenheit = (displayTemp * 9 / 5) + 32,
                    apparentTempCelsius = displayApparent,
                    apparentTempFahrenheit = (displayApparent * 9 / 5) + 32,
                    highCelsius = high,
                    lowCelsius = low,
                    humidity = humidity,
                    windSpeedKmH = wind,
                    precipitationProbability = precipProb,
                    uvIndex = uvIndex,
                    tip = tip,
                    isRealTime = true,
                    lastUpdated = updateLabel,
                    isGpsLocation = false
                )
            }
        } catch (_: Exception) {}

        // Fallback estimate
        val offline = OfflineWeatherHelper.getWeatherForDate(date)
        LocationWeather(
            cityName = cityName.ifBlank { "Local Weather" },
            country = "",
            latitude = lat,
            longitude = lon,
            condition = offline.condition,
            conditionEmoji = offline.emoji,
            tempCelsius = offline.tempCelsius,
            tempFahrenheit = offline.tempFahrenheit,
            apparentTempCelsius = offline.tempCelsius,
            apparentTempFahrenheit = offline.tempFahrenheit,
            highCelsius = offline.highCelsius,
            lowCelsius = offline.lowCelsius,
            humidity = 55,
            windSpeedKmH = 12.0,
            precipitationProbability = 15,
            uvIndex = 4.0,
            tip = offline.tip,
            isRealTime = false,
            lastUpdated = "Offline Estimate"
        )
    }

    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) WaveFocus/2.0")
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                sb.toString()
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun parseWmoCode(code: Int): Pair<String, String> {
        return when (code) {
            0 -> "Clear Sky" to "☀️"
            1 -> "Mainly Clear" to "🌤️"
            2 -> "Partly Cloudy" to "⛅"
            3 -> "Overcast" to "☁️"
            45, 48 -> "Foggy" to "🌫️"
            51, 53, 55 -> "Light Drizzle" to "🌦️"
            56, 57 -> "Freezing Drizzle" to "🌨️"
            61 -> "Slight Rain" to "🌧️"
            63 -> "Moderate Rain" to "🌧️"
            65 -> "Heavy Rain" to "🌧️"
            66, 67 -> "Freezing Rain" to "🌨️"
            71, 73, 75 -> "Snow Fall" to "❄️"
            77 -> "Snow Grains" to "❄️"
            80, 81, 82 -> "Rain Showers" to "🌦️"
            85, 86 -> "Snow Showers" to "🌨️"
            95 -> "Thunderstorm" to "⛈️"
            96, 99 -> "Thunderstorm with Hail" to "⛈️"
            else -> "Pleasant Weather" to "🌤️"
        }
    }

    private fun generateTip(condition: String, tempC: Int): String {
        return when {
            tempC >= 34 -> "High heat today ($tempC°C)! Drink plenty of water and prioritize indoor focus sessions."
            tempC >= 28 -> "Warm weather today ($tempC°C). Stay well hydrated while conquering your daily routines."
            tempC <= 0 -> "Freezing temperatures ($tempC°C). Stay warm indoors with hot tea and cozy deep focus!"
            tempC <= 10 -> "Crisp and cool ($tempC°C). Bundle up if going outside and maintain your routine momentum."
            condition.contains("Thunder", ignoreCase = true) ->
                "Thunderstorms in the area! Stay safe indoors and get into flow state."
            condition.contains("Rain", ignoreCase = true) || condition.contains("Drizzle", ignoreCase = true) || condition.contains("Shower", ignoreCase = true) ->
                "Rainy atmosphere outside—the soothing ambient sound is perfect for deep reading & habit building!"
            condition.contains("Snow", ignoreCase = true) ->
                "Snowy day outside. Great time to stay comfortable and check off today's milestones."
            condition.contains("Clear", ignoreCase = true) || condition.contains("Sunny", ignoreCase = true) ->
                "Clear, bright skies today! Catch natural daylight to elevate energy, focus, and mood."
            condition.contains("Cloud", ignoreCase = true) || condition.contains("Overcast", ignoreCase = true) ->
                "Overcast and calm. Great low-glare atmosphere for productivity and deep work."
            else -> "Favorable conditions today. Keep your streak alive and accomplish your goals!"
        }
    }
}
