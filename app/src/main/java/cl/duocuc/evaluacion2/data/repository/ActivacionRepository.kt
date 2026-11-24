package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.local.ActivacionEntity
import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.ActivacionDao
import cl.duocuc.evaluacion2.data.local.EvidenciaEntity
import cl.duocuc.evaluacion2.data.local.EvidenciaDao
import cl.duocuc.evaluacion2.data.local.EventoDao
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
    private val ejecutorTransacciones: EjecutorTransacciones,
    private val eventoDao: EventoDao? = null
) {

    constructor(database: AppDatabase) : this(
        activacionDao = database.activacionDao(),
        evidenciaDao = database.evidenciaDao(),
        historialDao = database.historialCambioDao(),
        opportunityDao = database.opportunityDao(),
        resultadoDao = database.resultadoDao(),
        usuarioDao = database.usuarioDao(),
        ejecutorTransacciones = RoomEjecutorTransacciones(database),
        eventoDao = database.eventoDao()
    )

    fun observeUsuarios() = usuarioDao.observeAll()

    suspend fun obtenerOportunidad(id: Long) = opportunityDao.buscarPorId(id)

    suspend fun obtenerEvento(id: Long) = eventoDao?.buscarPorId(id)

    suspend fun obtenerActivacion(oportunidadId: Long) =
        activacionDao.obtenerPorOportunidad(oportunidadId)

    suspend fun obtenerEvidencias(activacionId: Long) =
        evidenciaDao.listarPorActivacion(activacionId)

    suspend fun obtenerResultado(activacionId: Long) =
        resultadoDao.obtenerPorActivacion(activacionId)

    suspend fun guardarActivacion(activacion: ActivacionEntity, usuarioId: Long): Long =
        ejecutorTransacciones.ejecutar {
            val oportunidad = opportunityDao.buscarPorId(activacion.oportunidadId)
                ?: error("La oportunidad ya no está disponible.")
            check(
                oportunidad.estado == EstadoGestion.PARTICIPACION_CONFIRMADA.name ||
                    oportunidad.estado == EstadoGestion.ACTIVACION_REALIZADA.name ||
                    oportunidad.estado == EstadoGestion.ACTIVACION_CANCELADA.name
            ) {
                "La activación solo se puede coordinar cuando la participación está confirmada."
            }
            check(usuarioDao.buscarPorId(activacion.responsableId) != null) {
                "Selecciona un responsable disponible."
            }
            require((activacion.fecha ?: 0L) > 0L) {
                "Selecciona la fecha de realización."
            }
            require(activacion.costo == null ||
                (activacion.costo.isFinite() && activacion.costo >= 0)
            ) {
                "El costo debe ser un número igual o mayor que cero."
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
        usuarioId: Long,
        fechaRealizacion: Long? = null,
        asistentesEstimados: Int? = null,
        contactosGenerados: Int = 0,
        personasInteresadas: Int = 0,
        inscripciones: Int = 0
    ) = ejecutorTransacciones.ejecutar {
        require(nuevoEstado == EstadoGestion.ACTIVACION_REALIZADA.name ||
            nuevoEstado == EstadoGestion.ACTIVACION_CANCELADA.name
        ) {
            "Selecciona un resultado final válido."
        }

        val activacion = activacionDao.buscarPorId(activacionId)
            ?: error("No se encontró la activación asociada.")
        val oportunidad = opportunityDao.buscarPorId(activacion.oportunidadId)
            ?: error("La oportunidad ya no está disponible.")
        check(
            oportunidad.estado == EstadoGestion.PARTICIPACION_CONFIRMADA.name ||
                oportunidad.estado == EstadoGestion.ACTIVACION_REALIZADA.name ||
                oportunidad.estado == EstadoGestion.ACTIVACION_CANCELADA.name
        ) {
            "La oportunidad debe estar confirmada antes de registrar el resultado."
        }
        require(
            nuevoEstado != EstadoGestion.ACTIVACION_REALIZADA.name ||
                (fechaRealizacion ?: 0L) > 0L
        ) {
            "Selecciona la fecha de realización."
        }
        require(fechaRealizacion == null || fechaRealizacion > 0L) {
            "Selecciona una fecha de realización válida."
        }
        require(
            listOf(
                asistentesEstimados,
                contactosGenerados,
                personasInteresadas,
                inscripciones
            ).all { it == null || it >= 0 }
        ) {
            "Las cantidades deben ser números iguales o mayores que cero."
        }
        val ahora = System.currentTimeMillis()
        val actual = resultadoDao.obtenerPorActivacion(activacionId)
        val resultadoEntity = ResultadoEntity(
            id = actual?.id ?: 0,
            activacionId = activacionId,
            resultado = resultado,
            observaciones = observaciones,
            impacto = impacto,
            fecha = ahora,
            fechaRealizacion = fechaRealizacion,
            asistentesEstimados = asistentesEstimados,
            contactosGenerados = contactosGenerados,
            personasInteresadas = personasInteresadas,
            inscripciones = inscripciones
        )

        if (actual == null) {
            resultadoDao.insert(resultadoEntity)
        } else {
            resultadoDao.update(resultadoEntity)
        }

        activacionDao.update(
            activacion.copy(
                participacionConfirmada =
                    nuevoEstado == EstadoGestion.ACTIVACION_REALIZADA.name
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
