package cl.duocuc.evaluacion2.validation

import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.TipoAccionSeguimiento
import cl.duocuc.evaluacion2.data.model.TipoGestion
import cl.duocuc.evaluacion2.ui.viewmodels.GestionErrores
import cl.duocuc.evaluacion2.ui.viewmodels.GestionFormState

object GestionValidator {

    fun validar(form: GestionFormState): GestionErrores {
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
            descripcion = if (form.descripcion.isBlank() && form.resultado.isBlank()) {
                "El resultado de la gestión es obligatorio."
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
}
