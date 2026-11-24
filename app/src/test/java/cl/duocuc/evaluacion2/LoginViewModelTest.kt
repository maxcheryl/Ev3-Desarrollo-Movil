package cl.duocuc.evaluacion2

import cl.duocuc.evaluacion2.data.local.UsuarioEntity
import cl.duocuc.evaluacion2.data.model.PerfilUsuario
import cl.duocuc.evaluacion2.data.repository.UsuarioRepository
import cl.duocuc.evaluacion2.data.session.SessionManager
import cl.duocuc.evaluacion2.ui.viewmodels.LoginViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun camposVaciosImpidenIntentarAutenticar() = runTest {
        val repository = mockk<UsuarioRepository>()
        val viewModel = LoginViewModel(repository, SessionManager())

        viewModel.ingresar {}

        assertNotNull(viewModel.estado.value.correoError)
        assertNotNull(viewModel.estado.value.claveError)
        assertNull(viewModel.estado.value.mensajeError)
        coVerify(exactly = 0) { repository.autenticar(any(), any()) }
    }

    @Test
    fun credencialesIncorrectasMuestranError() = runTest {
        val repository = mockk<UsuarioRepository>()
        coEvery { repository.autenticar("ana@example.com", "bad") } returns null
        val viewModel = LoginViewModel(repository, SessionManager())
        viewModel.onCorreoChange("ana@example.com")
        viewModel.onClaveChange("bad")

        viewModel.ingresar {}
        advanceUntilIdle()

        assertEquals("Correo o contraseña incorrectos.", viewModel.estado.value.mensajeError)
        assertFalse(viewModel.estado.value.cargando)
    }

    @Test
    fun excepcionDelRepositorioSeInforma() = runTest {
        val repository = mockk<UsuarioRepository>()
        coEvery { repository.autenticar(any(), any()) } throws IllegalStateException("DB falla")
        val viewModel = LoginViewModel(repository, SessionManager())
        viewModel.onCorreoChange("ana@example.com")
        viewModel.onClaveChange("secret")

        viewModel.ingresar {}
        advanceUntilIdle()

        assertEquals("No se pudo iniciar sesión: DB falla", viewModel.estado.value.mensajeError)
        assertFalse(viewModel.estado.value.cargando)
    }

    @Test
    fun ingresoExitosoIniciaSesionYRestableceFormulario() = runTest {
        val repository = mockk<UsuarioRepository>()
        val session = SessionManager()
        val usuario = UsuarioEntity(
            id = 6,
            nombre = "Ana",
            correo = "ana@example.com",
            clave = "secret",
            perfil = "Ejecutivo comercial"
        )
        coEvery { repository.autenticar("ana@example.com", "secret") } returns usuario
        val viewModel = LoginViewModel(repository, session)
        viewModel.onCorreoChange("ana@example.com")
        viewModel.onClaveChange("secret")
        var exitoso = false

        viewModel.ingresar { exitoso = true }
        advanceUntilIdle()

        assertEquals(6L, session.actual?.id)
        assertEquals(PerfilUsuario.EJECUTIVO_COMERCIAL, session.actual?.perfil)
        assertTrue(exitoso)
        assertEquals("", viewModel.estado.value.correo)
        assertEquals("", viewModel.estado.value.clave)
    }

    @Test
    fun cerrarSesionLimpiaSesionYFormulario() = runTest {
        val session = SessionManager()
        session.iniciar(
            cl.duocuc.evaluacion2.data.session.UsuarioSesion(
                id = 7,
                nombre = "Ana",
                correo = "ana@example.com",
                perfil = PerfilUsuario.EJECUTIVO_COMERCIAL
            )
        )
        val viewModel = LoginViewModel(mockk(), session)
        viewModel.onCorreoChange("ana@example.com")
        viewModel.onClaveChange("secret")

        viewModel.cerrarSesion()

        assertNull(session.actual)
        assertEquals("", viewModel.estado.value.correo)
        assertEquals("", viewModel.estado.value.clave)
    }
}
