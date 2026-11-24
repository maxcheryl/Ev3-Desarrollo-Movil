package cl.duocuc.evaluacion2.ui.viewmodels

import androidx.lifecycle.ViewModel
import cl.duocuc.evaluacion2.qr.QrMaxValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Datos que la pantalla necesita para mostrar el resultado del escaneo. */
data class EscanearQrUiState(
    val escaneando: Boolean = true,
    val contenido: String? = null,
    val esValido: Boolean = false,
    val mensaje: String? = null
)

/**
 * Revisa el código detectado y decide si muestra la oportunidad o un aviso.
 */
class QrViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EscanearQrUiState())
    val uiState: StateFlow<EscanearQrUiState> = _uiState.asStateFlow()

    /**
     * Revisa el primer código leído y guarda el resultado para la pantalla.
     */
    fun onQrDetectado(contenido: String) {
        val valido = QrMaxValidator.esValido(contenido)
        _uiState.value = EscanearQrUiState(
            escaneando = false,
            contenido = contenido,
            esValido = valido,
            mensaje = if (valido) {
                null
            } else {
                "El código QR escaneado no pertenece a Agencia Atenas."
            }
        )
    }

    fun onPermisoDenegado() {
        _uiState.value = EscanearQrUiState(
            escaneando = false,
            mensaje = "Permiso de cámara denegado. No es posible escanear códigos QR."
        )
    }

    fun onSinCoincidencia() {
        _uiState.update {
            it.copy(
                mensaje = "El código es válido, pero no corresponde a ninguna oportunidad registrada."
            )
        }
    }

    /** Borra el resultado anterior y permite escanear otra vez. */
    fun reiniciarEscaneo() {
        _uiState.value = EscanearQrUiState()
    }
}
