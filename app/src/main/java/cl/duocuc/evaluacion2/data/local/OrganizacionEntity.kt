package cl.duocuc.evaluacion2.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "organizaciones",
    indices = [Index(value = ["nombre"], unique = true)]
)
data class OrganizacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val tipo: String,
    val comuna: String,
    val sitioWeb: String = "",
    val observaciones: String = ""
)
