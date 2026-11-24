package cl.duocuc.evaluacion2.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cl.duocuc.evaluacion2.location.ResultadoUbicacion
import cl.duocuc.evaluacion2.location.UbicacionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Datos que la pantalla muestra sobre la ubicación. */
data class UbicacionUiState(
    val obteniendo: Boolean = false,
    val resultado: ResultadoUbicacion? = null
)

/**
 * Pide la ubicación solo cuando el usuario lo solicita.
 * No la guarda ni sigue al dispositivo después de la consulta.
 */
class UbicacionViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UbicacionUiState())
    val uiState: StateFlow<UbicacionUiState> = _uiState.asStateFlow()

    fun obtenerUbicacion() {
        // No inicia otra consulta mientras la primera siga pendiente.
        if (_uiState.value.obteniendo) return
        _uiState.value = UbicacionUiState(obteniendo = true)

        UbicacionHelper.obtenerUbicacionUnaVez(getApplication()) { resultado ->
            _uiState.value = UbicacionUiState(obteniendo = false, resultado = resultado)
        }
    }

    fun onPermisoDenegado() {
        _uiState.value = UbicacionUiState(resultado = ResultadoUbicacion.PermisoDenegado)
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(UbicacionViewModel::class.java))
                    return UbicacionViewModel(application) as T
                }
            }
    }
}
