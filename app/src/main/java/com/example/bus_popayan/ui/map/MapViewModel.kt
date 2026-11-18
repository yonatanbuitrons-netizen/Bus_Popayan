
package com.example.bus_popayan.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bus_popayan.data.model.Paradero
import com.example.bus_popayan.data.repository.BusRepository
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    private val repository = BusRepository()

    private val _paraderos = MutableLiveData<List<Paradero>>(emptyList())
    val paraderos: LiveData<List<Paradero>> = _paraderos

    private val _paraderosFiltrados =
        MutableLiveData<List<Paradero>>(emptyList())
    val paraderosFiltrados: LiveData<List<Paradero>> =
        _paraderosFiltrados

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _cargando = MutableLiveData<Boolean>(false)
    val cargando: LiveData<Boolean> = _cargando

    private var todosLosParaderos = emptyList<Paradero>()

    private var busquedaActual = ""

    init {
        cargarParaderos()
    }

    fun cargarParaderos() {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null

            val resultado = repository.obtenerParaderos()

            resultado.onSuccess { lista ->
                todosLosParaderos = lista
                _paraderos.value = lista
                aplicarFiltro()
            }.onFailure { exception ->
                _error.value =
                    exception.message ?: "Error al cargar paraderos"
            }

            _cargando.value = false
        }
    }

    fun buscarParaderos(texto: String) {
        busquedaActual = texto.trim()
        aplicarFiltro()
    }

    private fun aplicarFiltro() {
        val filtrados = todosLosParaderos.filter { paradero ->

            val coincideNombre = paradero.nombre.contains(
                busquedaActual,
                ignoreCase = true
            )

            val coincideRuta = paradero.rutaId?.contains(
                busquedaActual,
                ignoreCase = true
            ) == true

            coincideNombre || coincideRuta
        }

        _paraderosFiltrados.value = filtrados
    }
}
