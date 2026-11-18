package com.example.bus_popayan.data.remote

import android.util.Log
import com.example.bus_popayan.Config
import com.example.bus_popayan.domain.model.Bus
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

/**
 * Conexión en tiempo real con el backend (Socket.IO sobre WebSocket).
 *
 * Eventos:
 *  - Envía  "ruta:seguir"          -> empieza a recibir posiciones de una ruta
 *  - Envía  "conductor:ubicacion"  -> transmite la ubicación de este celular
 *  - Envía  "conductor:detener"    -> deja de transmitir
 *  - Recibe "bus:ubicacion"        -> nueva posición de quien transmite
 *  - Recibe "bus:fuera"            -> quien transmitía se desconectó
 */
class BusSocketService {

    private var socket: Socket? = null

    private val _busUpdates = MutableSharedFlow<Bus>(
        replay = 1,
        extraBufferCapacity = 10
    )
    val busUpdates: Flow<Bus> = _busUpdates

    private val _busFuera = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val busFuera: Flow<String> = _busFuera

    private val _conectado = MutableStateFlow(false)
    val conectado: StateFlow<Boolean> = _conectado

    private var rutaActual: String? = null

    // Conectar al servidor Socket.IO
    fun connect(url: String = Config.SERVER_URL) {

        disconnect()

        try {
            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
            }

            socket = IO.socket(url, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Conectado al servidor")
                _conectado.value = true

                rutaActual?.let { ruta ->
                    socket?.emit("ruta:seguir", ruta)
                }
            }

            socket?.on("bus:ubicacion") { args ->
                try {
                    val datos = args.firstOrNull() as? JSONObject ?: return@on

                    // Si la posición la envió este mismo celular (modo "Compartir"),
                    // no se muestra como el bus que se está siguiendo.
                    if (datos.optString("emisor") == socket?.id()) return@on

                    val rutaId = datos.getString("ruta_id")
                    val nombre = datos.optString("nombre", "Compañero")

                    val bus = Bus(
                        id = rutaId,
                        plate = nombre,
                        routeId = rutaId,
                        latitude = datos.getDouble("latitud"),
                        longitude = datos.getDouble("longitud"),
                        name = nombre
                    )

                    _busUpdates.tryEmit(bus)
                    Log.d(TAG, "GPS: $rutaId, ${bus.latitude}, ${bus.longitude}")

                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando ubicación", e)
                }
            }

            socket?.on("bus:fuera") { args ->
                val datos = args.firstOrNull() as? JSONObject
                _busFuera.tryEmit(datos?.optString("ruta_id").orEmpty())
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Desconectado")
                _conectado.value = false
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Error de conexión: ${args.firstOrNull()}")
                _conectado.value = false
            }

            socket?.connect()

        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando Socket.IO", e)
        }
    }

    // Suscribirse a una ruta
    fun seguirRuta(rutaId: String) {
        val anterior = rutaActual
        rutaActual = rutaId

        if (socket?.connected() == true) {
            if (anterior != null && anterior != rutaId) {
                socket?.emit("ruta:dejar", anterior)
            }
            socket?.emit("ruta:seguir", rutaId)
        }
    }

    // Transmitir la ubicación de este celular (modo "compartir")
    fun enviarUbicacion(
        rutaId: String,
        latitud: Double,
        longitud: Double,
        nombre: String,
        precision: Float? = null
    ) {
        val datos = JSONObject().apply {
            put("ruta_id", rutaId)
            put("latitud", latitud)
            put("longitud", longitud)
            put("nombre", nombre)
            if (precision != null) put("precision", precision.toDouble())
        }
        socket?.emit("conductor:ubicacion", datos)
    }

    fun detenerTransmision() {
        socket?.emit("conductor:detener")
    }

    // Cerrar conexión
    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        _conectado.value = false
    }

    private companion object {
        const val TAG = "BusSocket"
    }
}
