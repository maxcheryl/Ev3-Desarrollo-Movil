package cl.duocuc.evaluacion2

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.PotencialComercial
import cl.duocuc.evaluacion2.data.model.Prioridad
import cl.duocuc.evaluacion2.data.model.TipoOportunidad
import cl.duocuc.evaluacion2.ui.viewmodels.FuentesOportunidad
import cl.duocuc.evaluacion2.ui.viewmodels.OportunidadFormState
import cl.duocuc.evaluacion2.validation.OportunidadValidator

class OportunidadValidatorTest : StringSpec({
    "Formulario valido no tiene errores" { OportunidadValidatorTestCases().formularioValidoNoTieneErrores() }
    "Campos obligatorios vacios reportan errores" { OportunidadValidatorTestCases().camposObligatoriosVaciosReportanErrores() }
    "Nombre muy corto es rechazado" { OportunidadValidatorTestCases().nombreMuyCortoEsRechazado() }
    "Opciones de seleccion invalidas son rechazadas" { OportunidadValidatorTestCases().opcionesDeSeleccionInvalidasSonRechazadas() }
    "Observaciones es opcional" { OportunidadValidatorTestCases().observacionesEsOpcional() }
    "Nombre con espacios es recortado al validar" { OportunidadValidatorTestCases().nombreConEspaciosEsRecortadoAlValidar() }
})

private class OportunidadValidatorTestCases {

    private fun formularioValido() = OportunidadFormState(
        nombre = "Feria de bienestar 2026",
        tipo = TipoOportunidad.FERIA_EMPRESARIAL.name,
        descripcion = "Feria corporativa con beneficios para colaboradores.",
        fuente = FuentesOportunidad.opciones.first(),
        prioridad = Prioridad.ALTA.name,
        potencialComercial = PotencialComercial.ALTO.name,
        estado = EstadoGestion.EVENTO_IDENTIFICADO.name,
        observaciones = ""
    )

    fun formularioValidoNoTieneErrores() {
        val errores = OportunidadValidator.validar(formularioValido())
        (errores.hayErrores) shouldBe false
    }

    fun camposObligatoriosVaciosReportanErrores() {
        val errores = OportunidadValidator.validar(OportunidadFormState())

        (errores.nombre) shouldNotBe null
        (errores.tipo) shouldNotBe null
        (errores.descripcion) shouldNotBe null
        (errores.fuente) shouldNotBe null
        (errores.prioridad) shouldNotBe null
        (errores.potencialComercial) shouldNotBe null
        // El estado inicia en «Evento identificado», por lo que ya es válido
        (errores.estado) shouldBe null
        (errores.hayErrores) shouldBe true
    }

    fun nombreMuyCortoEsRechazado() {
        val errores = OportunidadValidator.validar(formularioValido().copy(nombre = "ab"))
        (errores.nombre) shouldNotBe null
    }

    fun opcionesDeSeleccionInvalidasSonRechazadas() {
        val errores = OportunidadValidator.validar(
            formularioValido().copy(
                tipo = "No es un tipo",
                fuente = "Tienda no registrada",
                prioridad = "Urgente",
                potencialComercial = "Gigantesco",
                estado = "Inventado"
            )
        )

        (errores.tipo) shouldNotBe null
        (errores.fuente) shouldNotBe null
        (errores.prioridad) shouldNotBe null
        (errores.potencialComercial) shouldNotBe null
        (errores.estado) shouldNotBe null
    }

    fun observacionesEsOpcional() {
        val sinObservaciones = OportunidadValidator.validar(formularioValido())
        val conObservaciones = OportunidadValidator.validar(
            formularioValido().copy(observaciones = "Confirmar asistencia del área de marketing.")
        )

        (sinObservaciones.hayErrores) shouldBe false
        (conObservaciones.hayErrores) shouldBe false
    }

    fun nombreConEspaciosEsRecortadoAlValidar() {
        val errores = OportunidadValidator.validar(formularioValido().copy(nombre = "   "))
        (errores.nombre) shouldNotBe null
    }
}
