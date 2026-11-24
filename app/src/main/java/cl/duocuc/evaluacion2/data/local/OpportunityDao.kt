package cl.duocuc.evaluacion2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Consulta y modifica las oportunidades guardadas.
 * Las listas avisan a la pantalla cuando cambian los datos.
 */
@Dao
interface OpportunityDao {

    /** Muestra primero la oportunidad que se actualizó más recientemente. */
    @Query("SELECT * FROM oportunidades ORDER BY fechaActualizacion DESC")
    fun observeAll(): Flow<List<OpportunityEntity>>

    @Query("SELECT COUNT(*) FROM oportunidades")
    suspend fun count(): Int

    @Query("SELECT * FROM oportunidades WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): OpportunityEntity?

    @Query(
        "SELECT * FROM oportunidades " +
            "WHERE id NOT IN (SELECT oportunidadId FROM historial_cambios) " +
            "ORDER BY fechaCreacion ASC"
    )
    suspend fun listarSinHistorial(): List<OpportunityEntity>

    // Guarda la oportunidad nueva o reemplaza la que ya tiene ese id.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(oportunidad: OpportunityEntity): Long

    @Update
    suspend fun update(oportunidad: OpportunityEntity)

    @Query(
        "UPDATE oportunidades SET eventoId = :eventoId, " +
            "fechaActualizacion = :fechaActualizacion WHERE id = :oportunidadId"
    )
    suspend fun asociarEvento(
        oportunidadId: Long,
        eventoId: Long,
        fechaActualizacion: Long
    ): Int

    @Query("UPDATE oportunidades SET responsableId = :responsableId WHERE responsableId IS NULL")
    suspend fun assignNullResponsible(responsableId: Long): Int

    @Query("DELETE FROM oportunidades WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
