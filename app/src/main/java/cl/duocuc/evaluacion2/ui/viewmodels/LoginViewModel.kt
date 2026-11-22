package cl.duocuc.evaluacion2.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.DatabaseSeed
import cl.duocuc.evaluacion2.data.model.PerfilUsuario
import cl.duocuc.evaluacion2.data.repository.UsuarioRepository
import cl.duocuc.evaluacion2.data.session.SessionManager
import cl.duocuc.evaluacion2.data.session.UsuarioSesion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val correo: String = "",
    val clave: String = "",
    val correoError: String? = null,
    val claveError: String? = null,
    val mensajeError: String? = null,
    val cargando: Boolean = false
)

data class CuentaDemo(
    val etiqueta: String,
    val correo: String,
    val clave: String
)

/**
 * ViewModel del inicio de sesión contra los usuarios locales de demostración.
 */
class LoginViewModel(
    application: Application,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    private val repository = UsuarioRepository(
        AppDatabase.getInstance(application).usuarioDao()
    )

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado

    val usuario: StateFlow<UsuarioSesion?> = sessionManager.usuario

    fun onCorreoChange(valor: String) {
        _estado.update {
            it.copy(
                correo = valor,
                correoError = null,
                mensajeError = null
            )
        }
    }

    fun onClaveChange(valor: String) {
        _estado.update {
            it.copy(
                clave = valor,
                claveError = null,
                mensajeError = null
            )
        }
    }

    fun ingresar(onExitoso: () -> Unit) {
        val actual = _estado.value

        val correoError = if (actual.correo.isBlank()) "El correo es obligatorio." else null
        val claveError = if (actual.clave.isBlank()) "La contraseña es obligatoria." else null

        if (correoError != null || claveError != null) {
            _estado.update {
                it.copy(correoError = correoError, claveError = claveError)
            }
            return
        }

        if (actual.cargando) return
        _estado.update { it.copy(cargando = true, mensajeError = null) }

        viewModelScope.launch {
            try {
                val encontrado = repository.autenticar(actual.correo, actual.clave)
                if (encontrado == null) {
                    _estado.update {
                        it.copy(
                            cargando = false,
                            mensajeError = "Correo o contraseña incorrectos."
                        )
                    }
                    return@launch
                }

                sessionManager.iniciar(
                    UsuarioSesion(
                        id = encontrado.id,
                        nombre = encontrado.nombre,
                        correo = encontrado.correo,
                        perfil = PerfilUsuario.desdeNombre(encontrado.perfil)
                    )
                )
                _estado.value = LoginUiState()
                onExitoso()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update {
                    it.copy(
                        cargando = false,
                        mensajeError = "No se pudo iniciar sesión: ${e.message}"
                    )
                }
            }
        }
    }

    fun cerrarSesion() {
        sessionManager.cerrar()
        _estado.value = LoginUiState()
    }

    companion object {
        val cuentasDemo = listOf(
            CuentaDemo(
                etiqueta = "Ejecutivo comercial",
                correo = DatabaseSeed.USUARIO_DALI_CORREO,
                clave = DatabaseSeed.USUARIO_DALI_CLAVE
            ),
            CuentaDemo(
                etiqueta = "Supervisor comercial",
                correo = DatabaseSeed.USUARIO_MAXINE_CORREO,
                clave = DatabaseSeed.USUARIO_MAXINE_CLAVE
            )
        )

        fun factory(
            application: Application,
            sessionManager: SessionManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(LoginViewModel::class.java))
                return LoginViewModel(application, sessionManager) as T
            }
        }
    }
}
