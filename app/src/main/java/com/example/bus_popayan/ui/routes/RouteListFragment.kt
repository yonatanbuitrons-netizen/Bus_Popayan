package com.example.bus_popayan.ui.routes

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.bus_popayan.R
import com.example.bus_popayan.data.model.Paradero
import com.example.bus_popayan.data.model.Ruta
import com.example.bus_popayan.data.repository.BusRepository
import com.example.bus_popayan.databinding.FragmentRouteListBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.example.bus_popayan.ui.map.DestinoSeleccionado
import com.example.bus_popayan.data.model.PreferenciasUsuario
import com.example.bus_popayan.data.model.RutaFavorita
import com.example.bus_popayan.data.model.Viaje
import kotlinx.coroutines.launch

class RouteListFragment : Fragment() {

    private var _binding: FragmentRouteListBinding? = null
    private val binding get() = _binding!!

    private val repository = BusRepository()

    private var todasLasRutas = listOf<Ruta>()
    private var busquedaActual = ""
    private var filtroActual = "TODAS"

    private lateinit var adapter: RoutesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteListBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RoutesAdapter(emptyList()) { ruta ->
            mostrarParaderos(ruta)
        }

        binding.rvRoutes.adapter = adapter

        configurarBuscador()
        configurarFiltros()
        cargarRutas()
    }

    // BUSCADOR DE RUTAS
    private fun configurarBuscador() {

        binding.searchRoutes.setOnQueryTextListener(
            object : androidx.appcompat.widget.SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(
                    query: String?
                ): Boolean {

                    busquedaActual = query.orEmpty()
                    aplicarFiltros()
                    binding.searchRoutes.clearFocus()

                    return true
                }

                override fun onQueryTextChange(
                    newText: String?
                ): Boolean {

                    busquedaActual = newText.orEmpty()
                    aplicarFiltros()

                    return true
                }
            }
        )
    }

    // FILTROS DE RUTAS
    private fun configurarFiltros() {

        binding.chipGroupRoutes.setOnCheckedStateChangeListener {
                _, checkedIds ->

            filtroActual = when (checkedIds.firstOrNull()) {

                R.id.chipActive -> "ACTIVAS"

                R.id.chipInactive -> "INACTIVAS"

                else -> "TODAS"
            }

            aplicarFiltros()
        }
    }

    // CONSULTAR RUTAS DESDE MONGODB
    private fun cargarRutas() {

        viewLifecycleOwner.lifecycleScope.launch {

            val resultado = repository.obtenerRutas()

            if (_binding == null) return@launch

            resultado.onSuccess { rutas ->

                todasLasRutas = rutas
                aplicarFiltros()

            }.onFailure { error ->

                Toast.makeText(
                    requireContext(),
                    "Error al cargar rutas: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // APLICAR BUSQUEDA Y FILTROS
    private fun aplicarFiltros() {

        val texto = busquedaActual.trim()

        val rutasFiltradas = todasLasRutas.filter { ruta ->

            val coincideBusqueda =
                ruta.nombre.contains(
                    texto,
                    ignoreCase = true
                ) ||
                        ruta.numero.orEmpty().contains(
                            texto,
                            ignoreCase = true
                        ) ||
                        ruta.origen.orEmpty().contains(
                            texto,
                            ignoreCase = true
                        ) ||
                        ruta.destino.orEmpty().contains(
                            texto,
                            ignoreCase = true
                        )

            val coincideEstado = when (filtroActual) {

                "ACTIVAS" ->
                    ruta.estado.equals(
                        "Activo",
                        ignoreCase = true
                    ) ||
                            ruta.estado.equals(
                                "Activa",
                                ignoreCase = true
                            )

                "INACTIVAS" ->
                    ruta.estado.equals(
                        "Inactivo",
                        ignoreCase = true
                    ) ||
                            ruta.estado.equals(
                                "Inactiva",
                                ignoreCase = true
                            )

                else -> true
            }

            coincideBusqueda && coincideEstado
        }

        adapter.actualizarRutas(rutasFiltradas)

        binding.tvRoutesCount.text =
            "${rutasFiltradas.size} rutas encontradas"

        binding.tvEmptyRoutes.visibility =
            if (rutasFiltradas.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    // MOSTRAR DETALLE DEL RECORRIDO
    private fun mostrarParaderos(ruta: Ruta) {

        viewLifecycleOwner.lifecycleScope.launch {

            val resultado =
                repository.obtenerParaderos(ruta.rutaId)

            if (_binding == null) return@launch

            resultado.onSuccess { paraderos ->

                val vista = layoutInflater.inflate(
                    R.layout.dialog_route_detail,
                    null
                )

                val nombre = vista.findViewById<TextView>(
                    R.id.tvDetailName
                )

                val numero = vista.findViewById<TextView>(
                    R.id.tvDetailNumber
                )

                val origen = vista.findViewById<TextView>(
                    R.id.tvDetailOrigin
                )

                val destino = vista.findViewById<TextView>(
                    R.id.tvDetailDestination
                )

                val contenedor = vista.findViewById<LinearLayout>(
                    R.id.containerStops
                )

                nombre.text = ruta.nombre

                numero.text =
                    "Ruta ${ruta.numero ?: "Sin número"} · ${ruta.rutaId}"

                origen.text =
                    "Origen: ${ruta.origen ?: "No registrado"}"

                destino.text =
                    "Destino: ${ruta.destino ?: "No registrado"}"

                // Paradero destino para la favorita: el último con coordenadas
                val paraderoDestino = paraderos.lastOrNull {
                    it.latitud != null && it.longitud != null
                }

                val textoFavorita =
                    if (PreferenciasUsuario.esFavorita(requireContext(), ruta.rutaId))
                        "★ Quitar de favoritas"
                    else
                        "☆ Agregar a favoritas"

                val constructor = AlertDialog.Builder(
                    requireContext()
                )
                    .setView(vista)
                    .setNegativeButton("Cerrar", null)

                if (paraderoDestino != null) {
                    constructor.setPositiveButton(textoFavorita) { _, _ ->
                        val quedo = PreferenciasUsuario.alternarFavorita(
                            requireContext(),
                            RutaFavorita(
                                rutaId = ruta.rutaId,
                                rutaNombre = ruta.nombre,
                                paraderoNombre = paraderoDestino.nombre,
                                latitud = paraderoDestino.latitud ?: 0.0,
                                longitud = paraderoDestino.longitud ?: 0.0
                            )
                        )
                        Toast.makeText(
                            requireContext(),
                            if (quedo) "${ruta.nombre} agregada a favoritas"
                            else "${ruta.nombre} quitada de favoritas",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                val dialogo = constructor.create()

                if (paraderos.isEmpty()) {

                    val mensaje = TextView(
                        requireContext()
                    ).apply {

                        text =
                            "Esta ruta no tiene paraderos registrados."

                        textSize = 14f

                        setTextColor(
                            Color.parseColor("#64748B")
                        )
                    }

                    contenedor.addView(mensaje)

                } else {

                    val ayuda = TextView(requireContext()).apply {
                        text = "Toca un paradero para verlo en el mapa en vivo.\n" +
                                "Mantén presionado para ver los tiempos de llegada."
                        textSize = 13f
                        setTextColor(Color.parseColor("#64748B"))
                        setPadding(0, 0, 0, (10 * resources.displayMetrics.density).toInt())
                    }

                    contenedor.addView(ayuda)

                    paraderos.forEachIndexed { index, paradero ->

                        val item = TextView(
                            requireContext()
                        ).apply {

                            text =
                                "${index + 1}. ${paradero.nombre}\n" +
                                        (paradero.direccion ?: "Sin dirección")

                            textSize = 15f

                            setTextColor(
                                Color.parseColor("#334155")
                            )

                            val density =
                                resources.displayMetrics.density

                            val horizontal =
                                (16 * density).toInt()

                            val vertical =
                                (14 * density).toInt()

                            setPadding(
                                horizontal,
                                vertical,
                                horizontal,
                                vertical
                            )

                            background = GradientDrawable().apply {

                                setColor(
                                    Color.parseColor("#F1F5F9")
                                )

                                cornerRadius = 12 * density
                            }

                            // Toque: ver este paradero como destino en el mapa en vivo.
                            // Toque largo: ver los tiempos de llegada registrados.
                            setOnClickListener {

                                dialogo.dismiss()

                                irAlMapa(ruta, paradero)
                            }

                            setOnLongClickListener {

                                dialogo.dismiss()

                                mostrarEta(paradero)

                                true
                            }
                        }

                        val params =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {

                                bottomMargin =
                                    (10 * resources.displayMetrics.density)
                                        .toInt()
                            }

                        contenedor.addView(item, params)
                    }
                }

                dialogo.show()

            }.onFailure { error ->

                Toast.makeText(
                    requireContext(),
                    "Error al consultar paraderos: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ABRIR EL MAPA CON ESTE PARADERO COMO DESTINO
    private fun irAlMapa(ruta: Ruta, paradero: Paradero) {

        val lat = paradero.latitud
        val lon = paradero.longitud

        if (lat == null || lon == null) {
            Toast.makeText(
                requireContext(),
                "Este paradero no tiene coordenadas registradas",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        DestinoSeleccionado.elegir(ruta.rutaId, paradero.nombre, lat, lon)

        // Guardar en el historial de viajes
        PreferenciasUsuario.registrarViaje(
            requireContext(),
            Viaje(
                fecha = System.currentTimeMillis(),
                rutaId = ruta.rutaId,
                rutaNombre = ruta.nombre,
                paraderoNombre = paradero.nombre,
                latitud = lat,
                longitud = lon
            )
        )

        // Cambiar a la pestaña "Mapa" de la barra inferior
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottom_navigation)
            ?.selectedItemId = R.id.mapFragment
    }

    // CONSULTAR TIEMPOS ESTIMADOS DE LLEGADA
    private fun mostrarEta(paradero: Paradero) {

        viewLifecycleOwner.lifecycleScope.launch {

            val resultado =
                repository.obtenerEta(paradero.paraderoId)

            if (_binding == null) return@launch

            resultado.onSuccess { etas ->

                val mensaje = if (etas.isEmpty()) {

                    "No hay tiempos de llegada registrados."

                } else {

                    etas.joinToString("\n\n") { eta ->

                        "Bus: ${eta.busId}\n" +
                                "Tiempo estimado: ${eta.minutos} minutos"
                    }
                }

                AlertDialog.Builder(requireContext())
                    .setTitle(
                        "Llegadas a ${paradero.nombre}"
                    )
                    .setMessage(mensaje)
                    .setPositiveButton("Cerrar", null)
                    .show()

            }.onFailure { error ->

                Toast.makeText(
                    requireContext(),
                    "Error al consultar llegada: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {

        binding.rvRoutes.adapter = null

        _binding = null

        super.onDestroyView()
    }

    // ADAPTADOR DE TARJETAS DE RUTAS
    class RoutesAdapter(
        private var routes: List<Ruta>,
        private val onRutaClick: (Ruta) -> Unit
    ) : RecyclerView.Adapter<RoutesAdapter.ViewHolder>() {

        class ViewHolder(view: View) :
            RecyclerView.ViewHolder(view) {

            val nombre: TextView =
                view.findViewById(R.id.tvRouteName)

            val origen: TextView =
                view.findViewById(R.id.tvRouteOrigin)

            val destino: TextView =
                view.findViewById(R.id.tvRouteDestination)

            val boton: MaterialButton =
                view.findViewById(R.id.btnViewStops)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {

            val view = LayoutInflater.from(
                parent.context
            ).inflate(
                R.layout.item_route,
                parent,
                false
            )

            return ViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int
        ) {

            val ruta = routes[position]

            holder.nombre.text = ruta.nombre

            holder.origen.text =
                "Origen: ${ruta.origen ?: "No registrado"}"

            holder.destino.text =
                "Destino: ${ruta.destino ?: "No registrado"}"

            val clickListener = View.OnClickListener {

                val index = holder.bindingAdapterPosition

                if (index != RecyclerView.NO_POSITION) {

                    onRutaClick(routes[index])
                }
            }

            holder.boton.setOnClickListener(clickListener)

            holder.itemView.setOnClickListener(clickListener)
        }

        override fun getItemCount(): Int {
            return routes.size
        }

        fun actualizarRutas(nuevasRutas: List<Ruta>) {

            routes = nuevasRutas

            notifyDataSetChanged()
        }
    }
}