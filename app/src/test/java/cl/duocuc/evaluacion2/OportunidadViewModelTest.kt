package cl.duocuc.evaluacion2

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

import cl.duocuc.evaluacion2.data.local.DatabaseSeed
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.PerfilUsuario
import cl.duocuc.evaluacion2.data.model.PotencialComercial
import cl.duocuc.evaluacion2.data.model.Prioridad
import cl.duocuc.evaluacion2.data.model.TipoOportunidad
import cl.duocuc.evaluacion2.data.repository.OportunidadRepository
import cl.duocuc.evaluacion2.data.session.SessionManager
import cl.duocuc.evaluacion2.data.session.UsuarioSesion
import cl.duocuc.evaluacion2.ui.viewmodels.FuentesOportunidad
import cl.duocuc.evaluacion2.ui.viewmodels.OportunidadViewModel
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
class OportunidadViewModelTest : StringSpec({
    val dispatcher = StandardTestDispatcher()

    beforeSpec { Dispatchers.setMain(dispatcher) }
    afterSpec { Dispatchers.resetMain() }

    "Lista se filtra por perfil y actualiza permiso de supervisor" { OportunidadViewModelTestCases().listaSeFiltraPorPerfilYActualizaPermisoDeSupervisor() }
    "Begin editar maneja no encontrada y precarga entidad existente" { OportunidadViewModelTestCases().beginEditarManejaNoEncontradaYPrecargaEntidadExistente() }
    "Supervisor solo puede consultar y usar acciones acotadas" { OportunidadViewModelTestCases().supervisorSoloPuedeConsultarYUsarAccionesAcotadas() }
    "Supervisor modifica prioridad y descarta" { OportunidadViewModelTestCases().supervisorModificaPrioridadYDescarta() }
    "Guardar rechaza sesion ausente y formulario invalido" { OportunidadViewModelTestCases().guardarRechazaSesionAusenteYFormularioInvalido() }
    "Guardar nueva recorta campos asigna responsable y notifica" { OportunidadViewModelTestCases().guardarNuevaRecortaCamposAsignaResponsableYNotifica() }
    "Guardar editada mantiene responsable y reporta duplicado" { OportunidadViewModelTestCases().guardarEditadaMantieneResponsableYReportaDuplicado() }
    "Eliminar informa ausencia error y exito" { OportunidadViewModelTestCases().eliminarInformaAusenciaErrorYExito() }
    "Eliminar rechaza oportunidad de otro responsable" { OportunidadViewModelTestCases().eliminarRechazaOportunidadDeOtroResponsable() }
    "Ejecutivo no puede editar oportunidad ajena" { OportunidadViewModelTestCases().ejecutivoNoPuedeEditarOportunidadAjena() }
})

@OptIn(ExperimentalCoroutinesApi::class)
private class OportunidadViewModelTestCases {


    fun listaSeFiltraPorPerfilYActualizaPermisoDeSupervisor() = runTest {
        val repository = repository()
        val session = SessionManager().apply { iniciar(ejecutivo(1)) }
        val viewModel = OportunidadViewModel(repository, session)
        collectWhileSubscribed(viewModel.oportunidades)
        collectWhileSubscribed(viewModel.esSupervisor)
        advanceUntilIdle()

        (viewModel.oportunidades.value) shouldBe (listOf(oportunidad(10, responsable = 1)))
        (viewModel.esSupervisor.value) shouldBe false

        session.iniciar(supervisor())
        advanceUntilIdle()

        (viewModel.oportunidades.value) shouldBe (listOf(oportunidad(10, 1), oportunidad(11, 2)))
        (viewModel.esSupervisor.value) shouldBe true
    }

    fun beginEditarManejaNoEncontradaYPrecargaEntidadExistente() = runTest {
        val repository = repository()
        val session = SessionManager().apply { iniciar(supervisor()) }
        val viewModel = OportunidadViewModel(repository, session)
        collectWhileSubscribed(viewModel.oportunidades)
        advanceUntilIdle()

        viewModel.beginEditar(999)
        (viewModel.editor.value.mensajeError) shouldBe ("No se encontró la oportunidad solicitada.")

        val existing = oportunidad(10, responsable = 1)
        viewModel.beginEditar(existing.id)
        (viewModel.editor.value.mensajeError) shouldBe
            ("No tienes permiso para editar esta oportunidad.")

        session.iniciar(ejecutivo(1))
        advanceUntilIdle()
        viewModel.beginEditar(existing.id)
        (viewModel.editor.value.form.nombre) shouldBe (existing.nombre)
        (viewModel.editor.value.form.fechaCreacion) shouldBe (existing.fechaCreacion)
        (viewModel.editor.value.form.comuna) shouldBe (existing.comuna)
        (viewModel.editor.value.form.contactoId) shouldBe (existing.contactoId)
        (viewModel.editor.value.mensajeError) shouldBe null
    }

    fun supervisorSoloPuedeConsultarYUsarAccionesAcotadas() = runTest {
        val repository = repository()
        val session = SessionManager().apply { iniciar(supervisor()) }
        val viewModel = OportunidadViewModel(repository, session)
        collectWhileSubscribed(viewModel.oportunidades)
        advanceUntilIdle()

        viewModel.beginNueva()
        (viewModel.editor.value.mensajeError.orEmpty().contains("no puede crear")) shouldBe true
        viewModel.beginEditar(10)
        (viewModel.editor.value.mensajeError.orEmpty().contains("No tienes permiso")) shouldBe true
        viewModel.guardarOportunidad {}
        (viewModel.editor.value.mensajeError.orEmpty().contains("solo puede consultar")) shouldBe true

        var error: String? = null
        viewModel.eliminarOportunidad(10, {}, { error = it })
        (error.orEmpty().contains("debe descartar")) shouldBe true
        coVerify(exactly = 0) { repository.eliminar(any()) }
        coVerify(exactly = 0) { repository.guardar(any(), any()) }
    }

    fun supervisorModificaPrioridadYDescarta() = runTest {
        val repository = repository()
        val session = SessionManager().apply { iniciar(supervisor()) }
        val viewModel = OportunidadViewModel(repository, session)
        val existing = oportunidad(10, responsable = 1)
        var current = existing
        val saved = slot<OpportunityEntity>()
        coEvery { repository.buscarPorId(10) } answers { current }
        coEvery { repository.guardar(capture(saved), 99) } answers {
            current = saved.captured
            current.id
        }

        var error: String? = null
        var completado = false
        viewModel.cambiarPrioridad(
            id = 10,
            nuevaPrioridad = Prioridad.ALTA.name,
            onListo = { completado = true },
            onError = { error = it }
        )
        advanceUntilIdle()

        (current.prioridad) shouldBe Prioridad.ALTA.name
        (completado) shouldBe true
        (error) shouldBe null

        viewModel.descartarOportunidad(
            id = 10,
            onListo = { completado = true },
            onError = { error = it }
        )
        advanceUntilIdle()

        (current.estado) shouldBe EstadoGestion.OPORTUNIDAD_DESCARTADA.name
        coVerify(exactly = 2) { repository.guardar(any(), 99) }
    }

    fun guardarRechazaSesionAusenteYFormularioInvalido() = runTest {
        val repository = repository()
        val noSession = OportunidadViewModel(repository, SessionManager())
        noSession.beginNueva()
        noSession.guardarOportunidad {}
        (noSession.editor.value.mensajeError.orEmpty().contains("sesión")) shouldBe true

        val session = SessionManager().apply { iniciar(ejecutivo(1)) }
        val viewModel = OportunidadViewModel(repository, session)
        viewModel.beginNueva()
        viewModel.guardarOportunidad {}
        (viewModel.editor.value.errores.hayErrores) shouldBe true
        coVerify(exactly = 0) { repository.guardar(any(), any()) }
    }

    fun guardarNuevaRecortaCamposAsignaResponsableYNotifica() = runTest {
        val repository = repository()
        coEvery { repository.guardar(any(), 1) } returns 71
        val session = SessionManager().apply { iniciar(ejecutivo(1)) }
        val viewModel = OportunidadViewModel(repository, session)
        viewModel.beginNueva()
        completarFormulario(viewModel)
        viewModel.onNombreChange("  Feria nueva  ")
        viewModel.onDescripcionChange("  Descripción detallada  ")
        viewModel.onObservacionesChange("  Nota  ")
        viewModel.onPublicoEsperadoChange("  120 asistentes  ")
        viewModel.onComunaChange("  Santiago  ")
        viewModel.onFuenteChange(FuentesOportunidad.opciones.first())
        val fechaIdentificacion = viewModel.editor.value.form.fechaCreacion
        var idGuardado: Long? = null

        viewModel.guardarOportunidad { idGuardado = it }
        advanceUntilIdle()

        val saved = slot<OpportunityEntity>()
        coVerify { repository.guardar(capture(saved), 1) }
        (saved.captured.nombre) shouldBe ("Feria nueva")
        (saved.captured.descripcion) shouldBe ("Descripción detallada")
        (saved.captured.observaciones) shouldBe ("Nota")
        (saved.captured.publicoEsperado) shouldBe ("120 asistentes")
        (saved.captured.comuna) shouldBe ("Santiago")
        (saved.captured.fechaCreacion) shouldBe (fechaIdentificacion)
        (saved.captured.responsableId) shouldBe (1L)
        (idGuardado) shouldBe (71L)
        (viewModel.editor.value.mensajeError) shouldBe null
    }

    fun guardarEditadaMantieneResponsableYReportaDuplicado() = runTest {
        val repository = repository()
        val session = SessionManager().apply { iniciar(ejecutivo(1)) }
        val viewModel = OportunidadViewModel(repository, session)
        collectWhileSubscribed(viewModel.oportunidades)
        advanceUntilIdle()
        viewModel.beginEditar(10)
        viewModel.onNombreChange("  Feria editada  ")
        coEvery { repository.guardar(any(), 1) } returns 10
        var callbackId: Long? = null

        viewModel.guardarOportunidad { callbackId = it }
        advanceUntilIdle()

        val saved = slot<OpportunityEntity>()
        coVerify { repository.guardar(capture(saved), 1) }
        (saved.captured.nombre) shouldBe ("Feria editada")
        (saved.captured.responsableId) shouldBe (1L)
        (callbackId) shouldBe (10L)

        viewModel.beginNueva()
        completarFormulario(viewModel)
        coEvery { repository.guardar(any(), 1) } throws IllegalStateException("UNIQUE constraint")
        viewModel.guardarOportunidad {}
        advanceUntilIdle()
        (viewModel.editor.value.mensajeError.orEmpty().contains("UNIQUE constraint")) shouldBe true
        (viewModel.editor.value.guardando) shouldBe false
    }

    fun eliminarInformaAusenciaErrorYExito() = runTest {
        val repository = repository()
        val session = SessionManager().apply { iniciar(ejecutivo(1)) }
        val viewModel = OportunidadViewModel(repository, session)
        collectWhileSubscribed(viewModel.oportunidades)
        advanceUntilIdle()

        var error: String? = null
        viewModel.eliminarOportunidad(999, {}, { error = it })
        (error) shouldBe ("No se encontró la oportunidad para eliminarla.")

        coEvery { repository.eliminar(10) } returns 0
        viewModel.eliminarOportunidad(10, {}, { error = it })
        advanceUntilIdle()
        (error) shouldBe ("No se encontró la oportunidad para eliminarla.")

        coEvery { repository.eliminar(10) } returns 1
        var deleted = false
        viewModel.eliminarOportunidad(10, { deleted = true }, {})
        advanceUntilIdle()
        (deleted) shouldBe true

        coEvery { repository.eliminar(10) } throws IllegalStateException("DB falla")
        viewModel.eliminarOportunidad(10, {}, { error = it })
        advanceUntilIdle()
        (error.orEmpty().contains("DB falla")) shouldBe true
    }

    fun eliminarRechazaOportunidadDeOtroResponsable() = runTest {
        val repository = repository()
        val session = SessionManager().apply { iniciar(supervisor()) }
        val viewModel = OportunidadViewModel(repository, session)
        collectWhileSubscribed(viewModel.oportunidades)
        advanceUntilIdle()

        var supervisorError: String? = null
        viewModel.eliminarOportunidad(10, {}, { supervisorError = it })
        (supervisorError.orEmpty().contains("debe descartar")) shouldBe true
        coVerify(exactly = 0) { repository.eliminar(10) }

        session.iniciar(ejecutivo(1))
        advanceUntilIdle()

        var error: String? = null
        viewModel.eliminarOportunidad(11, {}, { error = it })

        (error) shouldBe ("No se encontró la oportunidad para eliminarla.")
        coVerify(exactly = 0) { repository.eliminar(11) }
    }

    fun ejecutivoNoPuedeEditarOportunidadAjena() = runTest {
        val repository = repository()
        val session = SessionManager().apply { iniciar(supervisor()) }
        val viewModel = OportunidadViewModel(repository, session)
        collectWhileSubscribed(viewModel.oportunidades)
        advanceUntilIdle()

        viewModel.beginEditar(11)
        session.iniciar(ejecutivo(1))
        advanceUntilIdle()
        viewModel.beginEditar(11)

        (viewModel.editor.value.mensajeError) shouldBe
            ("No se encontró la oportunidad solicitada.")
        viewModel.guardarOportunidad {}
        (viewModel.editor.value.mensajeError) shouldBe
            ("No se encontró la oportunidad solicitada.")
        coVerify(exactly = 0) { repository.guardar(any(), any()) }
    }

    private fun repository(): OportunidadRepository {
        val repository = mockk<OportunidadRepository>(relaxed = true)
        every { repository.observeAll() } returns flowOf(
            listOf(oportunidad(10, 1), oportunidad(11, 2))
        )
        coEvery { repository.semillarSiVacia() } returns Unit
        coEvery { repository.asignarResponsablesPendientes(DatabaseSeed.USUARIO_DALI_ID) } returns 0
        coEvery { repository.completarHistorialCreacion(DatabaseSeed.USUARIO_DALI_ID) } returns Unit
        return repository
    }

    private fun completarFormulario(viewModel: OportunidadViewModel) {
        viewModel.onNombreChange("Feria de bienestar 2026")
        viewModel.onTipoChange(TipoOportunidad.FERIA_EMPRESARIAL.name)
        viewModel.onDescripcionChange("Feria corporativa con beneficios.")
        viewModel.onFuenteChange(FuentesOportunidad.opciones.first())
        viewModel.onPrioridadChange(Prioridad.ALTA.name)
        viewModel.onPotencialComercialChange(PotencialComercial.ALTO.name)
        viewModel.onEstadoChange(EstadoGestion.EVENTO_IDENTIFICADO.name)
        viewModel.onComunaChange("Santiago")
    }

    private fun ejecutivo(id: Long) = UsuarioSesion(
        id = id,
        nombre = "Ejecutivo",
        correo = "ejecutivo$id@example.com",
        perfil = PerfilUsuario.EJECUTIVO_COMERCIAL
    )

    private fun supervisor() = UsuarioSesion(
        id = 99,
        nombre = "Supervisora",
        correo = "supervisor@example.com",
        perfil = PerfilUsuario.SUPERVISOR_COMERCIAL
    )

    private fun oportunidad(id: Long, responsable: Long) = OpportunityEntity(
        id = id,
        nombre = "Oportunidad $id",
        tipo = TipoOportunidad.FERIA_EMPRESARIAL.name,
        descripcion = "Descripción",
        fuente = FuentesOportunidad.opciones.first(),
        prioridad = Prioridad.MEDIA.name,
        potencialComercial = PotencialComercial.ALTO.name,
        estado = EstadoGestion.EVENTO_IDENTIFICADO.name,
        responsableId = responsable,
        comuna = "Santiago",
        fechaCreacion = id * 100,
        fechaActualizacion = id * 100 + 10,
        contactoId = 5
    )
}
