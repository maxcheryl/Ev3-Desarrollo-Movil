package cl.duocuc.evaluacion2.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction

interface EjecutorTransacciones {
    suspend fun <T> ejecutar(bloque: suspend () -> T): T
}

class RoomEjecutorTransacciones(
    private val database: RoomDatabase
) : EjecutorTransacciones {
    override suspend fun <T> ejecutar(bloque: suspend () -> T): T =
        database.withTransaction { bloque() }
}
