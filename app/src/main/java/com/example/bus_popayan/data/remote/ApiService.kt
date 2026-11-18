package com.example.bus_popayan.data.remote

import com.example.bus_popayan.data.model.Eta
import com.example.bus_popayan.data.model.LoginRequest
import com.example.bus_popayan.data.model.RegistroRequest
import com.example.bus_popayan.data.model.Paradero
import com.example.bus_popayan.data.model.Ruta
import com.example.bus_popayan.data.model.Usuario
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Contrato con la API REST (Node + Express + MongoDB).
interface ApiService {

    @GET("rutas")
    suspend fun getRutas(@Query("estado") estado: String? = null): List<Ruta>

    @GET("paraderos")
    suspend fun getParaderos(@Query("ruta_id") rutaId: String? = null): List<Paradero>

    @GET("eta/{paradero_id}")
    suspend fun getEta(@Path("paradero_id") paraderoId: String): List<Eta>

    @POST("usuarios")
    suspend fun registrarUsuario(@Body usuario: Usuario): Usuario

    // Autenticación: la contraseña se valida y se guarda cifrada en el servidor
    @POST("auth/registro")
    suspend fun registro(@Body datos: RegistroRequest): Usuario

    @POST("auth/login")
    suspend fun login(@Body datos: LoginRequest): Usuario
}