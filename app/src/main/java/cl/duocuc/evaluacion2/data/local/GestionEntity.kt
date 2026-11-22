package cl.duocuc.evaluacion2.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gestiones",
    indices = [Index("oportunidadId"), Index("usuarioId")]
)
data class GestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val oportunidadId: Long,
    val usuarioId: Long,
    val tipo: String,
    val descripcion: String,
    val resultado: String = "",
    val proximaAccion: String = "",
    val fechaProximaAccion: Long? = null,
    val fecha: Long = System.currentTimeMillis()
)
