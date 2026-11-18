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
import com.example.bus_popayan.R
import com.example.bus_popayan.data.model.PreferenciasUsuario
import com.example.bus_popayan.data.repository.BusRepository
import com.example.bus_popayan.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val repository = BusRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Si ya hay una sesión guardada, entrar directo al mapa
        if (PreferenciasUsuario.sesion(requireContext()) != null) {
            irAlMapa()
            return
        }

        binding.btnLogin.setOnClickListener { iniciarSesion() }

        binding.btnGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun iniciarSesion() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Correo no válido"
            return
        }

        cargando(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = repository.iniciarSesion(email, password)
            if (_binding == null) return@launch
            cargando(false)

            resultado.onSuccess { usuario ->
                PreferenciasUsuario.guardarSesion(requireContext(), usuario)
                Toast.makeText(requireContext(), "Bienvenido, ${usuario.nombre}", Toast.LENGTH_SHORT).show()
                irAlMapa()
            }.onFailure { error ->
                Toast.makeText(requireContext(), error.message ?: "No se pudo iniciar sesión", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun cargando(activo: Boolean) {
        binding.btnLogin.isEnabled = !activo
        binding.btnLogin.text = if (activo) "Verificando…" else "Iniciar Sesión"
    }

    private fun irAlMapa() {
        findNavController().navigate(R.id.action_loginFragment_to_mapFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}