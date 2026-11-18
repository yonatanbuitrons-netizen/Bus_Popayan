package com.example.bus_popayan.ui.map

import com.example.bus_popayan.Config

/**
 * Paradero que el usuario eligió en "Rutas" (ej. Universidad Autónoma del Cauca).
 * El mapa lo usa como "mi ubicación" (punto verde) y sigue esa ruta en vivo.
 */
object DestinoSeleccionado {

    var rutaId: String = Config.RUTA_DEMO
        private set

    var nombre: String? = null
        private set

    var latitud: Double? = null
        private set

    var longitud: Double? = null
        private set

    // Cambia cada vez que se elige un destino nuevo
    var version: Int = 0
        private set

    fun elegir(rutaId: String, nombre: String, latitud: Double, longitud: Double) {
        this.rutaId = rutaId
        this.nombre = nombre
        this.latitud = latitud
        this.longitud = longitud
        version++
    }
}
