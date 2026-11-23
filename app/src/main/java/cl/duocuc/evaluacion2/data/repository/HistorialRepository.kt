package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.HistorialCambioEntity
import kotlinx.coroutines.flow.Flow

class HistorialRepository(private val database: AppDatabase) {

    fun observePorOportunidad(oportunidadId: Long): Flow<List<HistorialCambioEntity>> =
        database.historialCambioDao().observePorOportunidad(oportunidadId)
}
