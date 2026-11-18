package com.example.bus_popayan.ui.api

import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

// Pantalla de demostración: ETA de un paradero + registro de usuario.
// La interfaz se arma en código para no depender de un layout XML.
class ApiTestActivity : AppCompatActivity() {

    private val viewModel: BusViewModel by viewModels()

    private lateinit var etParadero: EditText
    private lateinit var tvEta: TextView
    private lateinit var etNombre: EditText
    private lateinit var etEmail: EditText
    private lateinit var spTipo: Spinner
    private lateinit var tvRegistro: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(construirUi())

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.etaState.collect { estado ->
                        tvEta.text = when (estado) {
                            is UiState.Idle -> ""
                            is UiState.Loading -> "Consultando..."
                            is UiState.Error -> "Error: ${estado.message}"
                            is UiState.Success ->
                                if (estado.data.isEmpty()) "Sin buses estimados para ese paradero"
                                else estado.data.joinToString("\n") {
                                    "Bus ${it.busId} (ruta ${it.rutaId}): llega en ${it.minutos} min"
                                }
                        }
                    }
                }
                launch {
                    viewModel.registroState.collect { estado ->
                        tvRegistro.text = when (estado) {
                            is UiState.Idle -> ""
                            is UiState.Loading -> "Registrando..."
                            is UiState.Error -> "Error: ${estado.message}"
                            is UiState.Success ->
                                "Usuario registrado con ID ${estado.data.usuarioId}\n(${estado.data.nombre})"
                        }
                    }
                }
            }
        }
    }

    private fun titulo(texto: String) = TextView(this).apply {
        text = texto
        textSize = 18f
        setPadding(0, dp(20), 0, dp(8))
    }

    private fun construirUi(): ScrollView {
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(40), dp(20), dp(20))
        }
        val ancho = LinearLayout.LayoutParams.MATCH_PARENT
        val alto = LinearLayout.LayoutParams.WRAP_CONTENT

        // --- Funcionalidad 1: ETA ---
        raiz.addView(titulo("1. Tiempo de llegada a un paradero"))
        etParadero = EditText(this).apply { hint = "ID del paradero (ej: P001)" }
        raiz.addView(etParadero, LinearLayout.LayoutParams(ancho, alto))
        raiz.addView(Button(this).apply {
            text = "Consultar"
            setOnClickListener { viewModel.consultarEta(etParadero.text.toString()) }
        })
        tvEta = TextView(this).apply { textSize = 16f }
        raiz.addView(tvEta)

        // --- Funcionalidad 2: registro de usuario ---
        raiz.addView(titulo("2. Registrar usuario"))
        etNombre = EditText(this).apply { hint = "Nombre" }
        raiz.addView(etNombre, LinearLayout.LayoutParams(ancho, alto))
        etEmail = EditText(this).apply {
            hint = "Email"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or InputType.TYPE_CLASS_TEXT
        }
        raiz.addView(etEmail, LinearLayout.LayoutParams(ancho, alto))
        spTipo = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@ApiTestActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Pasajero", "Operador", "Administrador")
            )
        }
        raiz.addView(spTipo, LinearLayout.LayoutParams(ancho, alto))
        raiz.addView(Button(this).apply {
            text = "Registrar"
            setOnClickListener {
                viewModel.registrarUsuario(
                    etNombre.text.toString(),
                    etEmail.text.toString(),
                    spTipo.selectedItem.toString()
                )
            }
        })
        tvRegistro = TextView(this).apply { textSize = 16f }
        raiz.addView(tvRegistro)

        return ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ancho, ViewGroup.LayoutParams.MATCH_PARENT)
            addView(raiz)
        }
    }
}
