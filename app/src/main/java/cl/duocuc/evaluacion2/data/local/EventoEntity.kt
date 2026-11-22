package cl.duocuc.evaluacion2.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "eventos",
    indices = [Index("organizacionId")]
)
data class EventoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val fecha: Long,
    val comuna: String,
    val organizacionId: Long
)
