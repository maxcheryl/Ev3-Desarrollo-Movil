package cl.duocuc.evaluacion2

import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.TipoGestion
import cl.duocuc.evaluacion2.ui.viewmodels.GestionFormState
import cl.duocuc.evaluacion2.validation.GestionValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestionValidatorTest {

    private fun formularioValido() = GestionFormState(
        tipo = TipoGestion.CONTACTO.name,
        estado = EstadoGestion.CONTACTO_REALIZADO.name,
        descripcion = "Llamada con la organización para coordinar la propuesta."
    )

    @Test
    fun formularioValidoNoTieneErrores() {
        val errores = GestionValidator.validar(formularioValido())
        assertFalse(errores.hayErrores)
    }

    @Test
    fun camposObligatoriosVaciosReportanErrores() {
        val errores = GestionValidator.validar(GestionFormState())

        assertNotNull(errores.tipo)
        assertNotNull(errores.estado)
        assertNotNull(errores.descripcion)
        assertTrue(errores.hayErrores)
    }

    @Test
    fun tipoYEstadoInvalidosSonRechazados() {
        val errores = GestionValidator.validar(
            formularioValido().copy(
                tipo = "Tipo inventado",
                estado = "Estado inventado"
            )
        )

        assertNotNull(errores.tipo)
        assertNotNull(errores.estado)
        assertTrue(errores.hayErrores)
    }

    @Test
    fun proximaAccionYFechaSonOpcionales() {
        val errores = GestionValidator.validar(
            formularioValido().copy(
                proximaAccion = "",
                fechaProximaAccion = null,
                resultado = ""
            )
        )

        assertFalse(errores.hayErrores)
    }
}
