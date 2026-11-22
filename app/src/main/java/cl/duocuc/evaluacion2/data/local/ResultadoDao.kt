package cl.duocuc.evaluacion2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ResultadoDao {

    @Query("SELECT * FROM resultados WHERE activacionId = :activacionId LIMIT 1")
    suspend fun obtenerPorActivacion(activacionId: Long): ResultadoEntity?

    @Insert
    suspend fun insert(resultado: ResultadoEntity): Long

    @Update
    suspend fun update(resultado: ResultadoEntity)

    @Query("DELETE FROM resultados WHERE activacionId = :activacionId")
    suspend fun deleteByActivacion(activacionId: Long): Int
}
