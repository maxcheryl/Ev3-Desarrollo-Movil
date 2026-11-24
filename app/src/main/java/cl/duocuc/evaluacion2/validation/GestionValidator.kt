package cl.duocuc.evaluacion2.validation

import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.TipoAccionSeguimiento
import cl.duocuc.evaluacion2.data.model.TipoGestion
import cl.duocuc.evaluacion2.ui.viewmodels.GestionErrores
import cl.duocuc.evaluacion2.ui.viewmodels.GestionFormState
import java.util.Calendar
import java.util.TimeZone

object GestionValidator {

    private val patronHora = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

    fun validar(form: GestionFormState): GestionErrores {
        val horaGestionValida = patronHora.matches(form.horaGestion)
        val horaProximaValida = patronHora.matches(form.horaProximaAccion)
        val fechaSiguiente = form.fechaProximaAccion
        val fechaGestion = form.fechaGestion
        val gestionEnMillis = combinarFechaHora(fechaGestion, form.horaGestion)
        return GestionErrores(
            tipo = if (esTipoValido(form.tipo)) {
                null
            } else {
                "Selecciona el tipo de acción."
            },
            estado = if (form.estado.isBlank() || esEstadoValido(form.estado)) {
                null
            } else {
                "Selecciona un estado resultante válido."
            },
            descripcion = when {
                form.resultado.isBlank() ->
                    "El resultado de la gestión es obligatorio."
                form.resultado.trim().length < 3 ->
                    "Describe el resultado con al menos 3 caracteres."
                form.resultado.trim().length > 1000 ->
                    "El resultado no puede superar 1.000 caracteres."
                form.descripcion.trim().length > 2000 ->
                    "La descripción no puede superar 2.000 caracteres."
                else -> null
            },
            fechaGestion = when {
                fechaGestion == null || fechaGestion <= 0L ->
                    "Selecciona la fecha de la gestión."
                gestionEnMillis != null && gestionEnMillis > System.currentTimeMillis() ->
                    "La fecha y hora de la gestión no pueden ser futuras."
                else -> null
            },
            horaGestion = if (horaGestionValida) {
                null
            } else {
                "Selecciona una hora de gestión válida."
            },
            proximaAccion = when {
                form.proximaAccion.trim().length > 250 ->
                    "La próxima acción no puede superar 250 caracteres."
                form.proximaAccion.isNotBlank() && fechaSiguiente == null ->
                    "Selecciona la fecha del próximo seguimiento."
                fechaSiguiente != null && form.proximaAccion.isBlank() ->
                    "Describe la próxima acción o quita la fecha."
                else -> null
            },
            fechaProximaAccion = when {
                fechaSiguiente != null && fechaSiguiente <= 0L ->
                    "Selecciona una fecha válida para el próximo seguimiento."
                fechaSiguiente != null && fechaGestion != null &&
                    fechaSiguiente < fechaGestion ->
                    "El próximo seguimiento no puede ser anterior a la gestión."
                else -> null
            },
            horaProximaAccion = when {
                fechaSiguiente == null -> null
                !horaProximaValida -> "Selecciona una hora válida para el próximo seguimiento."
                fechaGestion != null && fechaSiguiente == fechaGestion &&
                    horaGestionValida && form.horaProximaAccion <= form.horaGestion ->
                    "El próximo seguimiento debe ser posterior a la gestión."
                else -> null
            },
            observaciones = if (form.observaciones.trim().length > 2000) {
                "Las observaciones no pueden superar 2.000 caracteres."
            } else {
                null
            }
        )
    }

    fun esTipoValido(valor: String): Boolean =
        TipoAccionSeguimiento.desdeNombre(valor) != null ||
            runCatching { TipoGestion.valueOf(valor) }.isSuccess

    fun esEstadoValido(valor: String): Boolean =
        runCatching { EstadoGestion.valueOf(valor) }
            .getOrNull()
            ?.let {
                it != EstadoGestion.ACTIVACION_REALIZADA &&
                    it != EstadoGestion.ACTIVACION_CANCELADA
            } == true

    private fun combinarFechaHora(fecha: Long?, hora: String): Long? {
        if (fecha == null || !patronHora.matches(hora)) return null
        val partes = hora.split(':')
        val fechaSeleccionada = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = fecha
        }
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, fechaSeleccionada.get(Calendar.YEAR))
            set(Calendar.MONTH, fechaSeleccionada.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, fechaSeleccionada.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, partes[0].toInt())
            set(Calendar.MINUTE, partes[1].toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
