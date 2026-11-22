package cl.duocuc.evaluacion2.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE oportunidades ADD COLUMN eventoId INTEGER")
        db.execSQL("ALTER TABLE oportunidades ADD COLUMN organizacionId INTEGER")
        db.execSQL("ALTER TABLE oportunidades ADD COLUMN contactoId INTEGER")
        db.execSQL("ALTER TABLE oportunidades ADD COLUMN responsableId INTEGER")
        db.execSQL("ALTER TABLE oportunidades ADD COLUMN publicoEsperado TEXT NOT NULL DEFAULT ''")

        db.execSQL(
            "UPDATE oportunidades SET estado = 'PARTICIPACION_CONFIRMADA' " +
                "WHERE estado = 'ACTIVACION_REALIZADA'"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nombre TEXT NOT NULL,
                correo TEXT NOT NULL,
                clave TEXT NOT NULL,
                perfil TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS organizaciones (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nombre TEXT NOT NULL,
                tipo TEXT NOT NULL,
                comuna TEXT NOT NULL,
                sitioWeb TEXT NOT NULL,
                observaciones TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS contactos (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nombre TEXT NOT NULL,
                organizacionId INTEGER NOT NULL,
                cargo TEXT NOT NULL,
                area TEXT NOT NULL,
                correo TEXT NOT NULL,
                telefono TEXT NOT NULL,
                observaciones TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS eventos (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nombre TEXT NOT NULL,
                fecha INTEGER NOT NULL,
                comuna TEXT NOT NULL,
                organizacionId INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS gestiones (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                oportunidadId INTEGER NOT NULL,
                usuarioId INTEGER NOT NULL,
                tipo TEXT NOT NULL,
                descripcion TEXT NOT NULL,
                resultado TEXT NOT NULL,
                proximaAccion TEXT NOT NULL,
                fechaProximaAccion INTEGER,
                fecha INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS historial_cambios (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                oportunidadId INTEGER NOT NULL,
                usuarioId INTEGER NOT NULL,
                tipoAccion TEXT NOT NULL,
                descripcion TEXT NOT NULL,
                fecha INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS activaciones (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                oportunidadId INTEGER NOT NULL,
                participacionConfirmada INTEGER NOT NULL,
                responsableId INTEGER NOT NULL,
                espacio TEXT NOT NULL,
                fecha INTEGER,
                costo REAL,
                requisitos TEXT NOT NULL,
                fechaCreacion INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS evidencias (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                activacionId INTEGER NOT NULL,
                tipo TEXT NOT NULL,
                nombre TEXT NOT NULL,
                ruta TEXT NOT NULL,
                fecha INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS resultados (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                activacionId INTEGER NOT NULL,
                resultado TEXT NOT NULL,
                observaciones TEXT NOT NULL,
                impacto TEXT NOT NULL,
                fecha INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_usuarios_correo ON usuarios (correo)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_organizaciones_nombre ON organizaciones (nombre)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_contactos_organizacionId ON contactos (organizacionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_eventos_organizacionId ON eventos (organizacionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_gestiones_oportunidadId ON gestiones (oportunidadId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_gestiones_usuarioId ON gestiones (usuarioId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_historial_cambios_oportunidadId ON historial_cambios (oportunidadId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_historial_cambios_usuarioId ON historial_cambios (usuarioId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_activaciones_oportunidadId ON activaciones (oportunidadId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_activaciones_responsableId ON activaciones (responsableId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_evidencias_activacionId ON evidencias (activacionId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_resultados_activacionId ON resultados (activacionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_oportunidades_eventoId ON oportunidades (eventoId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_oportunidades_organizacionId ON oportunidades (organizacionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_oportunidades_contactoId ON oportunidades (contactoId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_oportunidades_responsableId ON oportunidades (responsableId)")

        db.execSQL("UPDATE oportunidades SET responsableId = 1 WHERE responsableId IS NULL")
        DatabaseSeed.insertarUsuarios(db)
    }
}
