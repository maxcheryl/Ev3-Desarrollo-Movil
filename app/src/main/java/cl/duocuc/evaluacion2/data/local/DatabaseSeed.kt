package cl.duocuc.evaluacion2.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import cl.duocuc.evaluacion2.data.model.PerfilUsuario

object DatabaseSeed {

    const val USUARIO_DALI_ID = 1L
    const val USUARIO_DALI_NOMBRE = "Dalí Morales"
    const val USUARIO_DALI_CORREO = "dali.morales@max.com"
    const val USUARIO_DALI_CLAVE = "dali123"

    const val USUARIO_MAXINE_ID = 2L
    const val USUARIO_MAXINE_NOMBRE = "Maxine Moreno"
    const val USUARIO_MAXINE_CORREO = "maxine.moreno@max.com"
    const val USUARIO_MAXINE_CLAVE = "maxine123"

    fun insertarUsuarios(db: SupportSQLiteDatabase) {
        insertarUsuario(
            db = db,
            id = USUARIO_DALI_ID,
            nombre = USUARIO_DALI_NOMBRE,
            correo = USUARIO_DALI_CORREO,
            clave = USUARIO_DALI_CLAVE,
            perfil = PerfilUsuario.EJECUTIVO_COMERCIAL
        )
        insertarUsuario(
            db = db,
            id = USUARIO_MAXINE_ID,
            nombre = USUARIO_MAXINE_NOMBRE,
            correo = USUARIO_MAXINE_CORREO,
            clave = USUARIO_MAXINE_CLAVE,
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
