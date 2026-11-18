package com.example.bus_popayan.domain.usecase

import com.example.bus_popayan.domain.model.Bus
import com.example.bus_popayan.domain.model.Stop
import kotlin.math.*

class CalculateETAUsecase {

    /**
     * Calcula el tiempo estimado de llegada en minutos.
     * Asume una velocidad promedio de bus en Popayán de 25 km/h.
     */
    fun execute(bus: Bus, stop: Stop): Int {
        val distanceKm = calculateDistance(bus.latitude, bus.longitude, stop.latitude, stop.longitude)
        val averageSpeedKmH = 25.0
        val timeHours = distanceKm / averageSpeedKmH
        return (timeHours * 60).toInt()
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Radio de la Tierra en km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
