package cl.duocuc.evaluacion2

import cl.duocuc.evaluacion2.data.model.TipoOrganizacion
import cl.duocuc.evaluacion2.ui.viewmodels.ContactoFormState
import cl.duocuc.evaluacion2.ui.viewmodels.EventoFormState
import cl.duocuc.evaluacion2.ui.viewmodels.OrganizacionFormState
import cl.duocuc.evaluacion2.validation.ContactoValidator
import cl.duocuc.evaluacion2.validation.EventoValidator
import cl.duocuc.evaluacion2.validation.OrganizacionValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogoValidatorsTest {

    @Test
    fun organizacionValidaYCamposInvalidos() {
        val valid = OrganizacionFormState(
            nombre = "Agencia Max",
            tipo = TipoOrganizacion.values().first().name,
            comuna = "Santiago"
        )
        assertFalse(OrganizacionValidator.validar(valid).hayErrores)

        val invalid = OrganizacionValidator.validar(
            valid.copy(nombre = " A ", tipo = "NO_EXISTE", comuna = "")
        )
        assertNotNull(invalid.nombre)
        assertNotNull(invalid.tipo)
        assertNotNull(invalid.comuna)
    }

    @Test
    fun contactoValidaCorreoTelefonoYOrganizacion() {
        val valid = ContactoFormState(
            nombre = "Ana Pérez",
            organizacionId = 2L,
            cargo = "Gerenta",
            correo = "ana@example.com",
            telefono = "56912345678"
        )
        assertFalse(ContactoValidator.validar(valid).hayErrores)

        val invalid = ContactoValidator.validar(
            valid.copy(
                nombre = "A",
                organizacionId = null,
                cargo = "",
                correo = "correo-invalido",
                telefono = "123"
            )
        )
        assertNotNull(invalid.nombre)
        assertNotNull(invalid.organizacionId)
        assertNotNull(invalid.cargo)
        assertNotNull(invalid.correo)
        assertNotNull(invalid.telefono)
    }

    @Test
    fun eventoExigeFechaComunaYOrganizacion() {
        val valid = EventoFormState(
            nombre = "Feria de bienestar",
            fecha = 1L,
            comuna = "Santiago",
            organizacionId = 3L
        )
        assertFalse(EventoValidator.validar(valid).hayErrores)

        val invalid = EventoValidator.validar(
            valid.copy(nombre = "X", fecha = null, comuna = "", organizacionId = null)
        )
        assertTrue(invalid.hayErrores)
        assertNotNull(invalid.nombre)
        assertNotNull(invalid.fecha)
        assertNotNull(invalid.comuna)
        assertNotNull(invalid.organizacionId)
    }
}
