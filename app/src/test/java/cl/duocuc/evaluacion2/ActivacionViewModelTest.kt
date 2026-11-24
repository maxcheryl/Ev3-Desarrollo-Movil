package cl.duocuc.evaluacion2

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.doubles.plusOrMinus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

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

@OptIn(ExperimentalCoroutinesApi::class)
class ActivacionViewModelTest : StringSpec({
    val dispatcher = StandardTestDispatcher()

    beforeSpec { Dispatchers.setMain(dispatcher) }
    afterSpec { Dispatchers.resetMain() }

    "Cargar mapea datos defaults resultado final y es idempotente" { ActivacionViewModelTestCases().cargarMapeaDatosDefaultsResultadoFinalYEsIdempotente() }
    "Cargar usa responsable predeterminado y estado final de la oportunidad" { ActivacionViewModelTestCases().cargarUsaResponsablePredeterminadoYEstadoFinalDeLaOportunidad() }
    "Cargar informa oportunidad ausente y excepciones" { ActivacionViewModelTestCases().cargarInformaOportunidadAusenteYExcepciones() }
    "Costo vacio y con coma son validos mientras negativo ono numerico se rechazan" { ActivacionViewModelTestCases().costoVacioYConComaSonValidosMientrasNegativoONoNumericoSeRechazan() }
    "Guardar coordinacion valida sesion responsable y recorta campos" { ActivacionViewModelTestCases().guardarCoordinacionValidaSesionResponsableYRecortaCampos() }
    "Agrega evidencia exige activacion y sesion y actualiza lista" { ActivacionViewModelTestCases().agregaEvidenciaExigeActivacionYSesionYActualizaLista() }
    "Guardar resultado valida estado sesion y activacion y actualiza resultado" { ActivacionViewModelTestCases().guardarResultadoValidaEstadoSesionYActivacionYActualizaResultado() }
})

@OptIn(ExperimentalCoroutinesApi::class)
private class ActivacionViewModelTestCases {


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
                resultado = EstadoGestion.ACTIVACION_REALIZADA.name,
                observaciones = "Asistieron",
                impacto = "Alto",
                fechaRealizacion = 900,
                asistentesEstimados = 120,
                contactosGenerados = 20,
                personasInteresadas = 12,
                inscripciones = 8
            )
        )
        val viewModel = ActivacionViewModel(repository, SessionManager())
        advanceUntilIdle()

        viewModel.cargar(4)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        (state.oportunidadNombre) shouldBe ("Feria")
        (state.activacionId) shouldBe (21L)
        (state.evidencias) shouldBe (listOf(evidencia()))
        (state.resultadoGuardado?.id) shouldBe (30L)
        (state.participacionConfirmada) shouldBe true
        (state.responsableId) shouldBe (7L)
        (state.costo) shouldBe ("25.5")
        (state.requisitos) shouldBe ("Toldo")
        (state.observaciones) shouldBe ("Asistieron")
        (state.impacto) shouldBe ("Alto")
        (state.cargando) shouldBe false

        viewModel.cargar(4)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.obtenerOportunidad(4) }
    }

    fun cargarUsaResponsablePredeterminadoYEstadoFinalDeLaOportunidad() = runTest {
        val repository = repository(
            opportunity = oportunidad(EstadoGestion.ACTIVACION_CANCELADA.name),
            activation = null,
            result = null
        )
        val viewModel = ActivacionViewModel(repository, SessionManager())
        advanceUntilIdle()

        viewModel.cargar(4)
        advanceUntilIdle()

        (viewModel.uiState.value.responsableId) shouldBe (8L)
        (viewModel.uiState.value.resultadoFinal) shouldBe (EstadoGestion.ACTIVACION_CANCELADA.name)
        (viewModel.uiState.value.participacionConfirmada) shouldBe false
    }

    fun cargarInformaOportunidadAusenteYExcepciones() = runTest {
        val repository = repository(opportunity = null)
        val viewModel = ActivacionViewModel(repository, SessionManager())
        advanceUntilIdle()

        viewModel.cargar(4)
        advanceUntilIdle()
        (viewModel.uiState.value.mensajeError.orEmpty().contains("ya no está disponible")) shouldBe true
        (viewModel.uiState.value.cargando) shouldBe false

        coEvery { repository.obtenerOportunidad(5) } throws IllegalStateException("DB falla")
        viewModel.cargar(5)
        advanceUntilIdle()
        (viewModel.uiState.value.mensajeError.orEmpty().contains("DB falla")) shouldBe true
    }

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
        (savedCosts[0].costo) shouldBe null
        (savedCosts[1].costo!!) shouldBe (12.75 plusOrMinus 0.001)

        viewModel.onCostoChange("-1")
        viewModel.guardarCoordinacion()
        (viewModel.uiState.value.mensajeError.orEmpty().contains("mayor que cero")) shouldBe true

        viewModel.onCostoChange("abc")
        viewModel.guardarCoordinacion()
        (viewModel.uiState.value.mensajeError.orEmpty().contains("número")) shouldBe true
    }

    fun guardarCoordinacionValidaSesionResponsableYRecortaCampos() = runTest {
        val noSessionRepository = repository()
        val noSession = ActivacionViewModel(noSessionRepository, SessionManager())
        advanceUntilIdle()
        noSession.cargar(4)
        advanceUntilIdle()
        noSession.guardarCoordinacion()
        (noSession.uiState.value.mensajeError.orEmpty().contains("sesión")) shouldBe true

        val repo = repository(users = emptyList())
        val viewModel = ActivacionViewModel(repo, SessionManager().apply { iniciar(ejecutivo()) })
        advanceUntilIdle()
        viewModel.cargar(4)
        advanceUntilIdle()
        viewModel.guardarCoordinacion()
        (viewModel.uiState.value.mensajeError.orEmpty().contains("responsable")) shouldBe true

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
        (saved.captured.espacio) shouldBe ("Plaza")
        (saved.captured.requisitos) shouldBe ("Permiso municipal")
        (validVm.uiState.value.activacionId) shouldBe (55L)
        (validVm.uiState.value.mensajeExito) shouldBe ("Coordinación guardada.")
    }

    fun agregaEvidenciaExigeActivacionYSesionYActualizaLista() = runTest {
        val noActivationRepo = repository()
        val noActivationVm = ActivacionViewModel(noActivationRepo, SessionManager().apply { iniciar(ejecutivo()) })
        advanceUntilIdle()
        noActivationVm.agregarEvidencia("FOTO", "stand.jpg", "content://stand")
        (noActivationVm.uiState.value.mensajeError.orEmpty().contains("Guarda la coordinación")) shouldBe true

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

        (viewModel.uiState.value.evidencias) shouldBe (listOf(evidence))
        (viewModel.uiState.value.mensajeExito) shouldBe ("Evidencia adjunta.")
    }

    fun guardarResultadoValidaEstadoSesionYActivacionYActualizaResultado() = runTest {
        val noActivation = ActivacionViewModel(repository(), SessionManager())
        noActivation.guardarResultado()
        (noActivation.uiState.value.mensajeError.orEmpty().contains("Guarda la coordinación")) shouldBe true

        val repository = repository(
            activation = ActivacionEntity(id = 21, oportunidadId = 4, responsableId = 1)
        )
        val viewModel = ActivacionViewModel(repository, SessionManager())
        advanceUntilIdle()
        viewModel.cargar(4)
        advanceUntilIdle()
        viewModel.guardarResultado()
        (viewModel.uiState.value.mensajeError.orEmpty().contains("sesión")) shouldBe true

        val session = SessionManager().apply { iniciar(ejecutivo()) }
        val validVm = ActivacionViewModel(repository, session)
        advanceUntilIdle()
        validVm.cargar(4)
        advanceUntilIdle()
        validVm.onResultadoFinalChange("INVALIDO")
        validVm.guardarResultado()
        (validVm.uiState.value.mensajeError.orEmpty().contains("resultado final")) shouldBe true

        coEvery {
            repository.guardarResultado(
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any()
            )
        } returns 77
        validVm.onResultadoFinalChange(EstadoGestion.ACTIVACION_REALIZADA.name)
        validVm.onObservacionesChange("  Confirmada  ")
        validVm.onImpactoChange("  Positivo  ")
        validVm.onFechaRealizacionChange(500)
        validVm.onAsistentesEstimadosChange("120")
        validVm.onContactosGeneradosChange("-1")
        validVm.guardarResultado()
        (validVm.uiState.value.mensajeError.orEmpty().contains("mayores que cero")) shouldBe true
        validVm.onContactosGeneradosChange("30")
        validVm.onPersonasInteresadasChange("20")
        validVm.onInscripcionesChange("12")
        validVm.guardarResultado()
        advanceUntilIdle()

        (validVm.uiState.value.resultadoGuardado?.resultado) shouldBe (EstadoGestion.ACTIVACION_REALIZADA.name)
        (validVm.uiState.value.resultadoGuardado?.observaciones) shouldBe ("Confirmada")
        (validVm.uiState.value.resultadoGuardado?.asistentesEstimados) shouldBe (120)
        (validVm.uiState.value.resultadoGuardado?.contactosGenerados) shouldBe (30)
        (validVm.uiState.value.resultadoGuardado?.personasInteresadas) shouldBe (20)
        (validVm.uiState.value.resultadoGuardado?.inscripciones) shouldBe (12)
        (validVm.uiState.value.participacionConfirmada) shouldBe true
        (validVm.uiState.value.mensajeExito) shouldBe ("Resultado de activación guardado.")
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
        coEvery {
            repository.guardarResultado(
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any()
            )
        } returns 77
        return repository
    }

    private fun oportunidad(
        estado: String = EstadoGestion.PARTICIPACION_CONFIRMADA.name
    ) = OpportunityEntity(
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
