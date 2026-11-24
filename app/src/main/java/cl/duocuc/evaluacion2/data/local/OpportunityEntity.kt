package cl.duocuc.evaluacion2.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Datos de una oportunidad guardados en la base.
 * El tipo, la prioridad y el estado se guardan por nombre; la pantalla usa
 * ese nombre para mostrar una etiqueta más fácil de leer.
 * Un id de 0 indica que es nueva; Room asigna el id al guardarla.
 * Los códigos de evento, organización, contacto y responsable se revisan
 * en los repositorios antes de guardar.
 */
@Entity(
    tableName = "oportunidades",
    indices = [
        Index("eventoId"),
        Index("organizacionId"),
        Index("contactoId"),
        Index("responsableId")
    ]
)
data class OpportunityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val tipo: String,
    val descripcion: String,
    val fuente: String,
    val prioridad: String,
    val potencialComercial: String,
    val estado: String,
    val observaciones: String = "",
    val codigoQr: String = "",
    val eventoId: Long? = null,
    val organizacionId: Long? = null,
    val contactoId: Long? = null,
    val responsableId: Long? = null,
    @ColumnInfo(defaultValue = "''") val publicoEsperado: String = "",
    @ColumnInfo(defaultValue = "''") val comuna: String = "",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis()
)
