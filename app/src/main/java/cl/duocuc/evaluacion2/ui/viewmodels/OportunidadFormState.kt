package cl.duocuc.evaluacion2.ui.viewmodels

import cl.duocuc.evaluacion2.data.model.EstadoGestion

/**
 * Datos que el formulario muestra al crear o editar una oportunidad.
 * Las opciones guardan el nombre elegido, como tipo o prioridad.
 */
data class OportunidadFormState(
    val id: Long = 0,
    val nombre: String = "",
    val tipo: String = "",
    val descripcion: String = "",
    val fuente: String = "",
    val prioridad: String = "",
    val potencialComercial: String = "",
    val estado: String = EstadoGestion.EVENTO_IDENTIFICADO.name,
    val observaciones: String = "",
    val codigoQr: String = "",
    val eventoId: Long? = null,
    val organizacionId: Long? = null,
    val contactoId: Long? = null,
    val responsableId: Long? = null,
    val publicoEsperado: String = "",
    val comuna: String = "",
    val fechaCreacion: Long? = null
)

/** Mensajes de error que la pantalla muestra junto a cada campo. */
data class OportunidadErrores(
    val nombre: String? = null,
    val tipo: String? = null,
    val descripcion: String? = null,
    val fuente: String? = null,
    val prioridad: String? = null,
    val potencialComercial: String? = null,
    val estado: String? = null,
    val fechaIdentificacion: String? = null,
    val comuna: String? = null,
    val publicoEsperado: String? = null,
    val observaciones: String? = null,
    val organizacionId: String? = null,
    val contactoId: String? = null,
    val eventoId: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(
            nombre,
            tipo,
            descripcion,
            fuente,
            prioridad,
            potencialComercial,
            estado,
            fechaIdentificacion,
            comuna,
            publicoEsperado,
            observaciones,
            organizacionId,
            contactoId,
            eventoId
        ).any { it != null }
}

/** Opciones disponibles para indicar de dónde llegó la oportunidad. */
object FuentesOportunidad {
    val opciones = listOf(
        "Redes sociales",
        "Sitio web",
        "Correo electrónico",
        "Contacto directo",
        "Referencia",
        "Comunicación comercial"
    )

    fun esValida(fuente: String): Boolean = fuente in opciones
}
