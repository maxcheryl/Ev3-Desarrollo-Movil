package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.local.ActivacionEntity
import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.ActivacionDao
import cl.duocuc.evaluacion2.data.local.EvidenciaEntity
import cl.duocuc.evaluacion2.data.local.EvidenciaDao
import cl.duocuc.evaluacion2.data.local.HistorialCambioDao
import cl.duocuc.evaluacion2.data.local.HistorialCambioEntity
import cl.duocuc.evaluacion2.data.local.OpportunityDao
import cl.duocuc.evaluacion2.data.local.ResultadoEntity
import cl.duocuc.evaluacion2.data.local.ResultadoDao
import cl.duocuc.evaluacion2.data.local.UsuarioDao
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import kotlinx.coroutines.flow.Flow

class ActivacionRepository(
    private val activacionDao: ActivacionDao,
    private val evidenciaDao: EvidenciaDao,
    private val historialDao: HistorialCambioDao,
    private val opportunityDao: OpportunityDao,
    private val resultadoDao: ResultadoDao,
    private val usuarioDao: UsuarioDao,
    private val ejecutorTransacciones: EjecutorTransacciones
) {

    constructor(database: AppDatabase) : this(
        activacionDao = database.activacionDao(),
        evidenciaDao = database.evidenciaDao(),
        historialDao = database.historialCambioDao(),
        opportunityDao = database.opportunityDao(),
        resultadoDao = database.resultadoDao(),
        usuarioDao = database.usuarioDao(),
        ejecutorTransacciones = RoomEjecutorTransacciones(database)
    )

    fun observeUsuarios() = usuarioDao.observeAll()

    suspend fun obtenerOportunidad(id: Long) = opportunityDao.buscarPorId(id)

    suspend fun obtenerActivacion(oportunidadId: Long) =
        activacionDao.obtenerPorOportunidad(oportunidadId)

    suspend fun obtenerEvidencias(activacionId: Long) =
        evidenciaDao.listarPorActivacion(activacionId)

    suspend fun obtenerResultado(activacionId: Long) =
        resultadoDao.obtenerPorActivacion(activacionId)

    suspend fun guardarActivacion(activacion: ActivacionEntity, usuarioId: Long): Long =
        ejecutorTransacciones.ejecutar {
            check(opportunityDao.buscarPorId(activacion.oportunidadId) != null) {
                "La oportunidad ya no está disponible."
            }
            check(usuarioDao.buscarPorId(activacion.responsableId) != null) {
                "Selecciona un responsable disponible."
            }

            val existente = activacionDao.obtenerPorOportunidad(activacion.oportunidadId)
            val id = if (existente == null) {
                activacionDao.insert(activacion.copy(id = 0))
            } else {
                activacionDao.update(activacion.copy(id = existente.id))
                existente.id
            }

            historialDao.insert(
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
        ejecutorTransacciones.ejecutar {
            val activacion = activacionDao.buscarPorId(evidencia.activacionId)
                ?: error("No se encontró la activación asociada.")
            val id = evidenciaDao.insert(evidencia)
            historialDao.insert(
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
    ) = ejecutorTransacciones.ejecutar {
        require(nuevoEstado == EstadoGestion.PARTICIPACION_CONFIRMADA.name ||
            nuevoEstado == EstadoGestion.OPORTUNIDAD_DESCARTADA.name
        ) {
            "Selecciona un resultado final válido."
        }

        val activacion = activacionDao.buscarPorId(activacionId)
            ?: error("No se encontró la activación asociada.")
        val oportunidad = opportunityDao.buscarPorId(activacion.oportunidadId)
            ?: error("La oportunidad ya no está disponible.")
        val ahora = System.currentTimeMillis()
        val actual = resultadoDao.obtenerPorActivacion(activacionId)
        val resultadoEntity = ResultadoEntity(
            id = actual?.id ?: 0,
            activacionId = activacionId,
            resultado = resultado,
            observaciones = observaciones,
            impacto = impacto,
            fecha = ahora
        )

        if (actual == null) {
            resultadoDao.insert(resultadoEntity)
        } else {
            resultadoDao.update(resultadoEntity)
        }

        activacionDao.update(
            activacion.copy(
                participacionConfirmada =
                    nuevoEstado == EstadoGestion.PARTICIPACION_CONFIRMADA.name
            )
        )
        opportunityDao.update(
            oportunidad.copy(estado = nuevoEstado, fechaActualizacion = ahora)
        )
        historialDao.insert(
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
        resultadoDao.observePorActivacion(activacionId)
}
