package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.local.ContactoDao
import cl.duocuc.evaluacion2.data.local.ContactoEntity
import kotlinx.coroutines.flow.Flow

class ContactoRepository(private val dao: ContactoDao) {

    fun observeAll(): Flow<List<ContactoEntity>> = dao.observeAll()

    suspend fun contar(): Int = dao.count()

    suspend fun guardar(contacto: ContactoEntity): Long {
        return if (contacto.id == 0L) {
            dao.insert(contacto)
        } else {
            dao.update(contacto)
            contacto.id
        }
    }
}
