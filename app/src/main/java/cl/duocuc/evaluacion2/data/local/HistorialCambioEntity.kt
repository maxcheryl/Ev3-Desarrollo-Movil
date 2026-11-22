package cl.duocuc.evaluacion2.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "historial_cambios",
    indices = [Index("oportunidadId"), Index("usuarioId")]
)
data class HistorialCambioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val oportunidadId: Long,
    val usuarioId: Long,
    val tipoAccion: String,
    val descripcion: String,
    val fecha: Long = System.currentTimeMillis()
)
