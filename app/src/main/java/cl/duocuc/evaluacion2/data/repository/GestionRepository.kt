package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.historial.HistorialDescripciones
import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.GestionDao
import cl.duocuc.evaluacion2.data.local.GestionEntity
import cl.duocuc.evaluacion2.data.local.HistorialCambioDao
import cl.duocuc.evaluacion2.data.local.HistorialCambioEntity
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import cl.duocuc.evaluacion2.data.local.OpportunityDao
import kotlinx.coroutines.flow.Flow

class GestionRepository(
    private val gestionDao: GestionDao,
    private val opportunityDao: OpportunityDao,
    private val historialDao: HistorialCambioDao,
    private val ejecutorTransacciones: EjecutorTransacciones
) {

    constructor(database: AppDatabase) : this(
        gestionDao = database.gestionDao(),
        opportunityDao = database.opportunityDao(),
        historialDao = database.historialCambioDao(),
        ejecutorTransacciones = RoomEjecutorTransacciones(database)
    )

    fun observePorOportunidad(oportunidadId: Long): Flow<List<GestionEntity>> =
        gestionDao.observePorOportunidad(oportunidadId)

    suspend fun buscarOportunidad(oportunidadId: Long): OpportunityEntity? =
        opportunityDao.buscarPorId(oportunidadId)

    suspend fun registrar(gestion: GestionEntity, nuevoEstado: String): Long =
        ejecutorTransacciones.ejecutar {
            val oportunidad = opportunityDao.buscarPorId(gestion.oportunidadId)
                ?: error("La oportunidad ya no está disponible.")
            val id = gestionDao.insert(gestion)
            val ahora = System.currentTimeMillis()
            opportunityDao.update(
                oportunidad.copy(
                    estado = nuevoEstado,
                    fechaActualizacion = ahora
                )
            )
            val cambio = HistorialDescripciones.gestion(
                gestion = gestion,
                estadoAnterior = oportunidad.estado,
                estadoNuevo = nuevoEstado
            )
            historialDao.insert(
                HistorialCambioEntity(
                    oportunidadId = gestion.oportunidadId,
                    usuarioId = gestion.usuarioId,
                    tipoAccion = cambio.tipoAccion.name,
                    descripcion = cambio.descripcion,
                    fecha = ahora
                )
            )
            id
        }
}
