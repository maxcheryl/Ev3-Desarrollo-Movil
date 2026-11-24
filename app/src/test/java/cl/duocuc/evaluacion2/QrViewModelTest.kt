package cl.duocuc.evaluacion2

import cl.duocuc.evaluacion2.ui.viewmodels.QrViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrViewModelTest {

    @Test
    fun aceptaCodigoMaxYrechazaCodigoExterno() {
        val viewModel = QrViewModel()

        viewModel.onQrDetectado("MAX:OP-002")
        assertTrue(viewModel.uiState.value.esValido)
        assertFalse(viewModel.uiState.value.escaneando)
        assertEquals("MAX:OP-002", viewModel.uiState.value.contenido)

        viewModel.onQrDetectado("https://example.com/OP-002")
        assertFalse(viewModel.uiState.value.esValido)
        assertTrue(viewModel.uiState.value.mensaje.orEmpty().isNotBlank())
    }

    @Test
    fun informaPermisoDenegadoYSinCoincidenciaYPermiteReiniciar() {
        val viewModel = QrViewModel()
        viewModel.onPermisoDenegado()
        assertFalse(viewModel.uiState.value.escaneando)
        assertTrue(viewModel.uiState.value.mensaje.orEmpty().contains("Permiso"))

        viewModel.onQrDetectado("MAX:OP-404")
        viewModel.onSinCoincidencia()
        assertTrue(viewModel.uiState.value.esValido)
        assertTrue(viewModel.uiState.value.mensaje.orEmpty().contains("no corresponde"))

        viewModel.reiniciarEscaneo()
        assertTrue(viewModel.uiState.value.escaneando)
        assertEquals(null, viewModel.uiState.value.contenido)
        assertEquals(null, viewModel.uiState.value.mensaje)
    }
}
