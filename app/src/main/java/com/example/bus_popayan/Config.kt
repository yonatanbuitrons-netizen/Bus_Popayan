package com.example.bus_popayan

/**
 * Configuración central de la app.
 *
 * SERVER_URL es la dirección del backend (Node + Socket.IO). Cámbiala aquí y
 * se usa en toda la app (REST y tiempo real):
 *
 *  - Emulador de Android Studio:        "http://10.0.2.2:3000"
 *  - Celular físico en la misma WiFi:   "http://192.168.X.X:3000"  (IP del PC; la imprime npm start)
 *  - Celular en otra red (túnel https): "https://xxxx.trycloudflare.com"
 */
object Config {

    const val SERVER_URL = "http://10.0.2.2:3000"

    // Ruta que la app sigue en el mapa y en la que se comparte la ubicación.
    const val RUTA_DEMO = "R001"

    // Servicio gratuito de rutas por calles (OpenStreetMap). No necesita API key.
    const val OSRM_URL = "https://router.project-osrm.org/route/v1/driving/"

    // Proveedor de los cuadros del mapa (ambos gratis y sin API key):
    //  "OSM"  = OpenStreetMap estándar
    //  "ESRI" = Esri World Street Map (úsalo si OSM muestra cuadros de error)
    const val PROVEEDOR_MAPA = "OSM"

    // Velocidad para estimar el tiempo si OSRM no responde (bus urbano).
    const val VELOCIDAD_RESPALDO_KMH = 25.0
}