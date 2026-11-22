package cl.duocuc.evaluacion2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizacionDao {

    @Query("SELECT * FROM organizaciones ORDER BY nombre")
    fun observeAll(): Flow<List<OrganizacionEntity>>

    @Query("SELECT * FROM organizaciones WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): OrganizacionEntity?

    @Query("SELECT COUNT(*) FROM organizaciones")
    suspend fun count(): Int

    @Insert
    suspend fun insert(organizacion: OrganizacionEntity): Long

    @Update
    suspend fun update(organizacion: OrganizacionEntity)

    @Query("DELETE FROM organizaciones WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
