package cl.duocuc.evaluacion2.data.repository

import androidx.room.withTransaction
import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.GestionEntity
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import kotlinx.coroutines.flow.Flow

class GestionRepository(private val database: AppDatabase) {

    private val gestionDao = database.gestionDao()
    private val opportunityDao = database.opportunityDao()

    fun observePorOportunidad(oportunidadId: Long): Flow<List<GestionEntity>> =
        gestionDao.observePorOportunidad(oportunidadId)

    suspend fun buscarOportunidad(oportunidadId: Long): OpportunityEntity? =
        opportunityDao.buscarPorId(oportunidadId)

    suspend fun registrar(gestion: GestionEntity, nuevoEstado: String): Long =
        database.withTransaction {
            val oportunidad = opportunityDao.buscarPorId(gestion.oportunidadId)
                ?: error("La oportunidad ya no está disponible.")
            val id = gestionDao.insert(gestion)
            opportunityDao.update(
                oportunidad.copy(
                    estado = nuevoEstado,
                    fechaActualizacion = System.currentTimeMillis()
                )
            )
            id
        }
}
