package cl.duocuc.evaluacion2.ui.viewmodels

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun fechaLocalEnUtc(): Long {
    val hoy = Calendar.getInstance()
    // Usa la misma hora que el selector para que el día no cambie.
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            hoy.get(Calendar.YEAR),
            hoy.get(Calendar.MONTH),
            hoy.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
    }.timeInMillis
}

private fun horaLocalActual(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

data class GestionFormState(
    val tipo: String = "",
    val estado: String = "",
    val descripcion: String = "",
    val fechaGestion: Long? = fechaLocalEnUtc(),
    val resultado: String = "",
    val proximaAccion: String = "",
    val fechaProximaAccion: Long? = null,
    val horaGestion: String = horaLocalActual(),
    val horaProximaAccion: String = "",
    val observaciones: String = ""
)

data class GestionErrores(
    val tipo: String? = null,
    val estado: String? = null,
    val descripcion: String? = null,
    val fechaGestion: String? = null,
    val horaGestion: String? = null,
    val proximaAccion: String? = null,
    val fechaProximaAccion: String? = null,
    val horaProximaAccion: String? = null,
    val observaciones: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(
            tipo,
            estado,
            descripcion,
            fechaGestion,
            horaGestion,
            proximaAccion,
            fechaProximaAccion,
            horaProximaAccion,
            observaciones
        ).any { it != null }
}

data class GestionEditorUiState(
    val form: GestionFormState = GestionFormState(),
    val errores: GestionErrores = GestionErrores(),
    val guardando: Boolean = false,
    val mensajeError: String? = null
)
