package cl.duocuc.evaluacion2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoDao {

    @Query("SELECT * FROM eventos ORDER BY fecha")
    fun observeAll(): Flow<List<EventoEntity>>

    @Query("SELECT * FROM eventos WHERE organizacionId = :organizacionId ORDER BY fecha")
    fun observePorOrganizacion(organizacionId: Long): Flow<List<EventoEntity>>

    @Query("SELECT * FROM eventos WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): EventoEntity?

    @Query("SELECT COUNT(*) FROM eventos")
    suspend fun count(): Int

    @Insert
    suspend fun insert(evento: EventoEntity): Long

    @Update
    suspend fun update(evento: EventoEntity)

    @Query("DELETE FROM eventos WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM eventos WHERE organizacionId = :organizacionId")
    suspend fun deleteByOrganizacion(organizacionId: Long): Int
}
