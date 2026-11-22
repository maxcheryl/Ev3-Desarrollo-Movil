package cl.duocuc.evaluacion2.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import cl.duocuc.evaluacion2.data.model.PerfilUsuario

object DatabaseSeed {

    const val USUARIO_DALI_ID = 1L
    const val USUARIO_MAXINE_ID = 2L

    fun insertarUsuarios(db: SupportSQLiteDatabase) {
        insertarUsuario(
            db = db,
            id = USUARIO_DALI_ID,
            nombre = "Dalí Morales",
            correo = "dali.morales@max.com",
            clave = "dali123",
            perfil = PerfilUsuario.EJECUTIVO_COMERCIAL
        )
        insertarUsuario(
            db = db,
            id = USUARIO_MAXINE_ID,
            nombre = "Maxine Moreno",
            correo = "maxine.moreno@max.com",
            clave = "maxine123",
            perfil = PerfilUsuario.SUPERVISOR_COMERCIAL
        )
    }

    private fun insertarUsuario(
        db: SupportSQLiteDatabase,
        id: Long,
        nombre: String,
        correo: String,
        clave: String,
        perfil: PerfilUsuario
    ) {
        db.execSQL(
            "INSERT OR IGNORE INTO usuarios (id, nombre, correo, clave, perfil) VALUES (?, ?, ?, ?, ?)",
            arrayOf(id, nombre, correo, clave, perfil.name)
        )
    }
}
