package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.local.UsuarioDao
import cl.duocuc.evaluacion2.data.local.UsuarioEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsuarioRepositoryTest {

    @Test
    fun autenticarNormalizaCorreoPeroComparaClaveExactamente() = runBlocking {
        val dao = mockk<UsuarioDao>()
        val usuario = UsuarioEntity(
            id = 6,
            nombre = "Ana",
            correo = "ana@example.com",
            clave = "Clave con espacios",
            perfil = "EJECUTIVO_COMERCIAL"
        )
        coEvery { dao.buscarPorCorreo("ana@example.com") } returns usuario
        val repository = UsuarioRepository(dao)

        assertEquals(usuario, repository.autenticar("  ANA@EXAMPLE.COM ", "Clave con espacios"))
        assertNull(repository.autenticar("ana@example.com", " Clave con espacios "))
        coVerify(exactly = 2) { dao.buscarPorCorreo("ana@example.com") }
    }

    @Test
    fun correoNoEncontradoDevuelveNull() = runBlocking {
        val dao = mockk<UsuarioDao>()
        coEvery { dao.buscarPorCorreo("missing@example.com") } returns null

        assertNull(UsuarioRepository(dao).autenticar("missing@example.com", "secret"))
    }
}
