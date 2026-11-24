package cl.duocuc.evaluacion2

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import cl.duocuc.evaluacion2.ui.viewmodels.QrViewModel

class QrViewModelTest : StringSpec({
    "Acepta codigo max y rechaza codigo externo" { QrViewModelTestCases().aceptaCodigoMaxYrechazaCodigoExterno() }
    "Informa permiso denegado y sin coincidencia y permite reiniciar" { QrViewModelTestCases().informaPermisoDenegadoYSinCoincidenciaYPermiteReiniciar() }
})

private class QrViewModelTestCases {

    fun aceptaCodigoMaxYrechazaCodigoExterno() {
        val viewModel = QrViewModel()

        viewModel.onQrDetectado("MAX:OP-002")
        (viewModel.uiState.value.esValido) shouldBe true
        (viewModel.uiState.value.escaneando) shouldBe false
        (viewModel.uiState.value.contenido) shouldBe ("MAX:OP-002")

        viewModel.onQrDetectado("https://example.com/OP-002")
        (viewModel.uiState.value.esValido) shouldBe false
        (viewModel.uiState.value.mensaje.orEmpty().isNotBlank()) shouldBe true
    }

    fun informaPermisoDenegadoYSinCoincidenciaYPermiteReiniciar() {
        val viewModel = QrViewModel()
        viewModel.onPermisoDenegado()
        (viewModel.uiState.value.escaneando) shouldBe false
        (viewModel.uiState.value.mensaje.orEmpty().contains("Permiso")) shouldBe true

        viewModel.onQrDetectado("MAX:OP-404")
        viewModel.onSinCoincidencia()
        (viewModel.uiState.value.esValido) shouldBe true
        (viewModel.uiState.value.mensaje.orEmpty().contains("no corresponde")) shouldBe true

        viewModel.reiniciarEscaneo()
        (viewModel.uiState.value.escaneando) shouldBe true
        (viewModel.uiState.value.contenido) shouldBe (null)
        (viewModel.uiState.value.mensaje) shouldBe (null)
    }
}
