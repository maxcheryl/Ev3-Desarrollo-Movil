package cl.duocuc.evaluacion2

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

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

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest : StringSpec({
    val dispatcher = StandardTestDispatcher()

    beforeSpec { Dispatchers.setMain(dispatcher) }
    afterSpec { Dispatchers.resetMain() }

    "Campos vacios impiden intentar autenticar" { LoginViewModelTestCases().camposVaciosImpidenIntentarAutenticar() }
    "Credenciales incorrectas muestran error" { LoginViewModelTestCases().credencialesIncorrectasMuestranError() }
    "Excepcion del repositorio se informa" { LoginViewModelTestCases().excepcionDelRepositorioSeInforma() }
    "Ingreso exitoso inicia sesion y restablece formulario" { LoginViewModelTestCases().ingresoExitosoIniciaSesionYRestableceFormulario() }
    "Cerrar sesion limpia sesion y formulario" { LoginViewModelTestCases().cerrarSesionLimpiaSesionYFormulario() }
})

@OptIn(ExperimentalCoroutinesApi::class)
private class LoginViewModelTestCases {


    fun camposVaciosImpidenIntentarAutenticar() = runTest {
        val repository = mockk<UsuarioRepository>()
        val viewModel = LoginViewModel(repository, SessionManager())

        viewModel.ingresar {}

        (viewModel.estado.value.correoError) shouldNotBe null
        (viewModel.estado.value.claveError) shouldNotBe null
        (viewModel.estado.value.mensajeError) shouldBe null
        coVerify(exactly = 0) { repository.autenticar(any(), any()) }
    }

    fun credencialesIncorrectasMuestranError() = runTest {
        val repository = mockk<UsuarioRepository>()
        coEvery { repository.autenticar("ana@example.com", "bad") } returns null
        val viewModel = LoginViewModel(repository, SessionManager())
        viewModel.onCorreoChange("ana@example.com")
        viewModel.onClaveChange("bad")

        viewModel.ingresar {}
        advanceUntilIdle()

        (viewModel.estado.value.mensajeError) shouldBe ("Correo o contraseña incorrectos.")
        (viewModel.estado.value.cargando) shouldBe false
    }

    fun excepcionDelRepositorioSeInforma() = runTest {
        val repository = mockk<UsuarioRepository>()
        coEvery { repository.autenticar(any(), any()) } throws IllegalStateException("DB falla")
        val viewModel = LoginViewModel(repository, SessionManager())
        viewModel.onCorreoChange("ana@example.com")
        viewModel.onClaveChange("secret")

        viewModel.ingresar {}
        advanceUntilIdle()

        (viewModel.estado.value.mensajeError) shouldBe ("No se pudo iniciar sesión: DB falla")
        (viewModel.estado.value.cargando) shouldBe false
    }

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

        (session.actual?.id) shouldBe (6L)
        (session.actual?.perfil) shouldBe (PerfilUsuario.EJECUTIVO_COMERCIAL)
        (exitoso) shouldBe true
        (viewModel.estado.value.correo) shouldBe ("")
        (viewModel.estado.value.clave) shouldBe ("")
    }

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

        (session.actual) shouldBe null
        (viewModel.estado.value.correo) shouldBe ("")
        (viewModel.estado.value.clave) shouldBe ("")
    }
}
