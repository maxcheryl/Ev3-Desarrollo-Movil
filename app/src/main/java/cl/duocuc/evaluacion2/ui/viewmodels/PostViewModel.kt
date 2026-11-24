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
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

enum class CampoPublicacion {
    TITULO,
    CONTENIDO
}

data class PostUiState(
    val cargando: Boolean = true,
    val publicaciones: List<PostResponse> = emptyList(),
    // Los POST de JSONPlaceholder no se guardan en el servidor.
    val idsCreadosConPost: Set<Int> = emptySet(),
    val mensajeError: String? = null,
    val mutacionEnCurso: Boolean = false,
    val mensajeMutacion: String? = null,
    val mutacionFallida: Boolean = false,
    val erroresEvento: EventoPostErrores = EventoPostErrores(),
    val erroresPublicacion: PublicacionErrores = PublicacionErrores()
)

data class EventoPostErrores(
    val idEvento: String? = null,
    val nombreEvento: String? = null,
    val fecha: String? = null,
    val ubicacion: String? = null,
    val organizacion: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(idEvento, nombreEvento, fecha, ubicacion, organizacion)
            .any { it != null }
}

data class PublicacionErrores(
    val titulo: String? = null,
    val contenido: String? = null
) {
    val hayErrores: Boolean
        get() = titulo != null || contenido != null
}

enum class CampoEventoPost {
    ID,
    NOMBRE,
    FECHA,
    UBICACION,
    ORGANIZACION
}

/** Carga publicaciones externas y prepara las operaciones del formulario. */
class PostViewModel(
    private val repository: PostRepository = PostRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostUiState())
    val uiState = _uiState.asStateFlow()

    init {
        cargarPublicaciones()
    }

    // Carga de nuevo la lista y muestra un mensaje si el servicio no responde.
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
                        estado.publicaciones.filterNot { it.id == creada.id },
                    idsCreadosConPost = estado.idsCreadosConPost + creada.id
                )
            }
        }
    }

    fun crearEvento(
        idEvento: String,
        nombreEvento: String,
        fecha: String,
        ubicacion: String,
        organizacion: String
    ) {
        // Revisa los datos antes de enviarlos al servicio de prueba.
        val id = idEvento.trim().toIntOrNull()
        val errores = EventoPostErrores(
            idEvento = if (id == null || id <= 0) {
                "Ingresa un ID de evento entero mayor que cero."
            } else {
                null
            },
            nombreEvento = when {
                nombreEvento.isBlank() -> "El nombre del evento es obligatorio."
                nombreEvento.trim().length < 3 -> "Ingresa al menos 3 caracteres."
                nombreEvento.trim().length > 100 ->
                    "El nombre no puede superar 100 caracteres."
                else -> null
            },
            fecha = if (esFechaIsoValida(fecha.trim())) {
                null
            } else {
                "Usa una fecha real con formato AAAA-MM-DD."
            },
            ubicacion = validarTextoEvento(ubicacion, "la comuna o ubicación"),
            organizacion = validarTextoEvento(organizacion, "la organización")
        )
        if (errores.hayErrores) {
            _uiState.update {
                it.copy(
                    erroresEvento = errores,
                    mensajeMutacion = "Corrige los campos marcados antes de enviar el evento.",
                    mutacionFallida = true
                )
            }
            return
        }

        val nombre = nombreEvento.trim()
        val fechaLimpia = fecha.trim()
        val ubicacionLimpia = ubicacion.trim()
        val organizacionLimpia = organizacion.trim()
        val solicitud = PostRequest(
            userId = 1,
            title = nombre,
            body = "Fecha: $fechaLimpia · Ubicación: $ubicacionLimpia · " +
                "Organización: $organizacionLimpia",
            eventId = id,
            eventName = nombre,
            date = fechaLimpia,
            location = ubicacionLimpia,
            organization = organizacionLimpia
        )
        ejecutarMutacion("POST exitoso. JSONPlaceholder no conserva los cambios.") {
            val creado = repository.crearPublicacion(solicitud)
            _uiState.update { estado ->
                estado.copy(
                    publicaciones = listOf(creado) +
                        estado.publicaciones.filterNot { it.id == creado.id },
                    idsCreadosConPost = estado.idsCreadosConPost + creado.id
                )
            }
        }
    }

    fun limpiarErrorEvento(campo: CampoEventoPost) {
        _uiState.update { state ->
            val errores = when (campo) {
                CampoEventoPost.ID -> state.erroresEvento.copy(idEvento = null)
                CampoEventoPost.NOMBRE -> state.erroresEvento.copy(nombreEvento = null)
                CampoEventoPost.FECHA -> state.erroresEvento.copy(fecha = null)
                CampoEventoPost.UBICACION -> state.erroresEvento.copy(ubicacion = null)
                CampoEventoPost.ORGANIZACION ->
                    state.erroresEvento.copy(organizacion = null)
            }
            state.copy(
                erroresEvento = errores,
                mensajeMutacion = if (errores.hayErrores) state.mensajeMutacion else null,
                mutacionFallida = errores.hayErrores
            )
        }
    }

    fun limpiarErrorPublicacion(campo: CampoPublicacion) {
        _uiState.update { state ->
            if (!state.erroresPublicacion.hayErrores) {
                state
            } else {
                val errores = when (campo) {
                    CampoPublicacion.TITULO ->
                        state.erroresPublicacion.copy(titulo = null)
                    CampoPublicacion.CONTENIDO ->
                        state.erroresPublicacion.copy(contenido = null)
                }
                state.copy(
                    erroresPublicacion = errores,
                    mensajeMutacion = if (errores.hayErrores) state.mensajeMutacion else null,
                    mutacionFallida = errores.hayErrores
                )
            }
        }
    }

    fun actualizarPublicacion(publicacion: PostResponse, titulo: String, contenido: String) {
        val solicitud = crearSolicitud(titulo, contenido, publicacion.userId) ?: return
        val creadaConPost = publicacion.id in _uiState.value.idsCreadosConPost
        val mensajeExito = if (creadaConPost) {
            "Cambios aplicados. JSONPlaceholder no conserva los cambios."
        } else {
            "PUT exitoso. JSONPlaceholder no conserva los cambios."
        }
        ejecutarMutacion(mensajeExito) {
            // Los registros creados con POST solo se pueden cambiar en esta pantalla.
            val actualizada = if (creadaConPost) {
                publicacion.copy(
                    title = solicitud.title,
                    body = solicitud.body,
                    eventName = publicacion.eventName?.let { solicitud.title }
                )
            } else {
                val respuesta = repository.actualizarPublicacion(publicacion.id, solicitud)
                respuesta.copy(
                    eventId = respuesta.eventId ?: publicacion.eventId,
                    eventName = publicacion.eventName?.let { titulo.trim() }
                        ?: respuesta.eventName,
                    date = respuesta.date ?: publicacion.date,
                    location = respuesta.location ?: publicacion.location,
                    organization = respuesta.organization ?: publicacion.organization
                )
            }
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
        val errores = PublicacionErrores(
            titulo = when {
                titulo.isBlank() -> "El título es obligatorio."
                titulo.trim().length < 3 -> "El título debe tener al menos 3 caracteres."
                titulo.trim().length > 100 -> "El título no puede superar 100 caracteres."
                else -> null
            },
            contenido = when {
                contenido.isBlank() -> "El contenido es obligatorio."
                contenido.trim().length > 5000 ->
                    "El contenido no puede superar 5.000 caracteres."
                else -> null
            }
        )
        if (errores.hayErrores) {
            val mensaje = if (titulo.isBlank() || contenido.isBlank()) {
                "El título y el contenido son obligatorios."
            } else {
                "Corrige los campos marcados antes de guardar."
            }
            _uiState.update {
                it.copy(
                    erroresPublicacion = errores,
                    mensajeMutacion = mensaje,
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
                    mutacionFallida = false,
                    erroresEvento = EventoPostErrores(),
                    erroresPublicacion = PublicacionErrores()
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

    private fun esFechaIsoValida(valor: String): Boolean {
        if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(valor)) return false
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false
        }
        val posicion = ParsePosition(0)
        val fecha = formato.parse(valor, posicion)
        return fecha != null && posicion.index == valor.length
    }

    private fun validarTextoEvento(valor: String, campo: String): String? =
        when {
            valor.isBlank() -> "Completa $campo."
            valor.trim().length < 2 -> "Ingresa $campo en forma válida."
            valor.trim().length > 150 -> "$campo no puede superar 150 caracteres."
            else -> null
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
