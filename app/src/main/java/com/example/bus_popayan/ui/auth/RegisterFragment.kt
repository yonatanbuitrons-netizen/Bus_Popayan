package com.example.bus_popayan.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bus_popayan.data.repository.BusRepository
import com.example.bus_popayan.databinding.FragmentRegisterBinding
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val repository = BusRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnRegister.setOnClickListener { registrar() }
    }

    private fun registrar() {
        val nombre = binding.etNombre.text.toString().trim()
        val email = binding.etEmailRegistro.text.toString().trim()
        val password = binding.etPasswordRegistro.text.toString()

        var valido = true
        if (nombre.length < 3) {
            binding.etNombre.error = "Escribe tu nombre (mínimo 3 letras)"
            valido = false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmailRegistro.error = "Correo no válido"
            valido = false
        }
        if (password.length < 6) {
            binding.etPasswordRegistro.error = "Mínimo 6 caracteres"
            valido = false
        }
        if (!valido) return

        cargando(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = repository.crearCuenta(nombre, email, password)
            if (_binding == null) return@launch
            cargando(false)

            resultado.onSuccess {
                Toast.makeText(
                    requireContext(),
                    "Cuenta creada exitosamente. Ahora inicia sesión.",
                    Toast.LENGTH_LONG
                ).show()
                findNavController().popBackStack()
            }.onFailure { error ->
                Toast.makeText(requireContext(), error.message ?: "No se pudo crear la cuenta", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun cargando(activo: Boolean) {
        binding.btnRegister.isEnabled = !activo
        binding.btnRegister.text = if (activo) "Creando cuenta…" else "Registrarse"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}