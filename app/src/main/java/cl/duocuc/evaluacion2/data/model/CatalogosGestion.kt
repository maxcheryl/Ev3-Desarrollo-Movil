package cl.duocuc.evaluacion2.data.model

enum class PerfilUsuario(val etiqueta: String) {
    EJECUTIVO_COMERCIAL("Ejecutivo comercial"),
    SUPERVISOR_COMERCIAL("Supervisor comercial");

    companion object {
        fun desdeNombre(nombre: String): PerfilUsuario =
            entries.firstOrNull { it.name == nombre } ?: EJECUTIVO_COMERCIAL
    }
}

enum class TipoOrganizacion(val etiqueta: String) {
    EMPRESA("Empresa"),
    MUNICIPALIDAD("Municipalidad"),
    INSTITUCION("Institución"),
    OTRA("Otra");

    companion object {
        fun desdeNombre(nombre: String): TipoOrganizacion =
            entries.firstOrNull { it.name == nombre } ?: EMPRESA
    }
}

enum class TipoGestion(val etiqueta: String) {
    CONTACTO("Contacto"),
    GESTION("Gestión"),
    PROPUESTA("Propuesta");

    companion object {
        fun desdeNombre(nombre: String): TipoGestion =
            entries.firstOrNull { it.name == nombre } ?: GESTION
    }
}

enum class TipoAccionHistorial(val etiqueta: String) {
    CREACION("Creación"),
    EDICION("Edición"),
    CAMBIO_ESTADO("Cambio de estado"),
    CAMBIO_PRIORIDAD("Cambio de prioridad"),
    ASIGNACION_RESPONSABLE("Asignación de responsable"),
    REASIGNACION_RESPONSABLE("Reasignación de responsable"),
    ELIMINACION("Eliminación"),
    GESTION("Gestión"),
    ACTIVACION("Activación"),
    RESULTADO("Resultado");

    companion object {
        fun desdeNombre(nombre: String): TipoAccionHistorial =
            entries.firstOrNull { it.name == nombre } ?: EDICION
    }
}

enum class TipoEvidencia(val etiqueta: String) {
    FOTO("Foto"),
    DOCUMENTO("Documento");

    companion object {
        fun desdeNombre(nombre: String): TipoEvidencia =
            entries.firstOrNull { it.name == nombre } ?: FOTO
    }
}
