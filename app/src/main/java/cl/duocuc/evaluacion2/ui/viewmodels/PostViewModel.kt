package cl.duocuc.evaluacion2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duocuc.evaluacion2.data.model.PostRequest
import cl.duocuc.evaluacion2.data.model.PostResponse
import cl.duocuc.evaluacion2.data.repository.PostRepository
import cl.duocuc.evaluacion2.data.repository.RespuestaApiVaciaException
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class PostUiState(
    val cargando: Boolean = true,
    val publicaciones: List<PostResponse> = emptyList(),
    val mensajeError: String? = null,
    val mutacionEnCurso: Boolean = false,
    val mensajeMutacion: String? = null,
    val mutacionFallida: Boolean = false
)

class PostViewModel(
    private val repository: PostRepository = PostRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostUiState())
    val uiState = _uiState.asStateFlow()

    init {
        cargarPublicaciones()
    }

    fun cargarPublicaciones() {
        viewModelScope.launch {
            _uiState.value = PostUiState(cargando = true)
            try {
                _uiState.value = PostUiState(
                    cargando = false,
                    publicaciones = repository.obtenerPublicaciones()
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: SocketTimeoutException) {
                _uiState.value = PostUiState(
                    cargando = false,
                    mensajeError = "La solicitud agotó el tiempo de espera. Inténtalo nuevamente."
                )
            } catch (e: UnknownHostException) {
                _uiState.value = PostUiState(
                    cargando = false,
                    mensajeError = "No se pudo conectar con el servicio. Revisa tu conexión a Internet."
                )
            } catch (e: HttpException) {
                _uiState.value = PostUiState(
                    cargando = false,
                    mensajeError = "El servicio respondió con un error HTTP ${e.code()}."
                )
            } catch (e: JsonParseException) {
                _uiState.value = PostUiState(
                    cargando = false,
                    mensajeError = "El servicio devolvió una respuesta JSON inválida."
                )
            } catch (e: RespuestaApiVaciaException) {
                _uiState.value = PostUiState(
                    cargando = false,
                    mensajeError = "El servicio respondió sin contenido."
                )
            } catch (e: IOException) {
                _uiState.value = PostUiState(
                    cargando = false,
                    mensajeError = "Ocurrió un error de conexión. Inténtalo nuevamente."
                )
            }
        }
    }

    fun crearPublicacion(titulo: String, contenido: String) {
        val solicitud = crearSolicitud(titulo, contenido) ?: return
        ejecutarMutacion("POST exitoso. JSONPlaceholder no conserva los cambios.") {
            val creada = repository.crearPublicacion(solicitud)
            _uiState.update { estado ->
                estado.copy(
                    publicaciones = listOf(creada) +
                        estado.publicaciones.filterNot { it.id == creada.id }
                )
            }
        }
    }

    fun actualizarPublicacion(publicacion: PostResponse, titulo: String, contenido: String) {
        val solicitud = crearSolicitud(titulo, contenido, publicacion.userId) ?: return
        ejecutarMutacion("PUT exitoso. JSONPlaceholder no conserva los cambios.") {
            val actualizada = repository.actualizarPublicacion(publicacion.id, solicitud)
            _uiState.update { estado ->
                estado.copy(
                    publicaciones = estado.publicaciones.map {
                        if (it.id == actualizada.id) actualizada else it
                    }
                )
            }
        }
    }

    fun eliminarPublicacion(id: Int) {
        ejecutarMutacion("DELETE exitoso. JSONPlaceholder no conserva los cambios.") {
            repository.eliminarPublicacion(id)
            _uiState.update { estado ->
                estado.copy(publicaciones = estado.publicaciones.filterNot { it.id == id })
            }
        }
    }

    private fun crearSolicitud(
        titulo: String,
        contenido: String,
        userId: Int = 1
    ): PostRequest? {
        if (titulo.isBlank() || contenido.isBlank()) {
            _uiState.update {
                it.copy(
                    mensajeMutacion = "El título y el contenido son obligatorios.",
                    mutacionFallida = true
                )
            }
            return null
        }
        return PostRequest(
            userId = userId,
            title = titulo.trim(),
            body = contenido.trim()
        )
    }

    private fun ejecutarMutacion(mensajeExito: String, operacion: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    mutacionEnCurso = true,
                    mensajeMutacion = null,
                    mutacionFallida = false
                )
            }
            try {
                operacion()
                _uiState.update {
                    it.copy(
                        mutacionEnCurso = false,
                        mensajeMutacion = mensajeExito
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SocketTimeoutException) {
                informarErrorMutacion(
                    "La solicitud agotó el tiempo de espera. Inténtalo nuevamente."
                )
            } catch (e: UnknownHostException) {
                informarErrorMutacion(
                    "No se pudo conectar con el servicio. Revisa tu conexión a Internet."
                )
            } catch (e: HttpException) {
                informarErrorMutacion("El servicio respondió con un error HTTP ${e.code()}.")
            } catch (e: JsonParseException) {
                informarErrorMutacion("El servicio devolvió una respuesta JSON inválida.")
            } catch (e: RespuestaApiVaciaException) {
                informarErrorMutacion("El servicio respondió sin contenido.")
            } catch (e: IOException) {
                informarErrorMutacion("Ocurrió un error de conexión. Inténtalo nuevamente.")
            }
        }
    }

    private fun informarErrorMutacion(mensaje: String) {
        _uiState.update {
            it.copy(
                mutacionEnCurso = false,
                mensajeMutacion = mensaje,
                mutacionFallida = true
            )
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(PostViewModel::class.java))
                    return PostViewModel() as T
                }
            }
    }
}
