package cl.duocuc.evaluacion2.validation

import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.PotencialComercial
import cl.duocuc.evaluacion2.data.model.Prioridad
import cl.duocuc.evaluacion2.data.model.TipoOportunidad
import cl.duocuc.evaluacion2.ui.viewmodels.FuentesOportunidad
import cl.duocuc.evaluacion2.ui.viewmodels.OportunidadErrores
import cl.duocuc.evaluacion2.ui.viewmodels.OportunidadFormState

/**
 * Revisa los datos del formulario antes de guardarlos.
 * Devuelve los errores que muestra la pantalla.
 */
object OportunidadValidator {

    fun validar(form: OportunidadFormState): OportunidadErrores {
        val fechaCreacion = form.fechaCreacion ?: 0L
        return OportunidadErrores(
            nombre = when {
                form.nombre.isBlank() -> "El nombre es obligatorio."
                form.nombre.trim().length < 3 -> "Ingresa al menos 3 caracteres."
                form.nombre.trim().length > 100 -> "El nombre no puede superar 100 caracteres."
                else -> null
            },
            tipo = if (esTipoValido(form.tipo)) {
                null
            } else {
                "Selecciona un tipo de oportunidad."
            },
            descripcion = when {
                form.descripcion.isBlank() -> "La descripción es obligatoria."
                form.descripcion.trim().length > 2000 ->
                    "La descripción no puede superar 2.000 caracteres."
                else -> null
            },
            fuente = if (FuentesOportunidad.esValida(form.fuente)) {
                null
            } else {
                "Selecciona la fuente u origen."
            },
            prioridad = if (esPrioridadValida(form.prioridad)) {
                null
            } else {
                "Selecciona una prioridad."
            },
            potencialComercial = if (esPotencialValido(form.potencialComercial)) {
                null
            } else {
                "Selecciona el potencial comercial."
            },
            estado = if (esEstadoValido(form.estado)) {
                null
            } else {
                "Selecciona un estado de gestión."
            },
            fechaIdentificacion = if (fechaCreacion <= 0L) {
                "Selecciona la fecha de identificación."
            } else if (fechaCreacion > System.currentTimeMillis()) {
                "La fecha de identificación no puede ser futura."
            } else {
                null
            },
            comuna = when {
                form.comuna.isBlank() -> "La comuna es obligatoria."
                form.comuna.trim().length < 2 -> "Ingresa una comuna válida."
                form.comuna.trim().length > 100 ->
                    "La comuna no puede superar 100 caracteres."
                else -> null
            },
            publicoEsperado = if (form.publicoEsperado.trim().length > 150) {
                "El público esperado no puede superar 150 caracteres."
            } else {
                null
            },
            observaciones = if (form.observaciones.trim().length > 2000) {
                "Las observaciones no pueden superar 2.000 caracteres."
            } else {
                null
            },
            organizacionId = if (form.organizacionId != null && form.organizacionId <= 0L) {
                "Selecciona una organización válida."
            } else {
                null
            },
            contactoId = if (form.contactoId != null && form.contactoId <= 0L) {
                "Selecciona un contacto válido."
            } else {
                null
            },
            eventoId = if (form.eventoId != null && form.eventoId <= 0L) {
                "Selecciona un evento válido."
            } else {
                null
            }
        )
    }

    // Si el texto no coincide con una opción, se considera inválido.
    fun esTipoValido(valor: String): Boolean =
        runCatching { TipoOportunidad.valueOf(valor) }.isSuccess

    fun esPrioridadValida(valor: String): Boolean =
        runCatching { Prioridad.valueOf(valor) }.isSuccess

    fun esPotencialValido(valor: String): Boolean =
        runCatching { PotencialComercial.valueOf(valor) }.isSuccess

    fun esEstadoValido(valor: String): Boolean =
        runCatching { EstadoGestion.valueOf(valor) }.isSuccess
}
