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

    private val patronSitioWeb = Regex(
        "^(?:https?://)?(?:www\\.)?(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}" +
            "[A-Za-z0-9])?\\.)+[A-Za-z]{2,}(?::\\d{1,5})?(?:[/?#][^\\s]*)?$",
        RegexOption.IGNORE_CASE
    )

    fun validar(form: OrganizacionFormState): OrganizacionErrores {
        return OrganizacionErrores(
            nombre = when {
                form.nombre.isBlank() -> "El nombre es obligatorio."
                form.nombre.trim().length < 3 -> "Ingresa al menos 3 caracteres."
                form.nombre.trim().length > 100 -> "El nombre no puede superar 100 caracteres."
                else -> null
            },
            tipo = if (esTipoValido(form.tipo)) {
                null
            } else {
                "Selecciona un tipo de organización."
            },
            comuna = validarTextoRequerido(form.comuna, "la comuna", 100),
            sitioWeb = when {
                form.sitioWeb.isBlank() -> null
                form.sitioWeb.trim().length > 2048 ->
                    "El sitio web no puede superar 2.048 caracteres."
                !patronSitioWeb.matches(form.sitioWeb.trim()) ->
                    "Ingresa una dirección web válida."
                else -> null
            },
            observaciones = validarLongitudOpcional(form.observaciones, "Las observaciones")
        )
    }

    fun esTipoValido(valor: String): Boolean =
        runCatching { TipoOrganizacion.valueOf(valor) }.isSuccess
}

object ContactoValidator {

    private val patronCorreo = Regex(
        "^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+" +
            "[A-Za-z]{2,}$"
    )
    private val patronTelefono = Regex("^\\+?[0-9() .-]+$")

    fun validar(form: ContactoFormState): ContactoErrores {
        return ContactoErrores(
            nombre = when {
                form.nombre.isBlank() -> "El nombre es obligatorio."
                form.nombre.trim().length < 3 -> "Ingresa al menos 3 caracteres."
                form.nombre.trim().length > 100 -> "El nombre no puede superar 100 caracteres."
                else -> null
            },
            organizacionId = if ((form.organizacionId ?: 0L) <= 0L) {
                "Selecciona una organización."
            } else {
                null
            },
            cargo = when {
                form.cargo.isBlank() -> "El cargo es obligatorio."
                form.cargo.trim().length < 2 -> "Ingresa un cargo válido."
                form.cargo.trim().length > 100 -> "El cargo no puede superar 100 caracteres."
                else -> null
            },
            correo = when {
                form.correo.isBlank() -> "El correo es obligatorio."
                form.correo.trim().length > 254 ->
                    "El correo no puede superar 254 caracteres."
                !patronCorreo.matches(form.correo.trim()) -> "Ingresa un correo válido."
                else -> null
            },
            telefono = when {
                form.telefono.isBlank() -> "El teléfono es obligatorio."
                !patronTelefono.matches(form.telefono.trim()) ->
                    "Usa solo números, espacios, paréntesis, +, guion o punto."
                form.telefono.count { it.isDigit() } !in 6..15 ->
                    "El teléfono debe contener entre 6 y 15 dígitos."
                else -> null
            },
            area = validarLongitudOpcional(form.area, "El área", 100),
            observaciones = validarLongitudOpcional(form.observaciones, "Las observaciones")
        )
    }
}

object EventoValidator {

    private val patronHora = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
    private val publicosObjetivoValidos = setOf(
        "Trabajadores",
        "Colaboradores",
        "Familias",
        "Asistentes",
        "Otro"
    )

    fun validar(form: EventoFormState): EventoErrores {
        val horaInicioValida = patronHora.matches(form.horaInicio)
        val horaTerminoValida = patronHora.matches(form.horaTermino)
        val mismoDia = form.fecha != null && form.fechaTermino == form.fecha
        val fechaInicio = form.fecha ?: 0L
        val fechaTermino = form.fechaTermino ?: 0L
        return EventoErrores(
            nombre = when {
                form.nombre.isBlank() -> "El nombre es obligatorio."
                form.nombre.trim().length < 3 -> "Ingresa al menos 3 caracteres."
                form.nombre.trim().length > 100 -> "El nombre no puede superar 100 caracteres."
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
                (form.fechaTermino ?: 0L) <= 0L -> "Selecciona la fecha de término."
                form.fecha != null && fechaTermino < fechaInicio ->
                    "La fecha de término debe ser igual o posterior al inicio."
                else -> null
            },
            horaInicio = if (horaInicioValida) null else "Selecciona una hora de inicio válida.",
            horaTermino = when {
                !horaTerminoValida -> "Selecciona una hora de término válida."
                mismoDia && horaInicioValida && form.horaTermino <= form.horaInicio ->
                    "La hora de término debe ser posterior al inicio."
                else -> null
            },
            direccion = validarTextoOpcional(form.direccion, "la dirección o ubicación", 200),
            region = validarTextoRequerido(form.region, "la región", 100),
            comuna = validarTextoRequerido(form.comuna, "la comuna", 100),
            organizacionId = if ((form.organizacionId ?: 0L) <= 0L) {
                "Selecciona una organización."
            } else {
                null
            },
            asistentesEstimados = validarCantidadOpcional(
                form.asistentesEstimados,
                "Ingresa una cantidad entera igual o mayor que cero."
            ),
            costoReferencial = validarDecimalOpcional(
                form.costoReferencial,
                "Ingresa un costo igual o mayor que cero."
            ),
            descripcion = validarLongitudOpcional(form.descripcion, "La descripción"),
            publicoEsperado = validarLongitudOpcional(
                form.publicoEsperado,
                "El público esperado",
                150
            ),
            publicoObjetivo = if (
                form.publicoObjetivo.any { it !in publicosObjetivoValidos }
            ) {
                "Selecciona opciones válidas para el público objetivo."
            } else {
                null
            },
            condicionesParticipacion = validarLongitudOpcional(
                form.condicionesParticipacion,
                "Las condiciones",
            ),
            requisitos = validarLongitudOpcional(form.requisitos, "Los requisitos"),
            espaciosDisponibles = validarLongitudOpcional(
                form.espaciosDisponibles,
                "Los espacios disponibles"
            )
        )
    }
}

private fun validarTextoRequerido(valor: String, campo: String, maximo: Int): String? =
    when {
        valor.isBlank() -> "Completa el campo $campo."
        valor.trim().length < 2 -> "Ingresa un valor válido para $campo."
        valor.trim().length > maximo -> "$campo no puede superar $maximo caracteres."
        else -> null
    }

private fun validarTextoOpcional(valor: String, campo: String, maximo: Int): String? =
    when {
        valor.isBlank() -> null
        valor.trim().length < 2 -> "Ingresa un valor válido para $campo."
        valor.trim().length > maximo -> "$campo no puede superar $maximo caracteres."
        else -> null
    }

private fun validarLongitudOpcional(
    valor: String,
    campo: String,
    maximo: Int = 2000
): String? = if (valor.trim().length > maximo) {
    "$campo ${if (campo.endsWith("s")) "no pueden" else "no puede"} " +
        "superar $maximo caracteres."
} else {
    null
}

private fun validarCantidadOpcional(valor: String, mensajeError: String): String? {
    if (valor.isBlank()) return null
    val cantidad = valor.trim().toIntOrNull()
    return if (cantidad == null || cantidad < 0) mensajeError else null
}

private fun validarDecimalOpcional(valor: String, mensajeError: String): String? {
    if (valor.isBlank()) return null
    val cantidad = valor.trim().replace(',', '.').toDoubleOrNull()
    return if (cantidad == null || !cantidad.isFinite() || cantidad < 0) mensajeError else null
}
