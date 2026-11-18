package com.example.bus_popayan.data.remote

import android.util.Log
import com.example.bus_popayan.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Resultado de calcular la ruta entre quien transmite y el usuario. */
data class RutaCalculada(
    val puntos: List<GeoPoint>,   // camino por las calles
    val distanciaM: Double,
    val duracionS: Double,
    val calles: List<String>,     // calles principales por donde pasa
    val estimada: Boolean         // true = sin OSRM (línea recta + velocidad promedio)
)

/**
 * Calcula la ruta por calles y el tiempo de llegada usando OSRM,
 * el motor de rutas gratuito de OpenStreetMap (no requiere API key).
 * Si no hay internet o OSRM falla, estima con distancia en línea recta.
 */
object RoutingService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun calcular(desde: GeoPoint, hasta: GeoPoint): RutaCalculada =
        withContext(Dispatchers.IO) {
            try {
                consultarOsrm(desde, hasta)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("Routing", "OSRM no disponible, se usa estimación", e)
                estimar(desde, hasta)
            }
        }

    private fun consultarOsrm(desde: GeoPoint, hasta: GeoPoint): RutaCalculada {
        // Locale.US: siempre punto decimal (en español el celular usaría coma)
        val coords = String.format(
            Locale.US,
            "%.6f,%.6f;%.6f,%.6f",
            desde.longitude, desde.latitude,
            hasta.longitude, hasta.latitude
        )
        val url = Config.OSRM_URL + coords +
            "?overview=full&geometries=geojson&steps=true"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "BusPopayan/1.0 (proyecto academico)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("OSRM HTTP ${response.code}")

            val json = JSONObject(response.body?.string().orEmpty())
            if (json.optString("code") != "Ok") error("OSRM: ${json.optString("code")}")

            val ruta = json.getJSONArray("routes").getJSONObject(0)

            val coordenadas = ruta.getJSONObject("geometry").getJSONArray("coordinates")
            val puntos = ArrayList<GeoPoint>(coordenadas.length())
            for (i in 0 until coordenadas.length()) {
                val c = coordenadas.getJSONArray(i)
                puntos.add(GeoPoint(c.getDouble(1), c.getDouble(0)))
            }

            // Nombres de calles, en orden, sin repetir y sin vacíos
            val calles = LinkedHashSet<String>()
            val legs = ruta.optJSONArray("legs")
            if (legs != null) {
                for (l in 0 until legs.length()) {
                    val steps = legs.getJSONObject(l).optJSONArray("steps") ?: continue
                    for (s in 0 until steps.length()) {
                        val paso = steps.getJSONObject(s)
                        val nombre = paso.optString("name").trim()
                        if (nombre.isNotEmpty() && paso.optDouble("distance", 0.0) > 40) {
                            calles.add(nombre)
                        }
                    }
                }
            }

            return RutaCalculada(
                puntos = puntos,
                distanciaM = ruta.getDouble("distance"),
                duracionS = ruta.getDouble("duration"),
                calles = calles.take(4),
                estimada = false
            )
        }
    }

    private fun estimar(desde: GeoPoint, hasta: GeoPoint): RutaCalculada {
        val distancia = distanciaMetros(desde, hasta)
        val velocidadMs = Config.VELOCIDAD_RESPALDO_KMH * 1000 / 3600
        return RutaCalculada(
            puntos = listOf(desde, hasta),
            distanciaM = distancia,
            duracionS = distancia / velocidadMs,
            calles = emptyList(),
            estimada = true
        )
    }

    /** Distancia en línea recta (fórmula de Haversine). */
    fun distanciaMetros(a: GeoPoint, b: GeoPoint): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) *
            sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(h), sqrt(1 - h))
    }
}
