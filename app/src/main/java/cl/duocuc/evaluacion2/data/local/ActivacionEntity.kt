package cl.duocuc.evaluacion2.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activaciones",
    indices = [
        Index(value = ["oportunidadId"], unique = true),
        Index("responsableId")
    ]
)
data class ActivacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val oportunidadId: Long,
    val participacionConfirmada: Boolean = false,
    val responsableId: Long,
    val espacio: String = "",
    val fecha: Long? = null,
    val costo: Double? = null,
    val requisitos: String = "",
    val fechaCreacion: Long = System.currentTimeMillis()
)
