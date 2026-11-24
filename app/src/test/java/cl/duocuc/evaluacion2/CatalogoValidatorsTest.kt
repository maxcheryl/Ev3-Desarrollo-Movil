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
            comuna = "Santiago",
            sitioWeb = "https://agenciamax.cl"
        )
        (OrganizacionValidator.validar(valid).hayErrores) shouldBe false

        val invalid = OrganizacionValidator.validar(
            valid.copy(nombre = " A ", tipo = "NO_EXISTE", comuna = "")
        )
        (invalid.nombre) shouldNotBe null
        (invalid.tipo) shouldNotBe null
        (invalid.comuna) shouldNotBe null

        val invalidOptional = OrganizacionValidator.validar(
            valid.copy(sitioWeb = "no es una web", observaciones = "x".repeat(2001))
        )
        (invalidOptional.sitioWeb) shouldNotBe null
        (invalidOptional.observaciones) shouldNotBe null
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

        val invalidOptional = ContactoValidator.validar(
            valid.copy(area = "x".repeat(101), observaciones = "x".repeat(2001))
        )
        (invalidOptional.area) shouldNotBe null
        (invalidOptional.observaciones) shouldNotBe null
    }

    fun eventoExigeFechaComunaYOrganizacion() {
        val valid = EventoFormState(
            nombre = "Feria de bienestar",
            fecha = 1L,
            fechaTermino = 2L,
            horaInicio = "09:00",
            horaTermino = "18:00",
            region = "Metropolitana",
            comuna = "Santiago",
            organizacionId = 3L
        )
        (EventoValidator.validar(valid).hayErrores) shouldBe false

        val invalid = EventoValidator.validar(
            valid.copy(
                nombre = "X",
                fecha = null,
                fechaTermino = null,
                region = "",
                comuna = "",
                organizacionId = null
            )
        )
        (invalid.hayErrores) shouldBe true
        (invalid.nombre) shouldNotBe null
        (invalid.fecha) shouldNotBe null
        (invalid.fechaTermino) shouldNotBe null
        (invalid.region) shouldNotBe null
        (invalid.comuna) shouldNotBe null
        (invalid.organizacionId) shouldNotBe null

        val cantidadesInvalidas = EventoValidator.validar(
            valid.copy(asistentesEstimados = "-1", costoReferencial = "-100")
        )
        (cantidadesInvalidas.asistentesEstimados) shouldNotBe null
        (cantidadesInvalidas.costoReferencial) shouldNotBe null

        val horarioInvalido = EventoValidator.validar(
            valid.copy(
                fecha = 10L,
                fechaTermino = 10L,
                horaInicio = "18:00",
                horaTermino = "18:00",
                direccion = "x".repeat(201),
                publicoEsperado = "x".repeat(151),
                publicoObjetivo = listOf("Audiencia inventada")
            )
        )
        (horarioInvalido.horaTermino) shouldNotBe null
        (horarioInvalido.direccion) shouldNotBe null
        (horarioInvalido.publicoEsperado) shouldNotBe null
        (horarioInvalido.publicoObjetivo) shouldNotBe null
    }
}
