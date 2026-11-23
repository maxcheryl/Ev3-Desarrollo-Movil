package cl.duocuc.evaluacion2.validation

import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.TipoGestion
import cl.duocuc.evaluacion2.ui.viewmodels.GestionErrores
import cl.duocuc.evaluacion2.ui.viewmodels.GestionFormState

object GestionValidator {

    fun validar(form: GestionFormState): GestionErrores {
        return GestionErrores(
            tipo = if (esTipoValido(form.tipo)) {
                null
            } else {
                "Selecciona el tipo de gestión."
            },
            estado = if (esEstadoValido(form.estado)) {
                null
            } else {
                "Selecciona el estado resultante."
            },
            descripcion = if (form.descripcion.isBlank()) {
                "La descripción es obligatoria."
            } else {
                null
            }
        )
    }

    fun esTipoValido(valor: String): Boolean =
        runCatching { TipoGestion.valueOf(valor) }.isSuccess

    fun esEstadoValido(valor: String): Boolean =
        runCatching { EstadoGestion.valueOf(valor) }.isSuccess
}
