package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.local.UsuarioDao
import cl.duocuc.evaluacion2.data.local.UsuarioEntity

class UsuarioRepository(private val dao: UsuarioDao) {

    suspend fun autenticar(correo: String, clave: String): UsuarioEntity? {
        val encontrado = dao.buscarPorCorreo(correo.trim().lowercase()) ?: return null
        return encontrado.takeIf { it.clave == clave }
    }
}
