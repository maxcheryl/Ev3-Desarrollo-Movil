package cl.duocuc.evaluacion2.data.historial

import cl.duocuc.evaluacion2.data.local.GestionEntity
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.Prioridad
import cl.duocuc.evaluacion2.data.model.TipoAccionSeguimiento
import cl.duocuc.evaluacion2.data.model.TipoAccionHistorial
import cl.duocuc.evaluacion2.data.model.TipoGestion

data class CambioHistorial(
    val tipoAccion: TipoAccionHistorial,
    val descripcion: String
)

object HistorialDescripciones {

    fun creacion(oportunidad: OpportunityEntity, responsable: String?): CambioHistorial {
        val estado = EstadoGestion.desdeNombre(oportunidad.estado).etiqueta
        val prioridad = Prioridad.desdeNombre(oportunidad.prioridad).etiqueta
        val responsableTexto = responsable ?: "Sin responsable"
        return CambioHistorial(
            tipoAccion = TipoAccionHistorial.CREACION,
            descripcion = "Oportunidad creada. Estado: $estado. Prioridad: $prioridad. " +
                "Responsable: $responsableTexto."
        )
    }

    fun actualizacion(
        anterior: OpportunityEntity,
        nueva: OpportunityEntity,
        responsableAnterior: String?,
        responsableNuevo: String?
    ): List<CambioHistorial> {
        val cambios = mutableListOf<CambioHistorial>()

        if (anterior.estado != nueva.estado) {
            cambios += CambioHistorial(
                tipoAccion = TipoAccionHistorial.CAMBIO_ESTADO,
                descripcion = "Estado: ${EstadoGestion.desdeNombre(anterior.estado).etiqueta} → " +
                    "${EstadoGestion.desdeNombre(nueva.estado).etiqueta}."
            )
        }

        if (anterior.prioridad != nueva.prioridad) {
            cambios += CambioHistorial(
                tipoAccion = TipoAccionHistorial.CAMBIO_PRIORIDAD,
                descripcion = "Prioridad: ${Prioridad.desdeNombre(anterior.prioridad).etiqueta} → " +
                    "${Prioridad.desdeNombre(nueva.prioridad).etiqueta}."
            )
        }

        if (anterior.responsableId != nueva.responsableId) {
            val anteriorTexto = responsableAnterior ?: "Sin responsable"
            val nuevoTexto = responsableNuevo ?: "Sin responsable"
            val tipo = if (anterior.responsableId == null) {
                TipoAccionHistorial.ASIGNACION_RESPONSABLE
            } else {
                TipoAccionHistorial.REASIGNACION_RESPONSABLE
            }
            val descripcion = when {
                nueva.responsableId == null ->
                    "Responsable removido (antes: $anteriorTexto)."
                anterior.responsableId == null ->
                    "Responsable asignado: $nuevoTexto."
                else ->
                    "Responsable reasignado: $anteriorTexto → $nuevoTexto."
            }
            cambios += CambioHistorial(tipoAccion = tipo, descripcion = descripcion)
        }

        val campos = mutableListOf<String>()
        if (anterior.nombre != nueva.nombre) campos += "Nombre"
        if (anterior.descripcion != nueva.descripcion) campos += "Descripción"
        if (anterior.tipo != nueva.tipo) campos += "Tipo"
        if (anterior.fuente != nueva.fuente) campos += "Fuente u origen"
        if (anterior.potencialComercial != nueva.potencialComercial) campos += "Potencial comercial"
        if (anterior.organizacionId != nueva.organizacionId) campos += "Organización"
        if (anterior.contactoId != nueva.contactoId) campos += "Contacto"
        if (anterior.eventoId != nueva.eventoId) campos += "Evento"
        if (anterior.observaciones != nueva.observaciones) campos += "Observaciones"
        if (anterior.codigoQr != nueva.codigoQr) campos += "Referencia QR"
        if (anterior.publicoEsperado != nueva.publicoEsperado) campos += "Público esperado"
        if (anterior.comuna != nueva.comuna) campos += "Comuna"

        if (campos.isNotEmpty()) {
            val etiqueta = if (campos.size == 1) "Dato actualizado" else "Datos actualizados"
            cambios += CambioHistorial(
                tipoAccion = TipoAccionHistorial.EDICION,
                descripcion = "$etiqueta: ${campos.joinToString(", ")}."
            )
        }

        return cambios
    }

    fun gestion(
        gestion: GestionEntity,
        estadoAnterior: String,
        estadoNuevo: String
    ): CambioHistorial {
        val accionSeguimiento = TipoAccionSeguimiento.desdeNombre(gestion.tipo)
        val tipo = TipoGestion.desdeNombre(gestion.tipo)
        val base = when {
            accionSeguimiento != null -> "Seguimiento de ${accionSeguimiento.etiqueta} registrado"
            tipo == TipoGestion.GESTION -> "Gestión registrada"
            else -> "Gestión de ${tipo.etiqueta} registrada"
        }
        val detalle = (gestion.descripcion.ifBlank { gestion.resultado })
            .trim()
            .trimEnd('.', '!', '?')
        val partes = mutableListOf(base)
        if (detalle.isNotBlank()) partes += detalle
        if (estadoAnterior != estadoNuevo) {
            partes += "Estado: ${EstadoGestion.desdeNombre(estadoAnterior).etiqueta} → " +
                "${EstadoGestion.desdeNombre(estadoNuevo).etiqueta}"
        }
        return CambioHistorial(
            tipoAccion = TipoAccionHistorial.GESTION,
            descripcion = partes.joinToString(". ") + "."
        )
    }
}
