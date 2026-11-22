package cl.duocuc.evaluacion2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactoDao {

    @Query("SELECT * FROM contactos ORDER BY nombre")
    fun observeAll(): Flow<List<ContactoEntity>>

    @Query("SELECT * FROM contactos WHERE organizacionId = :organizacionId ORDER BY nombre")
    fun observePorOrganizacion(organizacionId: Long): Flow<List<ContactoEntity>>

    @Query("SELECT * FROM contactos WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): ContactoEntity?

    @Query("SELECT COUNT(*) FROM contactos")
    suspend fun count(): Int

    @Insert
    suspend fun insert(contacto: ContactoEntity): Long

    @Update
    suspend fun update(contacto: ContactoEntity)

    @Query("DELETE FROM contactos WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM contactos WHERE organizacionId = :organizacionId")
    suspend fun deleteByOrganizacion(organizacionId: Long): Int
}
