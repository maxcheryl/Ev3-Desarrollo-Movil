package cl.duocuc.evaluacion2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de datos local de la aplicación.
 * Se comparte una sola instancia mientras la app está abierta.
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
    version = 6,
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

        // Al crear las tablas, agrega los usuarios de demostración.
        private val seedCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                DatabaseSeed.insertarUsuarios(db)
            }
        }

        // Reutiliza la base de datos si ya existe; si no, crea una sola instancia.
        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "max.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(seedCallback)
                    .build()
                    .also { instance = it }
            }
    }
}
