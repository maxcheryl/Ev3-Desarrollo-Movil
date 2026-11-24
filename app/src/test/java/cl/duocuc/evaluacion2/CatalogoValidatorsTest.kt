package cl.duocuc.evaluacion2

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

import cl.duocuc.evaluacion2.data.model.TipoOrganizacion
import cl.duocuc.evaluacion2.ui.viewmodels.ContactoFormState
import cl.duocuc.evaluacion2.ui.viewmodels.EventoFormState
import cl.duocuc.evaluacion2.ui.viewmodels.OrganizacionFormState
import cl.duocuc.evaluacion2.validation.ContactoValidator
import cl.duocuc.evaluacion2.validation.EventoValidator
import cl.duocuc.evaluacion2.validation.OrganizacionValidator

class CatalogoValidatorsTest : StringSpec({
    "Organizacion valida y campos invalidos" { CatalogoValidatorsTestCases().organizacionValidaYCamposInvalidos() }
    "Contacto valida correo telefono y organizacion" { CatalogoValidatorsTestCases().contactoValidaCorreoTelefonoYOrganizacion() }
    "Evento exige fecha comuna y organizacion" { CatalogoValidatorsTestCases().eventoExigeFechaComunaYOrganizacion() }
})

private class CatalogoValidatorsTestCases {

    fun organizacionValidaYCamposInvalidos() {
        val valid = OrganizacionFormState(
            nombre = "Agencia Max",
            tipo = TipoOrganizacion.values().first().name,
            comuna = "Santiago"
        )
        (OrganizacionValidator.validar(valid).hayErrores) shouldBe false

        val invalid = OrganizacionValidator.validar(
            valid.copy(nombre = " A ", tipo = "NO_EXISTE", comuna = "")
        )
        (invalid.nombre) shouldNotBe null
        (invalid.tipo) shouldNotBe null
        (invalid.comuna) shouldNotBe null
    }

    fun contactoValidaCorreoTelefonoYOrganizacion() {
        val valid = ContactoFormState(
            nombre = "Ana Pérez",
            organizacionId = 2L,
            cargo = "Gerenta",
            correo = "ana@example.com",
            telefono = "56912345678"
        )
        (ContactoValidator.validar(valid).hayErrores) shouldBe false

        val invalid = ContactoValidator.validar(
            valid.copy(
                nombre = "A",
                organizacionId = null,
                cargo = "",
                correo = "correo-invalido",
                telefono = "123"
            )
        )
        (invalid.nombre) shouldNotBe null
        (invalid.organizacionId) shouldNotBe null
        (invalid.cargo) shouldNotBe null
        (invalid.correo) shouldNotBe null
        (invalid.telefono) shouldNotBe null
    }

    fun eventoExigeFechaComunaYOrganizacion() {
        val valid = EventoFormState(
            nombre = "Feria de bienestar",
            fecha = 1L,
            comuna = "Santiago",
            organizacionId = 3L
        )
        (EventoValidator.validar(valid).hayErrores) shouldBe false

        val invalid = EventoValidator.validar(
            valid.copy(nombre = "X", fecha = null, comuna = "", organizacionId = null)
        )
        (invalid.hayErrores) shouldBe true
        (invalid.nombre) shouldNotBe null
        (invalid.fecha) shouldNotBe null
        (invalid.comuna) shouldNotBe null
        (invalid.organizacionId) shouldNotBe null
    }
}
