package com.example.bus_popayan.data.model

import android.content.Context
import com.google.gson.annotations.SerializedName
import org.json.JSONArray
import org.json.JSONObject

// Modelos de datos de Bus Popayán

data class Ruta(
    @SerializedName("ruta_id")
    val rutaId: String,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("numero")
    val numero: String?,

    @SerializedName("origen")
    val origen: String?,

    @SerializedName("destino")
    val destino: String?,

    @SerializedName("estado")
    val estado: String?
)

data class Paradero(
    @SerializedName("paradero_id")
    val paraderoId: String,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("direccion")
    val direccion: String?,

    @SerializedName("ruta_id")
    val rutaId: String?,

    @SerializedName("estado")
    val estado: String?,

    @SerializedName("latitud")
    val latitud: Double? = null,

    @SerializedName("longitud")
    val longitud: Double? = null
)

data class Eta(
    @SerializedName("eta_id")
    val etaId: String,

    @SerializedName("bus_id")
    val busId: String,

    @SerializedName("ruta_id")
    val rutaId: String,

    @SerializedName("paradero_id")
    val paraderoId: String,

    @SerializedName("minutos")
    val minutos: Int
)

data class Usuario(
    @SerializedName("usuario_id")
    val usuarioId: String? = null,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("tipo")
    val tipo: String
)

// ---------- Autenticación ----------

data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)

data class RegistroRequest(
    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)

// ======================================
// FAVORITAS, HISTORIAL Y SESIÓN (se guardan en el teléfono)
// ======================================

data class RutaFavorita(
    val rutaId: String,
    val rutaNombre: String,
    val paraderoNombre: String,
    val latitud: Double,
    val longitud: Double
)

data class Viaje(
    val fecha: Long,
    val rutaId: String,
    val rutaNombre: String,
    val paraderoNombre: String,
    val latitud: Double,
    val longitud: Double
)

object PreferenciasUsuario {

    private const val ARCHIVO = "bus_popayan_usuario"
    private const val CLAVE_FAVORITAS = "rutas_favoritas"
    private const val CLAVE_HISTORIAL = "historial_viajes"
    private const val MAX_HISTORIAL = 30
    private const val CLAVE_SESION = "sesion_usuario"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    // ---------- Rutas favoritas ----------

    fun favoritas(ctx: Context): List<RutaFavorita> {
        val arreglo = leer(ctx, CLAVE_FAVORITAS)
        return (0 until arreglo.length()).mapNotNull { i ->
            val o = arreglo.optJSONObject(i) ?: return@mapNotNull null
            RutaFavorita(
                rutaId = o.optString("rutaId"),
                rutaNombre = o.optString("rutaNombre"),
                paraderoNombre = o.optString("paraderoNombre"),
                latitud = o.optDouble("latitud"),
                longitud = o.optDouble("longitud")
            )
        }
    }

    fun esFavorita(ctx: Context, rutaId: String): Boolean =
        favoritas(ctx).any { it.rutaId == rutaId }

    fun alternarFavorita(ctx: Context, favorita: RutaFavorita): Boolean {
        val lista = favoritas(ctx).toMutableList()
        val existente = lista.indexOfFirst { it.rutaId == favorita.rutaId }
        val quedoFavorita = if (existente >= 0) {
            lista.removeAt(existente)
            false
        } else {
            lista.add(favorita)
            true
        }
        val arreglo = JSONArray()
        lista.forEach { f ->
            arreglo.put(JSONObject().apply {
                put("rutaId", f.rutaId)
                put("rutaNombre", f.rutaNombre)
                put("paraderoNombre", f.paraderoNombre)
                put("latitud", f.latitud)
                put("longitud", f.longitud)
            })
        }
        escribir(ctx, CLAVE_FAVORITAS, arreglo)
        return quedoFavorita
    }

    // ---------- Historial de viajes ----------

    fun historial(ctx: Context): List<Viaje> {
        val arreglo = leer(ctx, CLAVE_HISTORIAL)
        return (0 until arreglo.length()).mapNotNull { i ->
            val o = arreglo.optJSONObject(i) ?: return@mapNotNull null
            Viaje(
                fecha = o.optLong("fecha"),
                rutaId = o.optString("rutaId"),
                rutaNombre = o.optString("rutaNombre"),
                paraderoNombre = o.optString("paraderoNombre"),
                latitud = o.optDouble("latitud"),
                longitud = o.optDouble("longitud")
            )
        }
    }

    fun registrarViaje(ctx: Context, viaje: Viaje) {
        val lista = listOf(viaje) + historial(ctx)
        val arreglo = JSONArray()
        lista.take(MAX_HISTORIAL).forEach { v ->
            arreglo.put(JSONObject().apply {
                put("fecha", v.fecha)
                put("rutaId", v.rutaId)
                put("rutaNombre", v.rutaNombre)
                put("paraderoNombre", v.paraderoNombre)
                put("latitud", v.latitud)
                put("longitud", v.longitud)
            })
        }
        escribir(ctx, CLAVE_HISTORIAL, arreglo)
    }

    fun borrarHistorial(ctx: Context) {
        prefs(ctx).edit().remove(CLAVE_HISTORIAL).apply()
    }

    // ---------- Sesión del usuario ----------

    fun guardarSesion(ctx: Context, usuario: Usuario) {
        val o = JSONObject().apply {
            put("usuarioId", usuario.usuarioId ?: "")
            put("nombre", usuario.nombre)
            put("email", usuario.email)
            put("tipo", usuario.tipo)
        }
        prefs(ctx).edit().putString(CLAVE_SESION, o.toString()).apply()
    }

    fun sesion(ctx: Context): Usuario? {
        val texto = prefs(ctx).getString(CLAVE_SESION, null) ?: return null
        return try {
            val o = JSONObject(texto)
            Usuario(
                usuarioId = o.optString("usuarioId"),
                nombre = o.optString("nombre"),
                email = o.optString("email"),
                tipo = o.optString("tipo", "pasajero")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun cerrarSesion(ctx: Context) {
        prefs(ctx).edit().remove(CLAVE_SESION).apply()
    }

    // ---------- Utilidades ----------

    private fun leer(ctx: Context, clave: String): JSONArray =
        try {
            JSONArray(prefs(ctx).getString(clave, "[]") ?: "[]")
        } catch (e: Exception) {
            JSONArray()
        }

    private fun escribir(ctx: Context, clave: String, arreglo: JSONArray) {
        prefs(ctx).edit().putString(clave, arreglo.toString()).apply()
    }
}