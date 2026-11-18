package com.example.bus_popayan.domain.model

data class Stop(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val routes: List<String> // IDs of routes passing through this stop
)
