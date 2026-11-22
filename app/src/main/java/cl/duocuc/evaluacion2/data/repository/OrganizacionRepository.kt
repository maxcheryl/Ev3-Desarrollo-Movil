package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.local.OrganizacionDao
import cl.duocuc.evaluacion2.data.local.OrganizacionEntity
import kotlinx.coroutines.flow.Flow

class OrganizacionRepository(private val dao: OrganizacionDao) {

    fun observeAll(): Flow<List<OrganizacionEntity>> = dao.observeAll()

    suspend fun contar(): Int = dao.count()

    suspend fun guardar(organizacion: OrganizacionEntity): Long {
        return if (organizacion.id == 0L) {
            dao.insert(organizacion)
        } else {
            dao.update(organizacion)
            organizacion.id
        }
    }
}
