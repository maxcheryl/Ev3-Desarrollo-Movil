package cl.duocuc.evaluacion2

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.TipoGestion
import cl.duocuc.evaluacion2.ui.viewmodels.GestionFormState
import cl.duocuc.evaluacion2.validation.GestionValidator

class GestionValidatorTest : StringSpec({
    "Formulario valido no tiene errores" { GestionValidatorTestCases().formularioValidoNoTieneErrores() }
    "Campos obligatorios vacios reportan errores" { GestionValidatorTestCases().camposObligatoriosVaciosReportanErrores() }
    "Tipo y estado invalidos son rechazados" { GestionValidatorTestCases().tipoYEstadoInvalidosSonRechazados() }
    "Proxima accion y fecha son opcionales" { GestionValidatorTestCases().proximaAccionYFechaSonOpcionales() }
})

private class GestionValidatorTestCases {

    private fun formularioValido() = GestionFormState(
        tipo = TipoGestion.CONTACTO.name,
        estado = EstadoGestion.CONTACTO_REALIZADO.name,
        descripcion = "Llamada con la organización para coordinar la propuesta."
    )

    fun formularioValidoNoTieneErrores() {
        val errores = GestionValidator.validar(formularioValido())
        (errores.hayErrores) shouldBe false
    }

    fun camposObligatoriosVaciosReportanErrores() {
        val errores = GestionValidator.validar(GestionFormState())

        (errores.tipo) shouldNotBe null
        (errores.estado) shouldNotBe null
        (errores.descripcion) shouldNotBe null
        (errores.hayErrores) shouldBe true
    }

    fun tipoYEstadoInvalidosSonRechazados() {
        val errores = GestionValidator.validar(
            formularioValido().copy(
                tipo = "Tipo inventado",
                estado = "Estado inventado"
            )
        )

        (errores.tipo) shouldNotBe null
        (errores.estado) shouldNotBe null
        (errores.hayErrores) shouldBe true
    }

    fun proximaAccionYFechaSonOpcionales() {
        val errores = GestionValidator.validar(
            formularioValido().copy(
                proximaAccion = "",
                fechaProximaAccion = null,
                resultado = ""
            )
        )

        (errores.hayErrores) shouldBe false
    }
}
