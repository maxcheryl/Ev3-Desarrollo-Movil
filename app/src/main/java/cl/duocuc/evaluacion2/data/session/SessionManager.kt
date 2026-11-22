package cl.duocuc.evaluacion2.data.session

import cl.duocuc.evaluacion2.data.model.PerfilUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UsuarioSesion(
    val id: Long,
    val nombre: String,
    val correo: String,
    val perfil: PerfilUsuario
)

class SessionManager {

    private val _usuario = MutableStateFlow<UsuarioSesion?>(null)
    val usuario: StateFlow<UsuarioSesion?> = _usuario.asStateFlow()

    val actual: UsuarioSesion? get() = _usuario.value

    fun iniciar(usuario: UsuarioSesion) {
        _usuario.value = usuario
    }

    fun cerrar() {
        _usuario.value = null
    }
}
