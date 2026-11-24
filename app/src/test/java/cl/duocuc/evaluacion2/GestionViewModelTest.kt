package cl.duocuc.evaluacion2

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

import cl.duocuc.evaluacion2.data.local.GestionEntity
import cl.duocuc.evaluacion2.data.local.HistorialCambioEntity
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import cl.duocuc.evaluacion2.data.local.UsuarioEntity
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.PerfilUsuario
import cl.duocuc.evaluacion2.data.model.TipoGestion
import cl.duocuc.evaluacion2.data.repository.GestionRepository
import cl.duocuc.evaluacion2.data.repository.HistorialRepository
import cl.duocuc.evaluacion2.data.repository.UsuarioRepository
import cl.duocuc.evaluacion2.data.session.SessionManager
import cl.duocuc.evaluacion2.data.session.UsuarioSesion
import cl.duocuc.evaluacion2.ui.viewmodels.GestionViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class GestionViewModelTest : StringSpec({
    val dispatcher = StandardTestDispatcher()

    beforeSpec { Dispatchers.setMain(dispatcher) }
    afterSpec { Dispatchers.resetMain() }

    "Seleccionar oportunidad reinicia formulario y actualiza flujos" { GestionViewModelTestCases().seleccionarOportunidadReiniciaFormularioYActualizaFlujos() }
    "Begin nueva valida sesion perfil oportunidad y responsable" { GestionViewModelTestCases().beginNuevaValidaSesionPerfilOportunidadYResponsable() }
    "Guardar valida permisos y campos antes de registrar" { GestionViewModelTestCases().guardarValidaPermisosYCamposAntesDeRegistrar() }
    "Supervisor registra seguimiento general sin cambiar estado" { GestionViewModelTestCases().supervisorRegistraSeguimientoGeneralSinCambiarEstado() }
    "Guardar recorta datos notifica y muestra error de duplicado" { GestionViewModelTestCases().guardarRecortaDatosNotificaYMuestraErrorDeDuplicado() }
})

@OptIn(ExperimentalCoroutinesApi::class)
private class GestionViewModelTestCases {


    fun seleccionarOportunidadReiniciaFormularioYActualizaFlujos() = runTest {
        val repository = mockk<GestionRepository>(relaxed = true)
        val historialRepository = mockk<HistorialRepository>()
        val userRepository = mockk<UsuarioRepository>()
        val session = SessionManager().apply { iniciar(ejecutivo()) }
        every { repository.observePorOportunidad(2) } returns flowOf(emptyList())
        every { repository.observePorOportunidad(3) } returns flowOf(
            listOf(gestion())
        )
        every { historialRepository.observePorOportunidad(2) } returns flowOf(emptyList())
        every { historialRepository.observePorOportunidad(3) } returns flowOf(listOf(historial()))
        every { userRepository.observeAll() } returns flowOf(listOf(usuario()))
        val viewModel = viewModel(repository, historialRepository, userRepository, session)
        collectWhileSubscribed(viewModel.gestiones)
        collectWhileSubscribed(viewModel.historial)
        collectWhileSubscribed(viewModel.usuarios)
        collectWhileSubscribed(viewModel.esEjecutivo)
        advanceUntilIdle()

        (viewModel.esEjecutivo.value) shouldBe true
        (viewModel.usuarios.value) shouldBe (mapOf(1L to "Ejecutivo"))
        viewModel.onDescripcionChange("valor que se debe borrar")
        viewModel.seleccionarOportunidad(3)
        advanceUntilIdle()

        (viewModel.historial.value.single().oportunidadId) shouldBe (3L)
        (viewModel.gestiones.value.single().oportunidadId) shouldBe (3L)
        (viewModel.editor.value.form.descripcion) shouldBe ("")
    }

    fun beginNuevaValidaSesionPerfilOportunidadYResponsable() = runTest {
        val repository = mockk<GestionRepository>(relaxed = true)
        val history = mockk<HistorialRepository>()
        val users = mockk<UsuarioRepository>()
        every { repository.observePorOportunidad(any()) } returns flowOf(emptyList())
        every { history.observePorOportunidad(any()) } returns flowOf(emptyList())
        every { users.observeAll() } returns flowOf(emptyList())

        val noSession = viewModel(repository, history, users, SessionManager())
        noSession.seleccionarOportunidad(2)
        noSession.beginNueva()
        (noSession.editor.value.mensajeError.orEmpty().contains("sesión")) shouldBe true

        val supervisorSession = SessionManager().apply { iniciar(supervisor()) }
        val supervisorVm = viewModel(repository, history, users, supervisorSession)
        supervisorVm.seleccionarOportunidad(2)
        coEvery { repository.buscarOportunidad(2) } returns oportunidad(responsable = 1)
        supervisorVm.beginNueva()
        advanceUntilIdle()
        (supervisorVm.editor.value.mensajeError) shouldBe null

        val executiveSession = SessionManager().apply { iniciar(ejecutivo()) }
        val executiveVm = viewModel(repository, history, users, executiveSession)
        executiveVm.beginNueva()
        (executiveVm.editor.value.mensajeError.orEmpty().contains("oportunidad")) shouldBe true

        executiveVm.seleccionarOportunidad(2)
        coEvery { repository.buscarOportunidad(2) } returns null
        executiveVm.beginNueva()
        advanceUntilIdle()
        (executiveVm.editor.value.mensajeError.orEmpty().contains("ya no está")) shouldBe true

        coEvery { repository.buscarOportunidad(2) } returns oportunidad(responsable = 5)
        executiveVm.beginNueva()
        advanceUntilIdle()
        (executiveVm.editor.value.mensajeError.orEmpty().contains("permiso")) shouldBe true

        coEvery { repository.buscarOportunidad(2) } returns oportunidad(responsable = 1)
        executiveVm.beginNueva()
        advanceUntilIdle()
        (executiveVm.editor.value.form.estado) shouldBe ("")
    }

    fun guardarValidaPermisosYCamposAntesDeRegistrar() = runTest {
        val repository = mockk<GestionRepository>(relaxed = true)
        val history = mockk<HistorialRepository>()
        val users = mockk<UsuarioRepository>()
        every { repository.observePorOportunidad(any()) } returns flowOf(emptyList())
        every { history.observePorOportunidad(any()) } returns flowOf(emptyList())
        every { users.observeAll() } returns flowOf(emptyList())

        val noSession = viewModel(repository, history, users, SessionManager())
        noSession.seleccionarOportunidad(2)
        noSession.guardar {}
        (noSession.editor.value.mensajeError.orEmpty().contains("sesión")) shouldBe true

        val supervisorVm = viewModel(
            repository,
            history,
            users,
            SessionManager().apply { iniciar(supervisor()) }
        )
        supervisorVm.seleccionarOportunidad(2)
        supervisorVm.guardar {}
        (supervisorVm.editor.value.errores.hayErrores) shouldBe true

        val executiveVm = viewModel(
            repository,
            history,
            users,
            SessionManager().apply { iniciar(ejecutivo()) }
        )
        executiveVm.guardar {}
        (executiveVm.editor.value.mensajeError.orEmpty().contains("oportunidad")) shouldBe true
        executiveVm.seleccionarOportunidad(2)
        executiveVm.guardar {}
        (executiveVm.editor.value.errores.hayErrores) shouldBe true
        coVerify(exactly = 0) { repository.registrar(any(), any()) }
    }

    fun supervisorRegistraSeguimientoGeneralSinCambiarEstado() = runTest {
        val repository = mockk<GestionRepository>(relaxed = true)
        val history = mockk<HistorialRepository>()
        val users = mockk<UsuarioRepository>()
        every { repository.observePorOportunidad(2) } returns flowOf(emptyList())
        every { history.observePorOportunidad(2) } returns flowOf(emptyList())
        every { users.observeAll() } returns flowOf(emptyList())
        coEvery { repository.buscarOportunidad(2) } returns oportunidad(responsable = 1)
        coEvery { repository.registrar(any(), null) } returns 55

        val viewModel = viewModel(
            repository,
            history,
            users,
            SessionManager().apply { iniciar(supervisor()) }
        )
        viewModel.seleccionarOportunidad(2)
        viewModel.beginNueva()
        advanceUntilIdle()
        viewModel.onTipoChange(TipoGestion.GESTION.name)
        viewModel.onResultadoChange("Revisión general")
        viewModel.onEstadoChange(EstadoGestion.CONTACTO_REALIZADO.name)
        var saved = false

        viewModel.guardar { saved = true }
        advanceUntilIdle()

        val captured = slot<GestionEntity>()
        coVerify { repository.registrar(capture(captured), null) }
        (captured.captured.usuarioId) shouldBe (9L)
        (saved) shouldBe true
    }

    fun guardarRecortaDatosNotificaYMuestraErrorDeDuplicado() = runTest {
        val repository = mockk<GestionRepository>(relaxed = true)
        val history = mockk<HistorialRepository>()
        val users = mockk<UsuarioRepository>()
        every { repository.observePorOportunidad(2) } returns flowOf(emptyList())
        every { history.observePorOportunidad(2) } returns flowOf(emptyList())
        every { users.observeAll() } returns flowOf(emptyList())
        coEvery { repository.buscarOportunidad(2) } returns oportunidad(responsable = 1)
        val viewModel = viewModel(
            repository,
            history,
            users,
            SessionManager().apply { iniciar(ejecutivo()) }
        )
        viewModel.seleccionarOportunidad(2)
        viewModel.onTipoChange(TipoGestion.CONTACTO.name)
        viewModel.onEstadoChange(EstadoGestion.CONTACTO_REALIZADO.name)
        viewModel.onDescripcionChange("  Llamada realizada  ")
        viewModel.onResultadoChange("  Se presentó propuesta  ")
        viewModel.onProximaAccionChange("  Enviar correo  ")
        viewModel.onFechaProximaAccionChange(
            (viewModel.editor.value.form.fechaGestion ?: 0L) + 86_400_000L
        )
        var saved = false

        viewModel.guardar { saved = true }
        advanceUntilIdle()

        val captured = slot<GestionEntity>()
        coVerify { repository.registrar(capture(captured), EstadoGestion.CONTACTO_REALIZADO.name) }
        (captured.captured.descripcion) shouldBe ("")
        (captured.captured.resultado) shouldBe ("Se presentó propuesta")
        (captured.captured.proximaAccion) shouldBe ("Enviar correo")
        (captured.captured.usuarioId) shouldBe (1L)
        (saved) shouldBe true
        (viewModel.editor.value.form.descripcion) shouldBe ("")

        coEvery { repository.registrar(any(), any()) } throws IllegalStateException("duplicado")
        viewModel.onTipoChange(TipoGestion.GESTION.name)
        viewModel.onEstadoChange(EstadoGestion.CONTACTO_REALIZADO.name)
        viewModel.onResultadoChange("Seguimiento")
        viewModel.guardar {}
        advanceUntilIdle()
        (viewModel.editor.value.mensajeError.orEmpty().contains("duplicado")) shouldBe true
        (viewModel.editor.value.guardando) shouldBe false
    }

    private fun viewModel(
        repository: GestionRepository,
        history: HistorialRepository,
        users: UsuarioRepository,
        session: SessionManager
    ) = GestionViewModel(repository, history, users, session)

    private fun ejecutivo() = UsuarioSesion(
        id = 1,
        nombre = "Ejecutivo",
        correo = "ejecutivo@example.com",
        perfil = PerfilUsuario.EJECUTIVO_COMERCIAL
    )

    private fun supervisor() = ejecutivo().copy(
        id = 9,
        correo = "supervisor@example.com",
        perfil = PerfilUsuario.SUPERVISOR_COMERCIAL
    )

    private fun usuario() = UsuarioEntity(
        id = 1,
        nombre = "Ejecutivo",
        correo = "ejecutivo@example.com",
        clave = "secret",
        perfil = PerfilUsuario.EJECUTIVO_COMERCIAL.etiqueta
    )

    private fun oportunidad(responsable: Long?) = OpportunityEntity(
        id = 2,
        nombre = "Feria",
        tipo = "FERIA_EMPRESARIAL",
        descripcion = "Descripción",
        fuente = "Referencia",
        prioridad = "MEDIA",
        potencialComercial = "ALTO",
        estado = EstadoGestion.CONTACTO_PENDIENTE.name,
        responsableId = responsable
    )

    private fun gestion() = GestionEntity(
        oportunidadId = 3,
        usuarioId = 1,
        tipo = TipoGestion.CONTACTO.name,
        descripcion = "Llamada"
    )

    private fun historial() = HistorialCambioEntity(
        oportunidadId = 3,
        usuarioId = 1,
        tipoAccion = "GESTION",
        descripcion = "Llamada"
    )
}
