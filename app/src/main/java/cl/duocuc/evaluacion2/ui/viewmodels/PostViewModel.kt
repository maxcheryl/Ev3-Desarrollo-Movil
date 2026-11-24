package cl.duocuc.evaluacion2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duocuc.evaluacion2.data.model.PostResponse
import cl.duocuc.evaluacion2.data.repository.PostRepository
import cl.duocuc.evaluacion2.data.repository.RespuestaApiVaciaException
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class PostUiState(
    val cargando: Boolean = true,
    val publicaciones: List<PostResponse> = emptyList(),
    val mensajeError: String? = null
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
