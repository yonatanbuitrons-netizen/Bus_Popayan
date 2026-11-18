package com.example.bus_popayan.ui.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bus_popayan.data.model.Eta
import com.example.bus_popayan.data.model.Usuario
import com.example.bus_popayan.data.repository.BusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class BusViewModel(private val repository: BusRepository = BusRepository()) : ViewModel() {

    private val _etaState = MutableStateFlow<UiState<List<Eta>>>(UiState.Idle)
    val etaState: StateFlow<UiState<List<Eta>>> = _etaState

    private val _registroState = MutableStateFlow<UiState<Usuario>>(UiState.Idle)
    val registroState: StateFlow<UiState<Usuario>> = _registroState

    // Funcionalidad 1: tiempo de llegada a un paradero
    fun consultarEta(paraderoId: String) {
        if (paraderoId.isBlank()) {
            _etaState.value = UiState.Error("Escribe el ID del paradero (ej: P001)")
            return
        }
        _etaState.value = UiState.Loading
        viewModelScope.launch {
            repository.obtenerEta(paraderoId.trim().uppercase())
                .onSuccess { _etaState.value = UiState.Success(it) }
                .onFailure { _etaState.value = UiState.Error(it.message ?: "Error desconocido") }
        }
    }

    // Funcionalidad 2: registro de usuario
    fun registrarUsuario(nombre: String, email: String, tipo: String) {
        if (nombre.isBlank() || email.isBlank()) {
            _registroState.value = UiState.Error("Nombre y email son obligatorios")
            return
        }
        _registroState.value = UiState.Loading
        viewModelScope.launch {
            repository.registrarUsuario(Usuario(nombre = nombre.trim(), email = email.trim(), tipo = tipo))
                .onSuccess { _registroState.value = UiState.Success(it) }
                .onFailure { _registroState.value = UiState.Error(it.message ?: "Error desconocido") }
        }
    }
}
