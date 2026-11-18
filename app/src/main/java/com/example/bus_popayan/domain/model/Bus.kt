package com.example.bus_popayan.domain.model

data class Bus(
    val id: String,
    val plate: String,
    val routeId: String,
    val latitude: Double,
    val longitude: Double,
    val lastUpdate: Long = System.currentTimeMillis(),
    // Nombre de quien transmite (compañero / conductor)
    val name: String = ""
)
