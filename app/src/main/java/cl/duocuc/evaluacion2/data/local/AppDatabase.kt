package cl.duocuc.evaluacion2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de datos Room de la aplicación.
 * Se usa una única instancia para todo el proceso porque abrir
 * varias conexiones es caro y no aporta nada en una app tan pequeña.
 */
@Database(
    entities = [
        OpportunityEntity::class,
        UsuarioEntity::class,
        OrganizacionEntity::class,
        ContactoEntity::class,
        EventoEntity::class,
        GestionEntity::class,
        HistorialCambioEntity::class,
        ActivacionEntity::class,
        EvidenciaEntity::class,
        ResultadoEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun opportunityDao(): OpportunityDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun organizacionDao(): OrganizacionDao
    abstract fun contactoDao(): ContactoDao
    abstract fun eventoDao(): EventoDao
    abstract fun gestionDao(): GestionDao
    abstract fun historialCambioDao(): HistorialCambioDao
    abstract fun activacionDao(): ActivacionDao
    abstract fun evidenciaDao(): EvidenciaDao
    abstract fun resultadoDao(): ResultadoDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        // En una instalación nueva Room crea las tablas y este callback
        // inserta los usuarios base; en una actualización los inserta la migración.
        private val seedCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                DatabaseSeed.insertarUsuarios(db)
            }
        }

        // Double-checked locking: @Volatile garantiza que todos vean
        // la instancia recién creada y synchronized evita crear dos a la vez
        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "max.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(seedCallback)
                    .build()
                    .also { instance = it }
            }
    }
}
