package cl.duocuc.evaluacion2.validation

import cl.duocuc.evaluacion2.data.model.TipoOrganizacion
import cl.duocuc.evaluacion2.ui.viewmodels.ContactoErrores
import cl.duocuc.evaluacion2.ui.viewmodels.ContactoFormState
import cl.duocuc.evaluacion2.ui.viewmodels.EventoErrores
import cl.duocuc.evaluacion2.ui.viewmodels.EventoFormState
import cl.duocuc.evaluacion2.data.model.TipoOportunidad
import cl.duocuc.evaluacion2.ui.viewmodels.OrganizacionErrores
import cl.duocuc.evaluacion2.ui.viewmodels.OrganizacionFormState

object OrganizacionValidator {

    fun validar(form: OrganizacionFormState): OrganizacionErrores {
        return OrganizacionErrores(
            nombre = when {
                form.nombre.isBlank() -> "El nombre es obligatorio."
                form.nombre.trim().length < 3 -> "Ingresa al menos 3 caracteres."
                else -> null
            },
            tipo = if (esTipoValido(form.tipo)) {
                null
            } else {
                "Selecciona un tipo de organización."
            },
            comuna = if (form.comuna.isBlank()) {
                "La comuna es obligatoria."
            } else {
                null
            }
        )
    }

    fun esTipoValido(valor: String): Boolean =
        runCatching { TipoOrganizacion.valueOf(valor) }.isSuccess
}

object ContactoValidator {

    private val patronCorreo = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun validar(form: ContactoFormState): ContactoErrores {
        return ContactoErrores(
            nombre = when {
                form.nombre.isBlank() -> "El nombre es obligatorio."
                form.nombre.trim().length < 3 -> "Ingresa al menos 3 caracteres."
                else -> null
            },
            organizacionId = if ((form.organizacionId ?: 0L) <= 0L) {
                "Selecciona una organización."
            } else {
                null
            },
            cargo = if (form.cargo.isBlank()) {
                "El cargo es obligatorio."
            } else {
                null
            },
            correo = when {
                form.correo.isBlank() -> "El correo es obligatorio."
                !patronCorreo.matches(form.correo.trim()) -> "Ingresa un correo válido."
                else -> null
            },
            telefono = when {
                form.telefono.isBlank() -> "El teléfono es obligatorio."
                form.telefono.count { it.isDigit() } < 6 -> "Ingresa al menos 6 dígitos."
                else -> null
            }
        )
    }
}

object EventoValidator {

    fun validar(form: EventoFormState): EventoErrores {
        return EventoErrores(
            nombre = when {
                form.nombre.isBlank() -> "El nombre es obligatorio."
                form.nombre.trim().length < 3 -> "Ingresa al menos 3 caracteres."
                else -> null
            },
            tipo = if (TipoOportunidad.desdeNombre(form.tipo).name != form.tipo) {
                "Selecciona un tipo de evento."
            } else {
                null
            },
            fecha = if ((form.fecha ?: 0L) <= 0L) {
                "Selecciona la fecha del evento."
            } else {
                null
            },
            fechaTermino = when {
                form.fechaTermino == null -> "Selecciona la fecha de término."
                form.fecha != null && form.fechaTermino < form.fecha ->
                    "La fecha de término debe ser igual o posterior al inicio."
                else -> null
            },
            region = if (form.region.isBlank()) "La región es obligatoria." else null,
            comuna = if (form.comuna.isBlank()) {
                "La comuna es obligatoria."
            } else {
                null
            },
            organizacionId = if ((form.organizacionId ?: 0L) <= 0L) {
                "Selecciona una organización."
            } else {
                null
            },
            asistentesEstimados = form.asistentesEstimados.toIntOrNull().let { cantidad ->
                if (
                    form.asistentesEstimados.isNotBlank() &&
                    (cantidad == null || cantidad < 0)
                ) {
                    "Ingresa una cantidad igual o mayor que cero."
                } else {
                    null
                }
            },
            costoReferencial = if (
                form.costoReferencial.isNotBlank() &&
                (form.costoReferencial.replace(',', '.').toDoubleOrNull()?.let {
                    !it.isFinite() || it < 0
                } != false)
            ) {
                "Ingresa un costo igual o mayor que cero."
            } else {
                null
            }
        )
    }
}
