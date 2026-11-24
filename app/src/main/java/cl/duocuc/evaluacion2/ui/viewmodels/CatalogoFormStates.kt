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
    val comuna: String? = null,
    val sitioWeb: String? = null,
    val observaciones: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(nombre, tipo, comuna, sitioWeb, observaciones).any { it != null }
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
    val telefono: String? = null,
    val area: String? = null,
    val observaciones: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(
            nombre,
            organizacionId,
            cargo,
            correo,
            telefono,
            area,
            observaciones
        ).any { it != null }
}

data class EventoFormState(
    val id: Long = 0,
    val nombre: String = "",
    val tipo: String = "EVENTO_CORPORATIVO",
    val descripcion: String = "",
    val fecha: Long? = null,
    val fechaTermino: Long? = null,
    val horaInicio: String = "",
    val horaTermino: String = "",
    val direccion: String = "",
    val region: String = "",
    val comuna: String = "",
    val organizacionId: Long? = null,
    val publicoEsperado: String = "",
    val publicoObjetivo: List<String> = emptyList(),
    val asistentesEstimados: String = "",
    val condicionesParticipacion: String = "",
    val requisitos: String = "",
    val costoReferencial: String = "",
    val espaciosDisponibles: String = ""
)

data class EventoErrores(
    val nombre: String? = null,
    val tipo: String? = null,
    val fecha: String? = null,
    val fechaTermino: String? = null,
    val region: String? = null,
    val comuna: String? = null,
    val organizacionId: String? = null,
    val asistentesEstimados: String? = null,
    val costoReferencial: String? = null,
    val horaInicio: String? = null,
    val horaTermino: String? = null,
    val direccion: String? = null,
    val descripcion: String? = null,
    val publicoEsperado: String? = null,
    val publicoObjetivo: String? = null,
    val condicionesParticipacion: String? = null,
    val requisitos: String? = null,
    val espaciosDisponibles: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(
            nombre,
            tipo,
            fecha,
            fechaTermino,
            region,
            comuna,
            organizacionId,
            asistentesEstimados,
            costoReferencial,
            horaInicio,
            horaTermino,
            direccion,
            descripcion,
            publicoEsperado,
            publicoObjetivo,
            condicionesParticipacion,
            requisitos,
            espaciosDisponibles
        ).any { it != null }
}
