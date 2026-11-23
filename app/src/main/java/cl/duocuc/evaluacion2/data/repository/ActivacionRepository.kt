package cl.duocuc.evaluacion2.data.repository

import androidx.room.withTransaction
import cl.duocuc.evaluacion2.data.local.ActivacionEntity
import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.EvidenciaEntity
import cl.duocuc.evaluacion2.data.local.HistorialCambioEntity
import cl.duocuc.evaluacion2.data.local.ResultadoEntity
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import kotlinx.coroutines.flow.Flow

class ActivacionRepository(private val database: AppDatabase) {

    fun observeUsuarios() = database.usuarioDao().observeAll()

    suspend fun obtenerOportunidad(id: Long) = database.opportunityDao().buscarPorId(id)

    suspend fun obtenerActivacion(oportunidadId: Long) =
        database.activacionDao().obtenerPorOportunidad(oportunidadId)

    suspend fun obtenerEvidencias(activacionId: Long) =
        database.evidenciaDao().listarPorActivacion(activacionId)

    suspend fun obtenerResultado(activacionId: Long) =
        database.resultadoDao().obtenerPorActivacion(activacionId)

    suspend fun guardarActivacion(activacion: ActivacionEntity, usuarioId: Long): Long =
        database.withTransaction {
            check(database.opportunityDao().buscarPorId(activacion.oportunidadId) != null) {
                "La oportunidad ya no está disponible."
            }
            check(database.usuarioDao().buscarPorId(activacion.responsableId) != null) {
                "Selecciona un responsable disponible."
            }

            val existente = database.activacionDao()
                .obtenerPorOportunidad(activacion.oportunidadId)
            val id = if (existente == null) {
                database.activacionDao().insert(activacion.copy(id = 0))
            } else {
                database.activacionDao().update(activacion.copy(id = existente.id))
                existente.id
            }

            database.historialCambioDao().insert(
                HistorialCambioEntity(
                    oportunidadId = activacion.oportunidadId,
                    usuarioId = usuarioId,
                    tipoAccion = "ACTIVACION",
                    descripcion = if (existente == null) {
                        "Coordinación de activación registrada."
                    } else {
                        "Coordinación de activación actualizada."
                    }
                )
            )
            id
        }

    suspend fun agregarEvidencia(evidencia: EvidenciaEntity, usuarioId: Long): Long =
        database.withTransaction {
            val activacion = database.activacionDao().buscarPorId(evidencia.activacionId)
                ?: error("No se encontró la activación asociada.")
            val id = database.evidenciaDao().insert(evidencia)
            database.historialCambioDao().insert(
                HistorialCambioEntity(
                    oportunidadId = activacion.oportunidadId,
                    usuarioId = usuarioId,
                    tipoAccion = "ACTIVACION",
                    descripcion = "Evidencia adjunta: ${evidencia.nombre}."
                )
            )
            id
        }

    suspend fun guardarResultado(
        activacionId: Long,
        resultado: String,
        observaciones: String,
        impacto: String,
        nuevoEstado: String,
        usuarioId: Long
    ) = database.withTransaction {
        require(nuevoEstado == EstadoGestion.PARTICIPACION_CONFIRMADA.name ||
            nuevoEstado == EstadoGestion.OPORTUNIDAD_DESCARTADA.name
        ) {
            "Selecciona un resultado final válido."
        }

        val activacion = database.activacionDao().buscarPorId(activacionId)
            ?: error("No se encontró la activación asociada.")
        val oportunidad = database.opportunityDao().buscarPorId(activacion.oportunidadId)
            ?: error("La oportunidad ya no está disponible.")
        val ahora = System.currentTimeMillis()
        val actual = database.resultadoDao().obtenerPorActivacion(activacionId)
        val resultadoEntity = ResultadoEntity(
            id = actual?.id ?: 0,
            activacionId = activacionId,
            resultado = resultado,
            observaciones = observaciones,
            impacto = impacto,
            fecha = ahora
        )

        if (actual == null) {
            database.resultadoDao().insert(resultadoEntity)
        } else {
            database.resultadoDao().update(resultadoEntity)
        }

        database.activacionDao().update(
            activacion.copy(
                participacionConfirmada =
                    nuevoEstado == EstadoGestion.PARTICIPACION_CONFIRMADA.name
            )
        )
        database.opportunityDao().update(
            oportunidad.copy(estado = nuevoEstado, fechaActualizacion = ahora)
        )
        database.historialCambioDao().insert(
            HistorialCambioEntity(
                oportunidadId = oportunidad.id,
                usuarioId = usuarioId,
                tipoAccion = "RESULTADO",
                descripcion = "Resultado de activación registrado: " +
                    "${EstadoGestion.desdeNombre(nuevoEstado).etiqueta}."
            )
        )
    }

    fun observeResultado(activacionId: Long): Flow<ResultadoEntity?> =
        database.resultadoDao().observePorActivacion(activacionId)
}
