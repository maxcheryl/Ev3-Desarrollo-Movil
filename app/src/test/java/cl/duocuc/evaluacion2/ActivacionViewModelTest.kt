package cl.duocuc.evaluacion2

import cl.duocuc.evaluacion2.data.local.ActivacionEntity
import cl.duocuc.evaluacion2.data.local.EvidenciaEntity
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import cl.duocuc.evaluacion2.data.local.ResultadoEntity
import cl.duocuc.evaluacion2.data.local.UsuarioEntity
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.PerfilUsuario
import cl.duocuc.evaluacion2.data.repository.ActivacionRepository
import cl.duocuc.evaluacion2.data.session.SessionManager
import cl.duocuc.evaluacion2.data.session.UsuarioSesion
import cl.duocuc.evaluacion2.ui.viewmodels.ActivacionViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivacionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun cargarMapeaDatosDefaultsResultadoFinalYEsIdempotente() = runTest {
        val repository = repository(
            activation = ActivacionEntity(
                id = 21,
                oportunidadId = 4,
                participacionConfirmada = true,
                responsableId = 7,
                espacio = "Plaza",
                fecha = 123,
                costo = 25.5,
                requisitos = "Toldo"
            ),
            evidencias = listOf(evidencia()),
            result = ResultadoEntity(
                id = 30,
                activacionId = 21,
                resultado = EstadoGestion.PARTICIPACION_CONFIRMADA.name,
                observaciones = "Asistieron",
                impacto = "Alto"
            )
        )
        val viewModel = ActivacionViewModel(repository, SessionManager())
        advanceUntilIdle()

        viewModel.cargar(4)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Feria", state.oportunidadNombre)
        assertEquals(21L, state.activacionId)
        assertEquals(listOf(evidencia()), state.evidencias)
        assertEquals(30L, state.resultadoGuardado?.id)
        assertTrue(state.participacionConfirmada)
        assertEquals(7L, state.responsableId)
        assertEquals("25.5", state.costo)
        assertEquals("Toldo", state.requisitos)
        assertEquals("Asistieron", state.observaciones)
        assertEquals("Alto", state.impacto)
        assertFalse(state.cargando)

        viewModel.cargar(4)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.obtenerOportunidad(4) }
    }

    @Test
    fun cargarUsaResponsablePredeterminadoYEstadoFinalDeLaOportunidad() = runTest {
        val repository = repository(
            opportunity = oportunidad(EstadoGestion.OPORTUNIDAD_DESCARTADA.name),
            activation = null,
            result = null
        )
        val viewModel = ActivacionViewModel(repository, SessionManager())
        advanceUntilIdle()

        viewModel.cargar(4)
        advanceUntilIdle()

        assertEquals(8L, viewModel.uiState.value.responsableId)
        assertEquals(EstadoGestion.OPORTUNIDAD_DESCARTADA.name, viewModel.uiState.value.resultadoFinal)
        assertFalse(viewModel.uiState.value.participacionConfirmada)
    }

    @Test
    fun cargarInformaOportunidadAusenteYExcepciones() = runTest {
        val repository = repository(opportunity = null)
        val viewModel = ActivacionViewModel(repository, SessionManager())
        advanceUntilIdle()

        viewModel.cargar(4)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.mensajeError.orEmpty().contains("ya no está disponible"))
        assertFalse(viewModel.uiState.value.cargando)

        coEvery { repository.obtenerOportunidad(5) } throws IllegalStateException("DB falla")
        viewModel.cargar(5)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.mensajeError.orEmpty().contains("DB falla"))
    }

    @Test
    fun costoVacioYConComaSonValidosMientrasNegativoONoNumericoSeRechazan() = runTest {
        val repository = repository()
        val session = SessionManager().apply { iniciar(ejecutivo()) }
        val viewModel = ActivacionViewModel(repository, session)
        advanceUntilIdle()
        viewModel.cargar(4)
        advanceUntilIdle()

        viewModel.onCostoChange("")
        viewModel.guardarCoordinacion()
        advanceUntilIdle()

        viewModel.onCostoChange("12,75")
        viewModel.guardarCoordinacion()
        advanceUntilIdle()
        val savedCosts = mutableListOf<ActivacionEntity>()
        coVerify(exactly = 2) { repository.guardarActivacion(capture(savedCosts), 1) }
        assertNull(savedCosts[0].costo)
        assertEquals(12.75, savedCosts[1].costo!!, 0.001)

        viewModel.onCostoChange("-1")
        viewModel.guardarCoordinacion()
        assertTrue(viewModel.uiState.value.mensajeError.orEmpty().contains("mayor que cero"))

        viewModel.onCostoChange("abc")
        viewModel.guardarCoordinacion()
        assertTrue(viewModel.uiState.value.mensajeError.orEmpty().contains("número"))
    }

    @Test
    fun guardarCoordinacionValidaSesionResponsableYRecortaCampos() = runTest {
        val noSessionRepository = repository()
        val noSession = ActivacionViewModel(noSessionRepository, SessionManager())
        advanceUntilIdle()
        noSession.cargar(4)
        advanceUntilIdle()
        noSession.guardarCoordinacion()
        assertTrue(noSession.uiState.value.mensajeError.orEmpty().contains("sesión"))

        val repo = repository(users = emptyList())
        val viewModel = ActivacionViewModel(repo, SessionManager().apply { iniciar(ejecutivo()) })
        advanceUntilIdle()
        viewModel.cargar(4)
        advanceUntilIdle()
        viewModel.guardarCoordinacion()
        assertTrue(viewModel.uiState.value.mensajeError.orEmpty().contains("responsable"))

        val validRepo = repository()
        coEvery { validRepo.guardarActivacion(any(), 1) } returns 55
        val validVm = ActivacionViewModel(validRepo, SessionManager().apply { iniciar(ejecutivo()) })
        advanceUntilIdle()
        validVm.cargar(4)
        advanceUntilIdle()
        validVm.onEspacioChange("  Plaza  ")
        validVm.onRequisitosChange("  Permiso municipal  ")
        validVm.guardarCoordinacion()
        validVm.guardarCoordinacion()
        advanceUntilIdle()

        val saved = slot<ActivacionEntity>()
        coVerify(exactly = 1) { validRepo.guardarActivacion(capture(saved), 1) }
        assertEquals("Plaza", saved.captured.espacio)
        assertEquals("Permiso municipal", saved.captured.requisitos)
        assertEquals(55L, validVm.uiState.value.activacionId)
        assertEquals("Coordinación guardada.", validVm.uiState.value.mensajeExito)
    }

    @Test
    fun agregaEvidenciaExigeActivacionYSesionYActualizaLista() = runTest {
        val noActivationRepo = repository()
        val noActivationVm = ActivacionViewModel(noActivationRepo, SessionManager().apply { iniciar(ejecutivo()) })
        advanceUntilIdle()
        noActivationVm.agregarEvidencia("FOTO", "stand.jpg", "content://stand")
        assertTrue(noActivationVm.uiState.value.mensajeError.orEmpty().contains("Guarda la coordinación"))

        val evidence = evidencia()
        val repo = repository(
            activation = ActivacionEntity(id = 21, oportunidadId = 4, responsableId = 1),
            evidencias = listOf(evidence)
        )
        coEvery { repo.agregarEvidencia(any(), 1) } returns 33
        val viewModel = ActivacionViewModel(repo, SessionManager().apply { iniciar(ejecutivo()) })
        advanceUntilIdle()
        viewModel.cargar(4)
        advanceUntilIdle()

        viewModel.agregarEvidencia("FOTO", "otra.jpg", "content://otra")
        advanceUntilIdle()

        assertEquals(listOf(evidence), viewModel.uiState.value.evidencias)
        assertEquals("Evidencia adjunta.", viewModel.uiState.value.mensajeExito)
    }

    @Test
    fun guardarResultadoValidaEstadoSesionYActivacionYActualizaResultado() = runTest {
        val noActivation = ActivacionViewModel(repository(), SessionManager())
        noActivation.guardarResultado()
        assertTrue(noActivation.uiState.value.mensajeError.orEmpty().contains("Guarda la coordinación"))

        val repository = repository(
            activation = ActivacionEntity(id = 21, oportunidadId = 4, responsableId = 1)
        )
        val viewModel = ActivacionViewModel(repository, SessionManager())
        advanceUntilIdle()
        viewModel.cargar(4)
        advanceUntilIdle()
        viewModel.guardarResultado()
        assertTrue(viewModel.uiState.value.mensajeError.orEmpty().contains("sesión"))

        val session = SessionManager().apply { iniciar(ejecutivo()) }
        val validVm = ActivacionViewModel(repository, session)
        advanceUntilIdle()
        validVm.cargar(4)
        advanceUntilIdle()
        validVm.onResultadoFinalChange("INVALIDO")
        validVm.guardarResultado()
        assertTrue(validVm.uiState.value.mensajeError.orEmpty().contains("resultado final"))

        coEvery { repository.guardarResultado(any(), any(), any(), any(), any(), any()) } returns 77
        validVm.onResultadoFinalChange(EstadoGestion.PARTICIPACION_CONFIRMADA.name)
        validVm.onObservacionesChange("  Confirmada  ")
        validVm.onImpactoChange("  Positivo  ")
        validVm.guardarResultado()
        advanceUntilIdle()

        assertEquals(EstadoGestion.PARTICIPACION_CONFIRMADA.name, validVm.uiState.value.resultadoGuardado?.resultado)
        assertEquals("Confirmada", validVm.uiState.value.resultadoGuardado?.observaciones)
        assertTrue(validVm.uiState.value.participacionConfirmada)
        assertEquals("Resultado guardado y oportunidad cerrada.", validVm.uiState.value.mensajeExito)
    }

    private fun repository(
        opportunity: OpportunityEntity? = oportunidad(),
        activation: ActivacionEntity? = null,
        evidencias: List<EvidenciaEntity> = emptyList(),
        result: ResultadoEntity? = null,
        users: List<UsuarioEntity> = listOf(usuario(), usuario(1))
    ): ActivacionRepository {
        val repository = mockk<ActivacionRepository>(relaxed = true)
        every { repository.observeUsuarios() } returns flowOf(users)
        coEvery { repository.obtenerOportunidad(any()) } returns opportunity
        coEvery { repository.obtenerActivacion(any()) } returns activation
        coEvery { repository.obtenerEvidencias(any()) } returns evidencias
        coEvery { repository.obtenerResultado(any()) } returns result
        coEvery { repository.guardarActivacion(any(), any()) } returns 55
        coEvery { repository.agregarEvidencia(any(), any()) } returns 33
        coEvery { repository.guardarResultado(any(), any(), any(), any(), any(), any()) } returns 77
        return repository
    }

    private fun oportunidad(estado: String = EstadoGestion.EVENTO_IDENTIFICADO.name) = OpportunityEntity(
        id = 4,
        nombre = "Feria",
        tipo = "FERIA_EMPRESARIAL",
        descripcion = "Descripción",
        fuente = "Referencia",
        prioridad = "MEDIA",
        potencialComercial = "ALTO",
        estado = estado,
        responsableId = 1
    )

    private fun ejecutivo() = UsuarioSesion(
        id = 1,
        nombre = "Ejecutivo",
        correo = "ejecutivo@example.com",
        perfil = PerfilUsuario.EJECUTIVO_COMERCIAL
    )

    private fun usuario(id: Long = 8) = UsuarioEntity(
        id = id,
        nombre = "Responsable $id",
        correo = "responsable$id@example.com",
        clave = "secret",
        perfil = "EJECUTIVO_COMERCIAL"
    )

    private fun evidencia() = EvidenciaEntity(
        id = 25,
        activacionId = 21,
        tipo = "FOTO",
        nombre = "stand.jpg",
        ruta = "content://stand",
        fecha = 100
    )
}
