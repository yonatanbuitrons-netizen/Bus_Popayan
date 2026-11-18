package com.example.bus_popayan.data.repository

import com.example.bus_popayan.data.model.Eta
import com.example.bus_popayan.data.model.LoginRequest
import com.example.bus_popayan.data.model.Paradero
import com.example.bus_popayan.data.model.RegistroRequest
import com.example.bus_popayan.data.model.Ruta
import com.example.bus_popayan.data.model.Usuario
import com.example.bus_popayan.data.remote.ApiClient
import com.example.bus_popayan.data.remote.ApiService
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

// Oculta de dónde vienen los datos (API REST) al resto de la app.
class BusRepository(private val api: ApiService = ApiClient.service) {

    suspend fun obtenerEta(paraderoId: String): Result<List<Eta>> =
        safeCall { api.getEta(paraderoId) }

    suspend fun obtenerRutas(estado: String? = null): Result<List<Ruta>> =
        safeCall { api.getRutas(estado) }

    suspend fun obtenerParaderos(rutaId: String? = null): Result<List<Paradero>> =
        safeCall { api.getParaderos(rutaId) }

    suspend fun registrarUsuario(usuario: Usuario): Result<Usuario> =
        safeCall { api.registrarUsuario(usuario) }

    suspend fun crearCuenta(nombre: String, email: String, password: String): Result<Usuario> =
        safeCall { api.registro(RegistroRequest(nombre.trim(), email.trim(), password)) }

    suspend fun iniciarSesion(email: String, password: String): Result<Usuario> =
        safeCall { api.login(LoginRequest(email.trim(), password)) }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val mensaje = try {
                JSONObject(e.response()?.errorBody()?.string() ?: "").getString("error")
            } catch (_: Exception) {
                "Error del servidor (${e.code()})"
            }
            Result.failure(Exception(mensaje))
        } catch (e: IOException) {
            Result.failure(Exception("No se pudo conectar al servidor. ¿Está corriendo npm start?"))
        } catch (e: Exception) {
            Result.failure(e)
        }
}