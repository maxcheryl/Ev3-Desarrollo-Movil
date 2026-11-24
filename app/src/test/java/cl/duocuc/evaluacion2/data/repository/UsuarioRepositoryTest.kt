package cl.duocuc.evaluacion2.data.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import cl.duocuc.evaluacion2.data.local.UsuarioDao
import cl.duocuc.evaluacion2.data.local.UsuarioEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class UsuarioRepositoryTest : StringSpec({
    "Autenticar normaliza correo pero compara clave exactamente" { UsuarioRepositoryTestCases().autenticarNormalizaCorreoPeroComparaClaveExactamente() }
    "Correo no encontrado devuelve null" { UsuarioRepositoryTestCases().correoNoEncontradoDevuelveNull() }
})

private class UsuarioRepositoryTestCases {

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

        (repository.autenticar("  ANA@EXAMPLE.COM ", "Clave con espacios")) shouldBe (usuario)
        (repository.autenticar("ana@example.com", " Clave con espacios ")) shouldBe null
        coVerify(exactly = 2) { dao.buscarPorCorreo("ana@example.com") }
    }

    fun correoNoEncontradoDevuelveNull() = runBlocking {
        val dao = mockk<UsuarioDao>()
        coEvery { dao.buscarPorCorreo("missing@example.com") } returns null

        (UsuarioRepository(dao).autenticar("missing@example.com", "secret")) shouldBe null
    }
}
