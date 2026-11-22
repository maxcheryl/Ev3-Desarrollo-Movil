package cl.duocuc.evaluacion2.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contactos",
    indices = [Index("organizacionId")]
)
data class ContactoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val organizacionId: Long,
    val cargo: String,
    val area: String,
    val correo: String,
    val telefono: String,
    val observaciones: String = ""
)
