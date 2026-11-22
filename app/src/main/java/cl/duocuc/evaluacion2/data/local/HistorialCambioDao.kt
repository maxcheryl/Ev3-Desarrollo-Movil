package cl.duocuc.evaluacion2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistorialCambioDao {

    @Query("SELECT * FROM historial_cambios WHERE oportunidadId = :oportunidadId ORDER BY fecha DESC")
    fun observePorOportunidad(oportunidadId: Long): Flow<List<HistorialCambioEntity>>

    @Insert
    suspend fun insert(cambio: HistorialCambioEntity): Long

    @Query("DELETE FROM historial_cambios WHERE oportunidadId = :oportunidadId")
    suspend fun deleteByOportunidad(oportunidadId: Long): Int
}
