package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.local.EventoDao
import cl.duocuc.evaluacion2.data.local.EventoEntity
import kotlinx.coroutines.flow.Flow

class EventoRepository(private val dao: EventoDao) {

    fun observeAll(): Flow<List<EventoEntity>> = dao.observeAll()

    suspend fun contar(): Int = dao.count()

    suspend fun guardar(evento: EventoEntity): Long {
        return if (evento.id == 0L) {
            dao.insert(evento)
        } else {
            dao.update(evento)
            evento.id
        }
    }
}
