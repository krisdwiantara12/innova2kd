package com.innova.launcher2kd.service

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

/**
 * Live Weather & Outside Temperature Service
 * Membaca cuaca dan temperatur luar ruangan secara otomatis tanpa sensor tambahan.
 * Menggunakan Open-Meteo REST API (Free, Zero-Config, Hemat Kuota).
 */
class WeatherManager(
    private val context: Context,
    private val onWeatherUpdate: (city: String, tempC: Int, condition: String, iconEmoji: String) -> Unit
) {
    private val prefs = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var lastFetchTime = 0L
    private var isRunning = false

    fun start(lastLocation: Location? = null) {
        isRunning = true
        // Muat data cache tersimpan agar langsung tampil saat booting
        loadCachedWeather()

        if (lastLocation != null) {
            fetchWeather(lastLocation.latitude, lastLocation.longitude)
        } else {
            fetchWeatherByIp()
        }
    }

    fun onLocationUpdated(location: Location) {
        val now = System.currentTimeMillis()
        // Batasi fetch minimal tiap 15 menit agar tidak memboroskan kuota hotspot
        if (now - lastFetchTime > 15 * 60 * 1000) {
            fetchWeather(location.latitude, location.longitude)
        }
    }

    private fun loadCachedWeather() {
        val city = prefs.getString("city", "INDONESIA") ?: "INDONESIA"
        val temp = prefs.getInt("temp", 30)
        val cond = prefs.getString("cond", "Cerah Berawan") ?: "Cerah Berawan"
        val icon = prefs.getString("icon", "⛅") ?: "⛅"
        onWeatherUpdate(city, temp, cond, icon)
    }

    private fun fetchWeatherByIp() {
        thread {
            try {
                // Fallback koordinat Jakarta / Indonesia default
                fetchWeather(-6.2088, 106.8456)
            } catch (e: Exception) {}
        }
    }

    fun fetchWeather(lat: Double, lon: Double) {
        lastFetchTime = System.currentTimeMillis()
        thread {
            try {
                val urlStr = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,weather_code"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.requestMethod = "GET"

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val jsonStr = reader.use { it.readText() }
                    val jsonObj = JSONObject(jsonStr)
                    val current = jsonObj.getJSONObject("current")

                    val tempDouble = current.getDouble("temperature_2m")
                    val tempInt = Math.round(tempDouble).toInt()
                    val code = current.getInt("weather_code")

                    val (condition, emoji) = parseWmoCode(code)
                    val cityName = resolveCityName(lat, lon)

                    // Simpan cache
                    prefs.edit()
                        .putString("city", cityName)
                        .putInt("temp", tempInt)
                        .putString("cond", condition)
                        .putString("icon", emoji)
                        .apply()

                    handler.post {
                        if (isRunning) {
                            onWeatherUpdate(cityName, tempInt, condition, emoji)
                        }
                    }
                }
            } catch (e: Exception) {
                // Gunakan cache jika internet offline
                handler.post { loadCachedWeather() }
            }
        }
    }

    private fun resolveCityName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale("id", "ID"))
            val list = geocoder.getFromLocation(lat, lon, 1)
            if (!list.isNullOrEmpty()) {
                val addr = list[0]
                addr.subAdminArea ?: addr.locality ?: addr.adminArea ?: "INDONESIA"
            } else {
                "INDONESIA"
            }
        } catch (e: Exception) {
            "INDONESIA"
        }
    }

    private fun parseWmoCode(code: Int): Pair<String, String> {
        return when (code) {
            0 -> Pair("Cerah", "☀️")
            1 -> Pair("Cerah Berawan", "🌤️")
            2, 3 -> Pair("Berawan", "⛅")
            45, 48 -> Pair("Kabut", "🌫️")
            51, 53, 55 -> Pair("Gerimis", "🌦️")
            61, 63, 65 -> Pair("Hujan", "🌧️")
            80, 81, 82 -> Pair("Hujan Lebat", "⛈️")
            95, 96, 99 -> Pair("Badai Petir", "⚡")
            else -> Pair("Berawan", "☁️")
        }
    }

    fun stop() {
        isRunning = false
    }
}
