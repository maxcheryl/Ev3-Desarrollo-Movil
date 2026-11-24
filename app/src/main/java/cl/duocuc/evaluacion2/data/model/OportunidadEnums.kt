package cl.duocuc.evaluacion2.data.model

/**
 * Opciones fijas que usa la aplicación.
 * La base guarda el nombre y la pantalla muestra un texto más claro.
 * Si encuentra un nombre que ya no existe, usa una opción predeterminada.
 */

enum class TipoOportunidad(val etiqueta: String) {
    EVENTO_CORPORATIVO("Evento corporativo"),
    FERIA_EMPRESARIAL("Feria empresarial"),
    ACTIVIDAD_MUNICIPAL("Actividad municipal"),
    JORNADA_DE_BENEFICIOS("Jornada de beneficios"),
    FERIA_INSTITUCIONAL("Feria institucional"),
    ACTIVIDAD_DE_TRABAJADORES("Actividad de trabajadores"),
    ACTIVACION_COMERCIAL("Activación comercial"),
    OTRA_INSTANCIA("Otra instancia relevante");

    companion object {
        fun desdeNombre(nombre: String): TipoOportunidad =
            entries.firstOrNull { it.name == nombre } ?: EVENTO_CORPORATIVO
    }
}

enum class Prioridad(val etiqueta: String) {
    ALTA("Alta"),
    MEDIA("Media"),
    BAJA("Baja");

    companion object {
        fun desdeNombre(nombre: String): Prioridad =
            entries.firstOrNull { it.name == nombre } ?: MEDIA
    }
}

enum class PotencialComercial(val etiqueta: String) {
    ALTO("Alto"),
    MEDIO("Medio"),
    BAJO("Bajo");

    companion object {
        fun desdeNombre(nombre: String): PotencialComercial =
            entries.firstOrNull { it.name == nombre } ?: MEDIO
    }
}

enum class EstadoGestion(val etiqueta: String) {
    EVENTO_IDENTIFICADO("Evento identificado"),
    CONTACTO_PENDIENTE("Contacto pendiente"),
    CONTACTO_REALIZADO("Contacto realizado"),
    PROPUESTA_ENVIADA("Propuesta enviada"),
    EN_ESPERA_DE_RESPUESTA("En espera de respuesta"),
    PARTICIPACION_CONFIRMADA("Participación confirmada"),
    OPORTUNIDAD_DESCARTADA("Oportunidad descartada"),
    ACTIVACION_REALIZADA("Activación realizada"),
    ACTIVACION_CANCELADA("Activación cancelada");

    companion object {
        fun desdeNombre(nombre: String): EstadoGestion =
            entries.firstOrNull { it.name == nombre } ?: EVENTO_IDENTIFICADO
    }
}
