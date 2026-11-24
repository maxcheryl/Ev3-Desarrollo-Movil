package cl.duocuc.evaluacion2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GestionDao {

    @Query("SELECT * FROM gestiones WHERE oportunidadId = :oportunidadId ORDER BY fecha DESC")
    fun observePorOportunidad(oportunidadId: Long): Flow<List<GestionEntity>>

    @Insert
    suspend fun insert(gestion: GestionEntity): Long

    @Query("DELETE FROM gestiones WHERE oportunidadId = :oportunidadId")
    suspend fun deleteByOportunidad(oportunidadId: Long): Int
}
