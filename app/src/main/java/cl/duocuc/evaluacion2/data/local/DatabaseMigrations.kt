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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE oportunidades ADD COLUMN comuna TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE eventos ADD COLUMN tipo TEXT NOT NULL DEFAULT 'EVENTO_CORPORATIVO'")
        db.execSQL("ALTER TABLE eventos ADD COLUMN descripcion TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE eventos ADD COLUMN fechaTermino INTEGER")
        db.execSQL("ALTER TABLE eventos ADD COLUMN horaInicio TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE eventos ADD COLUMN horaTermino TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE eventos ADD COLUMN direccion TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE eventos ADD COLUMN region TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE eventos ADD COLUMN publicoEsperado TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE eventos ADD COLUMN publicoObjetivo TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE eventos ADD COLUMN asistentesEstimados INTEGER")
        db.execSQL("ALTER TABLE eventos ADD COLUMN condicionesParticipacion TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE eventos ADD COLUMN requisitos TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE eventos ADD COLUMN costoReferencial REAL")
        db.execSQL("ALTER TABLE eventos ADD COLUMN espaciosDisponibles TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            "UPDATE eventos SET fechaTermino = fecha " +
                "WHERE fechaTermino IS NULL"
        )

        db.execSQL(
            "ALTER TABLE activaciones ADD COLUMN observacionesCoordinacion " +
                "TEXT NOT NULL DEFAULT ''"
        )
        db.execSQL("ALTER TABLE resultados ADD COLUMN fechaRealizacion INTEGER")
        db.execSQL("ALTER TABLE resultados ADD COLUMN asistentesEstimados INTEGER")
        db.execSQL(
            "ALTER TABLE resultados ADD COLUMN contactosGenerados INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE resultados ADD COLUMN personasInteresadas INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL("ALTER TABLE resultados ADD COLUMN inscripciones INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "UPDATE usuarios SET correo = 'dali.morales@at.com' " +
                "WHERE id = 1 AND correo = 'dali.morales@max.com'"
        )
        db.execSQL(
            "UPDATE usuarios SET correo = 'maxine.moreno@at.com' " +
                "WHERE id = 2 AND correo = 'maxine.moreno@max.com'"
        )
    }
}
