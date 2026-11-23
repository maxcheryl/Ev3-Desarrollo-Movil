package cl.duocuc.evaluacion2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivacionDao {

    @Query("SELECT * FROM activaciones WHERE oportunidadId = :oportunidadId LIMIT 1")
    fun observePorOportunidad(oportunidadId: Long): Flow<ActivacionEntity?>

    @Query("SELECT * FROM activaciones WHERE oportunidadId = :oportunidadId LIMIT 1")
    suspend fun obtenerPorOportunidad(oportunidadId: Long): ActivacionEntity?

    @Query("SELECT * FROM activaciones WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): ActivacionEntity?

    @Insert
    suspend fun insert(activacion: ActivacionEntity): Long

    @Update
    suspend fun update(activacion: ActivacionEntity)

    @Query("DELETE FROM activaciones WHERE oportunidadId = :oportunidadId")
    suspend fun deleteByOportunidad(oportunidadId: Long): Int
}
