package cl.duocuc.evaluacion2.ui.viewmodels

data class GestionFormState(
    val tipo: String = "",
    val estado: String = "",
    val descripcion: String = "",
    val resultado: String = "",
    val proximaAccion: String = "",
    val fechaProximaAccion: Long? = null
)

data class GestionErrores(
    val tipo: String? = null,
    val estado: String? = null,
    val descripcion: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(tipo, estado, descripcion).any { it != null }
}

data class GestionEditorUiState(
    val form: GestionFormState = GestionFormState(),
    val errores: GestionErrores = GestionErrores(),
    val guardando: Boolean = false,
    val mensajeError: String? = null
)
