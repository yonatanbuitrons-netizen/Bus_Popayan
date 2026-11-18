package com.example.bus_popayan.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.bus_popayan.Config
import com.example.bus_popayan.R
import com.example.bus_popayan.data.model.Paradero
import com.example.bus_popayan.data.remote.BusSocketService
import com.example.bus_popayan.data.remote.RoutingService
import com.example.bus_popayan.data.remote.RutaCalculada
import com.example.bus_popayan.data.repository.BusRepository
import com.example.bus_popayan.databinding.FragmentMapBinding
import com.example.bus_popayan.domain.model.Bus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.InfoWindow
import java.io.File
import java.util.Locale
import kotlin.math.ceil

/**
 * Mapa principal (OpenStreetMap con osmdroid, sin API key).
 *
 *  - Recibe por Socket.IO la ubicación en vivo de quien comparte (compañero / bus).
 *  - Dibuja su recorrido (línea azul) y la ruta por calles hasta mi ubicación (línea verde).
 *  - Muestra en cuánto tiempo llega, a qué distancia y por qué calles viene.
 *  - "Compartir mi ubicación" convierte este celular en emisor (para el compañero).
 */
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels()
    private val repository = BusRepository()
    private val busSocketService = BusSocketService()

    // ---- Paraderos ----
    private val marcadoresParaderos = mutableMapOf<String, Marker>()
    private val paraderosPorId = mutableMapOf<String, Paradero>()

    // ---- Seguimiento en vivo ----
    private var marcadorBus: Marker? = null
    private var lineaRecorrido: Polyline? = null
    private var lineaRuta: Polyline? = null
    private val puntosRecorrido = mutableListOf<GeoPoint>()

    private var ultimoBus: Bus? = null
    private var ultimaSenalMs = 0L
    private var busActivo = false
    private var primeraUbicacionBus = true

    // ---- Mi ubicación ----
    private var marcadorYo: Marker? = null
    private var miUbicacion: GeoPoint? = null
    private var ubicacionManual = false
    private var primeraMiUbicacion = true
    private var locationManager: LocationManager? = null

    // ---- Compartir ----
    private var compartiendo = false

    // ---- Ruta seguida y destino elegido en "Rutas" ----
    private var rutaSeguida = Config.RUTA_DEMO
    private var nombreDestino: String? = null
    private var versionDestinoAplicada = 0

    // ---- Ruta / ETA ----
    private var jobRuta: Job? = null
    private var ultimoCalculoMs = 0L
    private var ultimaRuta: RutaCalculada? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            alRecibirGps(location)
        }

        // Se implementan para compatibilidad con Android 7–10
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private val permisoUbicacion = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.any { it }) {
            iniciarGps()
        } else {
            mostrarToast(
                "Sin permiso de ubicación. Mantén presionado el mapa para fijar tu ubicación a mano."
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // osmdroid se configura ANTES de inflar el MapView
        val ctx = requireContext().applicationContext
        Configuration.getInstance().apply {
            load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            // Identificador propio: los servidores de mapas bloquean apps "com.example"
            userAgentValue = "BusPopayan/1.0 (proyecto universitario, Popayan)"
            osmdroidBasePath = File(ctx.cacheDir, "osmdroid")
            // Carpeta nueva para no reutilizar las imágenes de error guardadas antes
            osmdroidTileCache = File(osmdroidBasePath, "tiles_v2")
        }

        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarMapa()
        configurarBuscador()
        configurarFiltros()
        configurarBotones()
        observarDatos()
        observarTiempoReal()
        iniciarReloj()
        pintarTarjeta()

        // Si se eligió un paradero en "Rutas", se usa como mi ubicación y se sigue su ruta
        aplicarDestinoSeleccionado()

        pedirPermisoUbicacion()

        // Conectar Android con Socket.IO y seguir la ruta
        busSocketService.seguirRuta(rutaSeguida)
        busSocketService.connect()
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapView?.onResume()
    }

    override fun onPause() {
        _binding?.mapView?.onPause()
        super.onPause()
    }

    // ======================================
    // MAPA
    // ======================================

    private fun configurarMapa() {
        val map = binding.mapView

        // Proveedor del mapa: se elige en Config.PROVEEDOR_MAPA ("OSM" o "ESRI")
        map.setTileSource(if (Config.PROVEEDOR_MAPA == "ESRI") MAPA_ESRI else MAPA_OSM)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.controller.setZoom(15.0)
        map.controller.setCenter(POPAYAN)

        // Toque largo = fijar "mi ubicación" a mano (útil si el GPS no da señal en el salón)
        val eventos = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                InfoWindow.closeAllInfoWindowsOn(map)
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (p == null) return false
                ubicacionManual = true
                nombreDestino = null
                actualizarMiUbicacion(p, null)
                recalcularRuta(forzar = true)
                mostrarToast("Tu ubicación quedó fijada aquí. Toca el botón de ubicación para volver al GPS.")
                return true
            }
        })
        map.overlays.add(0, eventos)

        // Recorrido real de quien transmite (azul)
        val recorrido = Polyline(map).apply {
            outlinePaint.color = Color.parseColor("#991565C0")
            outlinePaint.strokeWidth = 10f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            setPoints(ArrayList(puntosRecorrido))
            infoWindow = null
        }

        // Ruta calculada desde él hasta mí (verde)
        val ruta = Polyline(map).apply {
            outlinePaint.color = Color.parseColor("#2E7D32")
            outlinePaint.strokeWidth = 12f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            ultimaRuta?.let { setPoints(it.puntos) }
            infoWindow = null
        }

        map.overlays.add(recorrido)
        map.overlays.add(ruta)
        lineaRecorrido = recorrido
        lineaRuta = ruta

        // Si la vista se recrea (volver desde otra pestaña), reponer mi marcador
        miUbicacion?.let { actualizarMiUbicacion(it, null, enviar = false) }

        mostrarParaderos(viewModel.paraderosFiltrados.value.orEmpty())
    }

    /** Mantiene los marcadores en vivo por encima de los paraderos. */
    private fun traerAlFrente() {
        val map = _binding?.mapView ?: return
        listOfNotNull(marcadorYo, marcadorBus).forEach {
            map.overlays.remove(it)
            map.overlays.add(it)
        }
    }

    // ======================================
    // DESTINO ELEGIDO EN "RUTAS"
    // ======================================

    private fun aplicarDestinoSeleccionado() {
        val destino = DestinoSeleccionado
        val lat = destino.latitud
        val lon = destino.longitud
        if (destino.version == versionDestinoAplicada || lat == null || lon == null) return
        versionDestinoAplicada = destino.version

        if (destino.rutaId != rutaSeguida) {
            rutaSeguida = destino.rutaId
            reiniciarSeguimiento()
        }

        ubicacionManual = true
        nombreDestino = destino.nombre

        val punto = GeoPoint(lat, lon)
        actualizarMiUbicacion(punto, null, enviar = false)
        binding.mapView.controller.animateTo(punto, 16.0, 800L)
        recalcularRuta(forzar = true)

        mostrarToast("Destino: ${destino.nombre} · siguiendo la ruta $rutaSeguida")
    }

    /** Borra el bus, su recorrido y la ruta calculada (al cambiar de ruta). */
    private fun reiniciarSeguimiento() {
        ultimoBus = null
        busActivo = false
        ultimaRuta = null
        puntosRecorrido.clear()
        lineaRecorrido?.setPoints(ArrayList())
        lineaRuta?.setPoints(ArrayList())
        marcadorBus?.let { _binding?.mapView?.overlays?.remove(it) }
        marcadorBus = null
        primeraUbicacionBus = true
    }

    // ======================================
    // BOTONES
    // ======================================

    private fun configurarBotones() {
        binding.btnMiUbicacion.setOnClickListener {
            if (ubicacionManual) {
                ubicacionManual = false
                nombreDestino = null
                mostrarToast("Usando el GPS otra vez")
                iniciarGps()
            }
            val destino = miUbicacion
            if (destino != null) {
                binding.mapView.controller.animateTo(destino, 17.0, 800L)
            } else {
                pedirPermisoUbicacion()
                mostrarToast("Buscando tu ubicación… (o mantén presionado el mapa)")
            }
        }

        binding.btnCompartir.setOnClickListener {
            if (compartiendo) {
                detenerCompartir()
            } else {
                iniciarCompartir()
            }
        }
    }

    private fun iniciarCompartir() {
        if (!tienePermiso() && !ubicacionManual) {
            pedirPermisoUbicacion()
            mostrarToast("Da permiso de ubicación para compartir")
            return
        }

        compartiendo = true
        binding.btnCompartir.text = "Dejar de compartir"
        mostrarToast("Compartiendo tu ubicación en la ruta $rutaSeguida")

        miUbicacion?.let {
            busSocketService.enviarUbicacion(
                rutaSeguida, it.latitude, it.longitude, nombreDispositivo()
            )
        }
    }

    private fun detenerCompartir() {
        compartiendo = false
        busSocketService.detenerTransmision()
        _binding?.btnCompartir?.text = "Compartir mi ubicación"
    }

    private fun nombreDispositivo(): String =
        Build.MODEL?.takeIf { it.isNotBlank() } ?: "Celular Android"

    // ======================================
    // MI UBICACIÓN (GPS)
    // ======================================

    private fun tienePermiso(): Boolean {
        val ctx = context ?: return false
        return ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun pedirPermisoUbicacion() {
        if (tienePermiso()) {
            iniciarGps()
        } else {
            permisoUbicacion.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun iniciarGps() {
        if (!tienePermiso() || _binding == null) return

        detenerGps()

        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager = lm

        val proveedores = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { it in lm.allProviders }

        // Última ubicación conocida para no esperar al primer fix
        proveedores
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { alRecibirGps(it) }

        proveedores.forEach { proveedor ->
            try {
                lm.requestLocationUpdates(
                    proveedor, 2000L, 3f, locationListener, Looper.getMainLooper()
                )
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo usar $proveedor", e)
            }
        }

        val algunoActivo = proveedores.any {
            runCatching { lm.isProviderEnabled(it) }.getOrDefault(false)
        }
        if (!algunoActivo) {
            mostrarToast("Activa la ubicación (GPS) del celular")
        }
    }

    private fun detenerGps() {
        locationManager?.removeUpdates(locationListener)
        locationManager = null
    }

    private fun alRecibirGps(location: Location) {
        if (ubicacionManual) return
        actualizarMiUbicacion(
            GeoPoint(location.latitude, location.longitude),
            if (location.hasAccuracy()) location.accuracy else null
        )
        recalcularRuta(forzar = false)
    }

    private fun actualizarMiUbicacion(
        punto: GeoPoint,
        precision: Float?,
        enviar: Boolean = true
    ) {
        miUbicacion = punto
        val map = _binding?.mapView ?: return

        val marcador = marcadorYo ?: Marker(map).also {
            it.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_marcador_yo))
            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            it.title = "Tú"
            marcadorYo = it
            map.overlays.add(it)
        }
        marcador.position = punto
        marcador.title = nombreDestino ?: "Tú"
        marcador.snippet = when {
            nombreDestino != null -> "Tu destino (paradero)"
            ubicacionManual -> "Ubicación fijada a mano"
            else -> "Tu ubicación (GPS)"
        }

        if (primeraMiUbicacion && ultimoBus == null) {
            map.controller.animateTo(punto, 16.0, 800L)
        }
        primeraMiUbicacion = false

        map.invalidate()

        if (enviar && compartiendo) {
            busSocketService.enviarUbicacion(
                rutaSeguida, punto.latitude, punto.longitude, nombreDispositivo(), precision
            )
        }
        pintarTarjeta()
    }

    // ======================================
    // TIEMPO REAL (SOCKET.IO)
    // ======================================

    private fun observarTiempoReal() {
        viewLifecycleOwner.lifecycleScope.launch {
            busSocketService.busUpdates.collect { bus -> alRecibirBus(bus) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            busSocketService.busFuera.collect {
                busActivo = false
                pintarTarjeta()
                actualizarTextoTiempo()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            busSocketService.conectado.collect { actualizarTextoTiempo() }
        }
    }

    private fun alRecibirBus(bus: Bus) {
        val map = _binding?.mapView ?: return

        if (
            !bus.latitude.isFinite() || !bus.longitude.isFinite() ||
            bus.latitude !in -90.0..90.0 || bus.longitude !in -180.0..180.0
        ) return

        // Solo se muestra quien transmite en la ruta que se está siguiendo
        if (bus.routeId != rutaSeguida) return

        val punto = GeoPoint(bus.latitude, bus.longitude)

        ultimoBus = bus
        ultimaSenalMs = System.currentTimeMillis()
        busActivo = true

        // Recorrido: se agrega el punto si se movió más de 3 m
        val ultimoPunto = puntosRecorrido.lastOrNull()
        if (ultimoPunto == null || RoutingService.distanciaMetros(ultimoPunto, punto) > 3) {
            puntosRecorrido.add(punto)
            if (puntosRecorrido.size > 2000) puntosRecorrido.removeAt(0)
            lineaRecorrido?.setPoints(ArrayList(puntosRecorrido))
        }

        val nombre = bus.name.ifBlank { "Compañero" }

        val marcador = marcadorBus ?: Marker(map).also {
            it.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_marcador_bus))
            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            marcadorBus = it
            map.overlays.add(it)
        }
        marcador.position = punto
        marcador.title = "$nombre · Ruta ${bus.routeId}"
        marcador.snippet = "Ubicación en vivo"

        if (primeraUbicacionBus) {
            primeraUbicacionBus = false
            encuadrar(punto, miUbicacion)
        }

        map.invalidate()
        recalcularRuta(forzar = false)
        pintarTarjeta()
    }

    /** Ajusta la cámara para ver al compañero y a mí al mismo tiempo. */
    private fun encuadrar(bus: GeoPoint, yo: GeoPoint?) {
        val map = _binding?.mapView ?: return
        if (yo == null || RoutingService.distanciaMetros(bus, yo) < 80) {
            map.controller.animateTo(bus, 16.0, 800L)
            return
        }
        map.post {
            val caja = BoundingBox.fromGeoPoints(listOf(bus, yo))
            map.zoomToBoundingBox(caja.increaseByScale(1.6f), true, 120)
        }
    }

    // ======================================
    // RUTA Y TIEMPO DE LLEGADA (ETA)
    // ======================================

    private fun recalcularRuta(forzar: Boolean) {
        val bus = ultimoBus ?: return
        val yo = miUbicacion ?: return

        val ahora = System.currentTimeMillis()
        if (!forzar && (jobRuta?.isActive == true || ahora - ultimoCalculoMs < 4000)) return
        ultimoCalculoMs = ahora

        if (forzar) jobRuta?.cancel()

        val desde = GeoPoint(bus.latitude, bus.longitude)
        jobRuta = viewLifecycleOwner.lifecycleScope.launch {
            val ruta = RoutingService.calcular(desde, yo)
            if (_binding == null) return@launch
            ultimaRuta = ruta
            lineaRuta?.setPoints(ruta.puntos)
            binding.mapView.invalidate()
            pintarTarjeta()
        }
    }

    private fun pintarTarjeta() {
        val b = _binding ?: return
        val bus = ultimoBus

        if (bus == null) {
            b.tvTitulo.text = "Esperando ubicación en vivo…"
            b.tvEta.text = "--"
            b.tvDetalle.text =
                "Cuando tu compañero comparta su ubicación (ruta $rutaSeguida) verás aquí la ruta y el tiempo de llegada hasta ${nombreDestino ?: "ti"}."
            pintarEstado()
            return
        }

        val hacia = nombreDestino?.let { " → $it" }.orEmpty()
        b.tvTitulo.text = "${bus.name.ifBlank { "Compañero" }} · Ruta ${bus.routeId}$hacia"

        val yo = miUbicacion
        val ruta = ultimaRuta
        val posicionBus = GeoPoint(bus.latitude, bus.longitude)

        when {
            yo == null -> {
                b.tvEta.text = "¿Dónde estás?"
                b.tvDetalle.text =
                    "Activa el GPS o mantén presionado el mapa para fijar tu ubicación."
            }

            RoutingService.distanciaMetros(posicionBus, yo) > 200_000 -> {
                b.tvEta.text = "Tu ubicación está muy lejos"
                b.tvDetalle.text =
                    "Tu GPS marca otro país (el emulador trae California por defecto). " +
                        "Mantén presionado el mapa en Popayán para fijar tu ubicación."
            }

            RoutingService.distanciaMetros(posicionBus, yo) < 60 -> {
                b.tvEta.text = "¡Ya llegó a ${nombreDestino ?: "tu ubicación"}!"
                b.tvDetalle.text = "Está a menos de 60 m de ti."
            }

            ruta == null -> {
                b.tvEta.text = "Calculando ruta…"
                b.tvDetalle.text = ""
            }

            else -> {
                val minutos = ceil(ruta.duracionS / 60.0).toInt().coerceAtLeast(1)
                b.tvEta.text = if (minutos < 60) {
                    "Llega en $minutos min"
                } else {
                    "Llega en ${minutos / 60} h ${minutos % 60} min"
                }

                val km = String.format(Locale.getDefault(), "%.1f km", ruta.distanciaM / 1000.0)
                val calles = if (ruta.calles.isNotEmpty()) {
                    " · por " + ruta.calles.joinToString(" → ")
                } else ""
                val aviso = if (ruta.estimada) " (estimado en línea recta, sin conexión a OSRM)" else ""

                b.tvDetalle.text = "$km$calles$aviso"
            }
        }
        pintarEstado()
    }

    private fun pintarEstado() {
        val b = _binding ?: return
        val segundos = (System.currentTimeMillis() - ultimaSenalMs) / 1000
        val color = when {
            ultimoBus == null -> "#9AA0A6"
            !busActivo -> "#C62828"
            segundos <= 15 -> "#34A853"
            else -> "#F9AB00"
        }
        b.vEstado.backgroundTintList = ColorStateList.valueOf(Color.parseColor(color))
    }

    /** Actualiza "hace X s" cada segundo. */
    private fun iniciarReloj() {
        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                actualizarTextoTiempo()
                delay(1000)
            }
        }
    }

    private fun actualizarTextoTiempo() {
        val b = _binding ?: return
        val conectado = busSocketService.conectado.value
        val sufijoManual = when {
            nombreDestino != null -> " · destino: $nombreDestino"
            ubicacionManual -> " · tu ubicación: fijada a mano"
            else -> ""
        }

        b.tvActualizacion.text = when {
            ultimoBus == null && !conectado ->
                "Conectando al servidor ${Config.SERVER_URL}…"
            ultimoBus == null ->
                "Conectado. Esperando a que alguien comparta en $rutaSeguida…$sufijoManual"
            !busActivo -> {
                val s = (System.currentTimeMillis() - ultimaSenalMs) / 1000
                "Dejó de transmitir · última señal hace ${formatoTiempo(s)}"
            }
            else -> {
                val s = (System.currentTimeMillis() - ultimaSenalMs) / 1000
                val enVivo = if (conectado) "En vivo" else "Reconectando"
                "$enVivo · actualizado hace ${formatoTiempo(s)}$sufijoManual"
            }
        }
        pintarEstado()
    }

    private fun formatoTiempo(segundos: Long): String =
        if (segundos < 60) "$segundos s" else "${segundos / 60} min"

    // ======================================
    // BUSCADOR Y FILTROS DE PARADEROS
    // ======================================

    private fun configurarBuscador() {
        binding.searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    viewModel.buscarParaderos(query.orEmpty())
                    binding.searchView.clearFocus()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.buscarParaderos(newText.orEmpty())
                    return true
                }
            }
        )
    }

    private fun configurarFiltros() {
        binding.chipGroup.setOnCheckedStateChangeListener { _, _ ->
            viewModel.buscarParaderos(binding.searchView.query.toString())
        }
    }

    private fun observarDatos() {
        viewModel.paraderosFiltrados.observe(viewLifecycleOwner) { lista ->
            mostrarParaderos(lista)
        }

        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            if (!mensaje.isNullOrBlank()) {
                Log.w(TAG, "Paraderos: $mensaje")
            }
        }
    }

    private fun mostrarParaderos(paraderos: List<Paradero>) {
        val map = _binding?.mapView ?: return

        marcadoresParaderos.values.forEach { map.overlays.remove(it) }
        marcadoresParaderos.clear()
        paraderosPorId.clear()

        val icono = ContextCompat.getDrawable(requireContext(), R.drawable.ic_marcador_parada)

        paraderos.forEach { paradero ->
            val lat = paradero.latitud
            val lon = paradero.longitud

            if (
                lat != null && lon != null &&
                lat.isFinite() && lon.isFinite() &&
                lat in -90.0..90.0 && lon in -180.0..180.0
            ) {
                val marcador = Marker(map).apply {
                    position = GeoPoint(lat, lon)
                    title = paradero.nombre
                    snippet = "Ruta: ${paradero.rutaId ?: "Sin ruta"}"
                    setIcon(icono)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { _, _ ->
                        consultarLlegadas(paradero.paraderoId)
                        true
                    }
                }

                map.overlays.add(marcador)
                marcadoresParaderos[paradero.paraderoId] = marcador
                paraderosPorId[paradero.paraderoId] = paradero
            }
        }

        traerAlFrente()
        map.invalidate()
    }

    // ======================================
    // CONSULTAR TIEMPOS DE LLEGADA (API REST)
    // ======================================

    private fun consultarLlegadas(paraderoId: String) {
        val paradero = paraderosPorId[paraderoId] ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = repository.obtenerEta(paraderoId)

            if (_binding == null) return@launch

            resultado.onSuccess { llegadas ->
                val mensaje = if (llegadas.isEmpty()) {
                    "No hay llegadas registradas."
                } else {
                    llegadas.joinToString("\n") { eta -> "Bus ${eta.busId}: ${eta.minutos} min" }
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("Llegadas a ${paradero.nombre}")
                    .setMessage(mensaje)
                    .setPositiveButton("Cerrar", null)
                    .show()

            }.onFailure { error ->
                mostrarToast(error.message ?: "No se pudieron consultar las llegadas")
            }
        }
    }

    // ======================================
    // UTILIDADES Y LIBERAR RECURSOS
    // ======================================

    private fun mostrarToast(texto: String) {
        val ctx = context ?: return
        Toast.makeText(ctx, texto, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        jobRuta?.cancel()
        detenerGps()

        if (compartiendo) {
            compartiendo = false
            busSocketService.detenerTransmision()
        }
        busSocketService.disconnect()

        _binding?.mapView?.onDetach()

        // Objetos ligados a la vista; los datos (recorrido, última posición) se conservan
        marcadoresParaderos.clear()
        paraderosPorId.clear()
        marcadorBus = null
        marcadorYo = null
        lineaRecorrido = null
        lineaRuta = null
        primeraUbicacionBus = true
        primeraMiUbicacion = true

        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val TAG = "MapFragment"
        val POPAYAN = GeoPoint(2.4448, -76.6147)

        // Mapa estándar de OpenStreetMap. Funciona sin API key siempre que la app
        // se identifique con un User-Agent propio (ver onCreateView).
        val MAPA_OSM = XYTileSource(
            "OSMPublico", 0, 19, 256, ".png",
            arrayOf("https://tile.openstreetmap.org/"),
            "© OpenStreetMap"
        )

        // Alternativa (Esri World Street Map), también sin API key.
        // Esri usa el orden z/y/x en la URL, por eso se sobrescribe getTileURLString.
        val MAPA_ESRI = object : OnlineTileSourceBase(
            "EsriCalles", 0, 19, 256, "",
            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/"),
            "© Esri, OpenStreetMap"
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String =
                baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                    MapTileIndex.getY(pMapTileIndex) + "/" +
                    MapTileIndex.getX(pMapTileIndex)
        }
    }
}
