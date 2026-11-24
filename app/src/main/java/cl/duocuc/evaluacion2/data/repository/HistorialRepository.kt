package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.HistorialCambioDao
import cl.duocuc.evaluacion2.data.local.HistorialCambioEntity
import kotlinx.coroutines.flow.Flow

class HistorialRepository(private val dao: HistorialCambioDao) {

    constructor(database: AppDatabase) : this(database.historialCambioDao())

    fun observePorOportunidad(oportunidadId: Long): Flow<List<HistorialCambioEntity>> =
        dao.observePorOportunidad(oportunidadId)
}
