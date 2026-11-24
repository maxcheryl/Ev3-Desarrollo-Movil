package cl.duocuc.evaluacion2.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "eventos",
    indices = [Index("organizacionId")]
)
data class EventoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val fecha: Long,
    val comuna: String,
    val organizacionId: Long,
    @ColumnInfo(defaultValue = "'EVENTO_CORPORATIVO'")
    val tipo: String = "EVENTO_CORPORATIVO",
    @ColumnInfo(defaultValue = "''")
    val descripcion: String = "",
    val fechaTermino: Long? = null,
    @ColumnInfo(defaultValue = "''")
    val horaInicio: String = "",
    @ColumnInfo(defaultValue = "''")
    val horaTermino: String = "",
    @ColumnInfo(defaultValue = "''")
    val direccion: String = "",
    @ColumnInfo(defaultValue = "''")
    val region: String = "",
    @ColumnInfo(defaultValue = "''")
    val publicoEsperado: String = "",
    @ColumnInfo(defaultValue = "''")
    val publicoObjetivo: String = "",
    val asistentesEstimados: Int? = null,
    @ColumnInfo(defaultValue = "''")
    val condicionesParticipacion: String = "",
    @ColumnInfo(defaultValue = "''")
    val requisitos: String = "",
    val costoReferencial: Double? = null,
    @ColumnInfo(defaultValue = "''")
    val espaciosDisponibles: String = ""
)
