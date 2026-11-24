package cl.duocuc.evaluacion2

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.TipoAccionSeguimiento
import cl.duocuc.evaluacion2.ui.viewmodels.GestionFormState
import cl.duocuc.evaluacion2.validation.GestionValidator

class GestionValidatorTest : StringSpec({
    "Formulario valido no tiene errores" { GestionValidatorTestCases().formularioValidoNoTieneErrores() }
    "Estado resultante es opcional" { GestionValidatorTestCases().estadoResultanteEsOpcional() }
    "Tipo y estado invalidos son rechazados" { GestionValidatorTestCases().tipoYEstadoInvalidosSonRechazados() }
    "Proxima accion y fecha son opcionales" { GestionValidatorTestCases().proximaAccionYFechaSonOpcionales() }
    "Proxima accion y fecha deben ser coherentes" { GestionValidatorTestCases().proximaAccionDebeTenerFechaPosterior() }
    "Fecha de gestion futura se rechaza" { GestionValidatorTestCases().fechaDeGestionFuturaSeRechaza() }
})

private class GestionValidatorTestCases {

    private fun formularioValido() = GestionFormState(
        tipo = TipoAccionSeguimiento.PRIMER_CONTACTO.name,
        estado = EstadoGestion.CONTACTO_REALIZADO.name,
        resultado = "Llamada con la organización para coordinar la propuesta."
    )

    fun formularioValidoNoTieneErrores() {
        val errores = GestionValidator.validar(formularioValido())
        (errores.hayErrores) shouldBe false
    }

    fun estadoResultanteEsOpcional() {
        val errores = GestionValidator.validar(GestionFormState())

        (errores.tipo) shouldNotBe null
        (errores.estado) shouldBe null
        (errores.descripcion) shouldNotBe null
        (errores.hayErrores) shouldBe true

        (GestionValidator.validar(formularioValido().copy(estado = "")).hayErrores) shouldBe false
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
                fechaProximaAccion = null
            )
        )
        (errores.hayErrores) shouldBe false
    }

    fun proximaAccionDebeTenerFechaPosterior() {
        val fecha = formularioValido().fechaGestion ?: 0L
        val sinFecha = GestionValidator.validar(
            formularioValido().copy(proximaAccion = "Enviar propuesta", fechaProximaAccion = null)
        )
        (sinFecha.proximaAccion) shouldNotBe null

        // La próxima gestión tampoco puede quedar antes de la actual.
        val fechaAnterior = GestionValidator.validar(
            formularioValido().copy(
                proximaAccion = "Enviar propuesta",
                fechaProximaAccion = fecha - 86_400_000L
            )
        )
        (fechaAnterior.fechaProximaAccion) shouldNotBe null
    }

    fun fechaDeGestionFuturaSeRechaza() {
        val errores = GestionValidator.validar(
            formularioValido().copy(
                fechaGestion = System.currentTimeMillis() + 86_400_000L,
                horaGestion = "09:00"
            )
        )
        (errores.fechaGestion) shouldNotBe null
    }
}
