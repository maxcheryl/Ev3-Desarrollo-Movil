package cl.duocuc.evaluacion2.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evidencias",
    indices = [Index("activacionId")]
)
data class EvidenciaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activacionId: Long,
    val tipo: String,
    val nombre: String,
    val ruta: String,
    val fecha: Long = System.currentTimeMillis()
)
