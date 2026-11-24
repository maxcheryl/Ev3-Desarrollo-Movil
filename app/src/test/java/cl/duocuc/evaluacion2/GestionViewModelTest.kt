package cl.duocuc.evaluacion2

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GestionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
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

        assertTrue(viewModel.esEjecutivo.value)
        assertEquals(mapOf(1L to "Ejecutivo"), viewModel.usuarios.value)
        viewModel.onDescripcionChange("valor que se debe borrar")
        viewModel.seleccionarOportunidad(3)
        advanceUntilIdle()

        assertEquals(3L, viewModel.historial.value.single().oportunidadId)
        assertEquals(3L, viewModel.gestiones.value.single().oportunidadId)
        assertEquals("", viewModel.editor.value.form.descripcion)
    }

    @Test
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
        assertTrue(noSession.editor.value.mensajeError.orEmpty().contains("sesión"))

        val supervisorSession = SessionManager().apply { iniciar(supervisor()) }
        val supervisorVm = viewModel(repository, history, users, supervisorSession)
        supervisorVm.seleccionarOportunidad(2)
        supervisorVm.beginNueva()
        assertTrue(supervisorVm.editor.value.mensajeError.orEmpty().contains("Solo"))

        val executiveSession = SessionManager().apply { iniciar(ejecutivo()) }
        val executiveVm = viewModel(repository, history, users, executiveSession)
        executiveVm.beginNueva()
        assertTrue(executiveVm.editor.value.mensajeError.orEmpty().contains("oportunidad"))

        executiveVm.seleccionarOportunidad(2)
        coEvery { repository.buscarOportunidad(2) } returns null
        executiveVm.beginNueva()
        advanceUntilIdle()
        assertTrue(executiveVm.editor.value.mensajeError.orEmpty().contains("ya no está"))

        coEvery { repository.buscarOportunidad(2) } returns oportunidad(responsable = 5)
        executiveVm.beginNueva()
        advanceUntilIdle()
        assertTrue(executiveVm.editor.value.mensajeError.orEmpty().contains("permiso"))

        coEvery { repository.buscarOportunidad(2) } returns oportunidad(responsable = 1)
        executiveVm.beginNueva()
        advanceUntilIdle()
        assertEquals(EstadoGestion.CONTACTO_PENDIENTE.name, executiveVm.editor.value.form.estado)
    }

    @Test
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
        assertTrue(noSession.editor.value.mensajeError.orEmpty().contains("sesión"))

        val supervisorVm = viewModel(
            repository,
            history,
            users,
            SessionManager().apply { iniciar(supervisor()) }
        )
        supervisorVm.seleccionarOportunidad(2)
        supervisorVm.guardar {}
        assertTrue(supervisorVm.editor.value.mensajeError.orEmpty().contains("Solo"))

        val executiveVm = viewModel(
            repository,
            history,
            users,
            SessionManager().apply { iniciar(ejecutivo()) }
        )
        executiveVm.guardar {}
        assertTrue(executiveVm.editor.value.mensajeError.orEmpty().contains("oportunidad"))
        executiveVm.seleccionarOportunidad(2)
        executiveVm.guardar {}
        assertTrue(executiveVm.editor.value.errores.hayErrores)
        coVerify(exactly = 0) { repository.registrar(any(), any()) }
    }

    @Test
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
        viewModel.onFechaProximaAccionChange(500)
        var saved = false

        viewModel.guardar { saved = true }
        advanceUntilIdle()

        val captured = slot<GestionEntity>()
        coVerify { repository.registrar(capture(captured), EstadoGestion.CONTACTO_REALIZADO.name) }
        assertEquals("Llamada realizada", captured.captured.descripcion)
        assertEquals("Se presentó propuesta", captured.captured.resultado)
        assertEquals("Enviar correo", captured.captured.proximaAccion)
        assertEquals(1L, captured.captured.usuarioId)
        assertTrue(saved)
        assertEquals("", viewModel.editor.value.form.descripcion)

        coEvery { repository.registrar(any(), any()) } throws IllegalStateException("duplicado")
        viewModel.onTipoChange(TipoGestion.GESTION.name)
        viewModel.onEstadoChange(EstadoGestion.CONTACTO_REALIZADO.name)
        viewModel.onDescripcionChange("Seguimiento")
        viewModel.guardar {}
        advanceUntilIdle()
        assertTrue(viewModel.editor.value.mensajeError.orEmpty().contains("duplicado"))
        assertFalse(viewModel.editor.value.guardando)
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
