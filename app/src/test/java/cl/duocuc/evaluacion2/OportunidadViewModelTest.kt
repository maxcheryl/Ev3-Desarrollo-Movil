package cl.duocuc.evaluacion2

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OportunidadViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun listaSeFiltraPorPerfilYActualizaPermisoDeSupervisor() = runTest {
        val repository = repository()
        val session = SessionManager().apply { iniciar(ejecutivo(1)) }
        val viewModel = OportunidadViewModel(repository, session)
        collectWhileSubscribed(viewModel.oportunidades)
        collectWhileSubscribed(viewModel.esSupervisor)
        advanceUntilIdle()

        assertEquals(listOf(oportunidad(10, responsable = 1)), viewModel.oportunidades.value)
        assertFalse(viewModel.esSupervisor.value)

        session.iniciar(supervisor())
        advanceUntilIdle()

        assertEquals(listOf(oportunidad(10, 1), oportunidad(11, 2)), viewModel.oportunidades.value)
        assertTrue(viewModel.esSupervisor.value)
    }

    @Test
    fun beginEditarManejaNoEncontradaYPrecargaEntidadExistente() = runTest {
        val repository = repository()
        val session = SessionManager().apply { iniciar(supervisor()) }
        val viewModel = OportunidadViewModel(repository, session)
        collectWhileSubscribed(viewModel.oportunidades)
        advanceUntilIdle()

        viewModel.beginEditar(999)
        assertEquals("No se encontró la oportunidad solicitada.", viewModel.editor.value.mensajeError)

        val existing = oportunidad(10, responsable = 1)
        viewModel.beginEditar(existing.id)
        assertEquals(existing.nombre, viewModel.editor.value.form.nombre)
        assertEquals(existing.fechaCreacion, viewModel.editor.value.form.fechaCreacion)
        assertEquals(existing.contactoId, viewModel.editor.value.form.contactoId)
        assertNull(viewModel.editor.value.mensajeError)
    }

    @Test
    fun guardarRechazaSesionAusenteYFormularioInvalido() = runTest {
        val repository = repository()
        val noSession = OportunidadViewModel(repository, SessionManager())
        noSession.beginNueva()
        noSession.guardarOportunidad {}
        assertTrue(noSession.editor.value.mensajeError.orEmpty().contains("sesión"))

        val session = SessionManager().apply { iniciar(ejecutivo(1)) }
        val viewModel = OportunidadViewModel(repository, session)
        viewModel.beginNueva()
        viewModel.guardarOportunidad {}
        assertTrue(viewModel.editor.value.errores.hayErrores)
        coVerify(exactly = 0) { repository.guardar(any(), any()) }
    }

    @Test
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
        viewModel.onFuenteChange(FuentesOportunidad.opciones.first())
        var idGuardado: Long? = null

        viewModel.guardarOportunidad { idGuardado = it }
        advanceUntilIdle()

        val saved = slot<OpportunityEntity>()
        coVerify { repository.guardar(capture(saved), 1) }
        assertEquals("Feria nueva", saved.captured.nombre)
        assertEquals("Descripción detallada", saved.captured.descripcion)
        assertEquals("Nota", saved.captured.observaciones)
        assertEquals(1L, saved.captured.responsableId)
        assertEquals(71L, idGuardado)
        assertNull(viewModel.editor.value.mensajeError)
    }

    @Test
    fun guardarEditadaMantieneResponsableYReportaDuplicado() = runTest {
        val repository = repository()
        val session = SessionManager().apply { iniciar(supervisor()) }
        val viewModel = OportunidadViewModel(repository, session)
        collectWhileSubscribed(viewModel.oportunidades)
        advanceUntilIdle()
        viewModel.beginEditar(10)
        viewModel.onNombreChange("  Feria editada  ")
        coEvery { repository.guardar(any(), 99) } returns 10
        var callbackId: Long? = null

        viewModel.guardarOportunidad { callbackId = it }
        advanceUntilIdle()

        val saved = slot<OpportunityEntity>()
        coVerify { repository.guardar(capture(saved), 99) }
        assertEquals("Feria editada", saved.captured.nombre)
        assertEquals(1L, saved.captured.responsableId)
        assertEquals(10L, callbackId)

        viewModel.beginNueva()
        completarFormulario(viewModel)
        coEvery { repository.guardar(any(), 99) } throws IllegalStateException("UNIQUE constraint")
        viewModel.guardarOportunidad {}
        advanceUntilIdle()
        assertTrue(viewModel.editor.value.mensajeError.orEmpty().contains("UNIQUE constraint"))
        assertFalse(viewModel.editor.value.guardando)
    }

    @Test
    fun eliminarInformaAusenciaErrorYExito() = runTest {
        val repository = repository()
        val session = SessionManager().apply { iniciar(supervisor()) }
        val viewModel = OportunidadViewModel(repository, session)
        collectWhileSubscribed(viewModel.oportunidades)
        advanceUntilIdle()

        var error: String? = null
        viewModel.eliminarOportunidad(999, {}, { error = it })
        assertEquals("No se encontró la oportunidad para eliminarla.", error)

        coEvery { repository.eliminar(10) } returns 0
        viewModel.eliminarOportunidad(10, {}, { error = it })
        advanceUntilIdle()
        assertEquals("No se encontró la oportunidad para eliminarla.", error)

        coEvery { repository.eliminar(10) } returns 1
        var deleted = false
        viewModel.eliminarOportunidad(10, { deleted = true }, {})
        advanceUntilIdle()
        assertTrue(deleted)

        coEvery { repository.eliminar(11) } throws IllegalStateException("DB falla")
        viewModel.eliminarOportunidad(11, {}, { error = it })
        advanceUntilIdle()
        assertTrue(error.orEmpty().contains("DB falla"))
    }

    @Test
    fun eliminarRechazaOportunidadDeOtroResponsable() = runTest {
        val repository = repository()
        val session = SessionManager().apply { iniciar(supervisor()) }
        val viewModel = OportunidadViewModel(repository, session)
        collectWhileSubscribed(viewModel.oportunidades)
        advanceUntilIdle()
        session.iniciar(ejecutivo(1))

        var error: String? = null
        viewModel.eliminarOportunidad(11, {}, { error = it })

        assertEquals("No tienes permiso para eliminar esta oportunidad.", error)
        coVerify(exactly = 0) { repository.eliminar(11) }
    }

    @Test
    fun ejecutivoNoPuedeEditarOportunidadAjena() = runTest {
        val repository = repository()
        val session = SessionManager().apply { iniciar(supervisor()) }
        val viewModel = OportunidadViewModel(repository, session)
        collectWhileSubscribed(viewModel.oportunidades)
        advanceUntilIdle()

        viewModel.beginEditar(11)
        session.iniciar(ejecutivo(1))
        viewModel.beginEditar(11)

        assertEquals("No tienes permiso para editar esta oportunidad.", viewModel.editor.value.mensajeError)
        viewModel.guardarOportunidad {}
        assertEquals("No tienes permiso para editar esta oportunidad.", viewModel.editor.value.mensajeError)
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
        fechaCreacion = id * 100,
        fechaActualizacion = id * 100 + 10,
        contactoId = 5
    )
}
