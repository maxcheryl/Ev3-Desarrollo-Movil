package cl.duocuc.evaluacion2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenciaDao {

    @Query("SELECT * FROM evidencias WHERE activacionId = :activacionId ORDER BY fecha DESC")
    fun observePorActivacion(activacionId: Long): Flow<List<EvidenciaEntity>>

    @Insert
    suspend fun insert(evidencia: EvidenciaEntity): Long

    @Query("DELETE FROM evidencias WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM evidencias WHERE activacionId = :activacionId")
    suspend fun deleteByActivacion(activacionId: Long): Int
}
