package cl.duocuc.evaluacion2.ui.viewmodels

data class OrganizacionFormState(
    val id: Long = 0,
    val nombre: String = "",
    val tipo: String = "",
    val comuna: String = "",
    val sitioWeb: String = "",
    val observaciones: String = ""
)

data class OrganizacionErrores(
    val nombre: String? = null,
    val tipo: String? = null,
    val comuna: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(nombre, tipo, comuna).any { it != null }
}

data class ContactoFormState(
    val id: Long = 0,
    val nombre: String = "",
    val organizacionId: Long? = null,
    val cargo: String = "",
    val area: String = "",
    val correo: String = "",
    val telefono: String = "",
    val observaciones: String = ""
)

data class ContactoErrores(
    val nombre: String? = null,
    val organizacionId: String? = null,
    val cargo: String? = null,
    val correo: String? = null,
    val telefono: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(nombre, organizacionId, cargo, correo, telefono).any { it != null }
}

data class EventoFormState(
    val id: Long = 0,
    val nombre: String = "",
    val fecha: Long? = null,
    val comuna: String = "",
    val organizacionId: Long? = null
)

data class EventoErrores(
    val nombre: String? = null,
    val fecha: String? = null,
    val comuna: String? = null,
    val organizacionId: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(nombre, fecha, comuna, organizacionId).any { it != null }
}
