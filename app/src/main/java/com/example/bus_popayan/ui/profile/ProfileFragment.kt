package com.example.bus_popayan.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.bus_popayan.R
import com.example.bus_popayan.data.model.PreferenciasUsuario
import com.example.bus_popayan.databinding.FragmentProfileBinding
import com.example.bus_popayan.ui.map.DestinoSeleccionado
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Datos del usuario que inició sesión
        PreferenciasUsuario.sesion(requireContext())?.let { usuario ->
            binding.tvNombre.text = usuario.nombre
            binding.tvEmail.text = usuario.email
        }

        binding.btnFavoritas.setOnClickListener { mostrarFavoritas() }
        binding.btnHistorial.setOnClickListener { mostrarHistorial() }

        binding.btnLogout.setOnClickListener {
            PreferenciasUsuario.cerrarSesion(requireContext())
            Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
            // Regresar al login borrando el historial de pantallas
            findNavController().navigate(
                R.id.loginFragment, null,
                NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
            )
        }
    }

    // ======================================
    // RUTAS FAVORITAS
    // ======================================

    private fun mostrarFavoritas() {
        val favoritas = PreferenciasUsuario.favoritas(requireContext())

        if (favoritas.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Rutas favoritas")
                .setMessage(
                    "Todavía no tienes rutas favoritas.\n\n" +
                            "Ve a Rutas, abre una ruta y toca «☆ Agregar a favoritas»."
                )
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        val opciones = favoritas.map { f ->
            "★ ${f.rutaNombre} (${f.rutaId})\n     Destino: ${f.paraderoNombre}"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Rutas favoritas")
            .setItems(opciones) { _, i ->
                val f = favoritas[i]
                abrirEnMapa(f.rutaId, f.paraderoNombre, f.latitud, f.longitud)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    // ======================================
    // HISTORIAL DE VIAJES
    // ======================================

    private fun mostrarHistorial() {
        val viajes = PreferenciasUsuario.historial(requireContext())

        if (viajes.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Historial de viajes")
                .setMessage(
                    "Aún no hay viajes registrados.\n\n" +
                            "Cada vez que eliges un paradero en Rutas, el viaje se guarda aquí."
                )
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        val formato = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val opciones = viajes.map { v ->
            "${formato.format(Date(v.fecha))}\n     ${v.rutaNombre} → ${v.paraderoNombre}"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Historial de viajes")
            .setItems(opciones) { _, i ->
                val v = viajes[i]
                abrirEnMapa(v.rutaId, v.paraderoNombre, v.latitud, v.longitud)
            }
            .setNeutralButton("Borrar historial") { _, _ ->
                PreferenciasUsuario.borrarHistorial(requireContext())
                Toast.makeText(requireContext(), "Historial borrado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    // Abre el mapa con ese paradero como destino y siguiendo esa ruta
    private fun abrirEnMapa(rutaId: String, nombre: String, lat: Double, lon: Double) {
        if (lat.isNaN() || lon.isNaN()) {
            Toast.makeText(requireContext(), "Este destino no tiene coordenadas", Toast.LENGTH_SHORT).show()
            return
        }
        DestinoSeleccionado.elegir(rutaId, nombre, lat, lon)
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottom_navigation)
            ?.selectedItemId = R.id.mapFragment
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}