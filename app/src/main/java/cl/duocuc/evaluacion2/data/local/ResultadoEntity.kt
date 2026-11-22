package cl.duocuc.evaluacion2.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "resultados",
    indices = [Index(value = ["activacionId"], unique = true)]
)
data class ResultadoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activacionId: Long,
    val resultado: String,
    val observaciones: String = "",
    val impacto: String = "",
    val fecha: Long = System.currentTimeMillis()
)
