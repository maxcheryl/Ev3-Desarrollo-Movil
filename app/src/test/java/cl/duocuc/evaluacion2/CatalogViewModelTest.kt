package cl.duocuc.evaluacion2

import cl.duocuc.evaluacion2.data.local.ContactoEntity
import cl.duocuc.evaluacion2.data.local.EventoEntity
import cl.duocuc.evaluacion2.data.local.OrganizacionEntity
import cl.duocuc.evaluacion2.data.model.TipoOrganizacion
import cl.duocuc.evaluacion2.data.repository.ContactoRepository
import cl.duocuc.evaluacion2.data.repository.EventoRepository
import cl.duocuc.evaluacion2.data.repository.OrganizacionRepository
import cl.duocuc.evaluacion2.ui.viewmodels.ContactoViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.EventoViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.OrganizacionViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
class CatalogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun organizacionPrecargaValidaRecortaGuardaYTraduceDuplicados() = runTest {
        val existing = organizacion(id = 3)
        val repository = mockk<OrganizacionRepository>(relaxed = true)
        every { repository.observeAll() } returns flowOf(listOf(existing))
        coEvery { repository.guardar(any()) } returns 40
        val viewModel = OrganizacionViewModel(repository)
        collectWhileSubscribed(viewModel.organizaciones)
        advanceUntilIdle()

        viewModel.beginEditar(3)
        assertEquals(existing.nombre, viewModel.editor.value.form.nombre)
        viewModel.onNombreChange("  Agencia Max Editada  ")
        viewModel.guardar {}
        advanceUntilIdle()
        coVerify {
            repository.guardar(match { it.id == 3L && it.nombre == "Agencia Max Editada" })
        }
        viewModel.beginEditar(999)
        assertTrue(viewModel.editor.value.mensajeError.orEmpty().contains("No se encontró"))

        viewModel.beginNueva()
        viewModel.guardar {}
        assertTrue(viewModel.editor.value.errores.hayErrores)
        coVerify(exactly = 1) { repository.guardar(any()) }

        viewModel.onNombreChange("  Agencia Max  ")
        viewModel.onTipoChange(TipoOrganizacion.values().first().name)
        viewModel.onComunaChange("  Santiago  ")
        viewModel.onSitioWebChange("  https://example.com  ")
        viewModel.onObservacionesChange("  Nota  ")
        var savedId: Long? = null
        viewModel.guardar { savedId = it }
        advanceUntilIdle()

        val saved = mutableListOf<OrganizacionEntity>()
        coVerify(exactly = 2) { repository.guardar(capture(saved)) }
        assertEquals("Agencia Max", saved[1].nombre)
        assertEquals("Santiago", saved[1].comuna)
        assertEquals("https://example.com", saved[1].sitioWeb)
        assertEquals("Nota", saved[1].observaciones)
        assertEquals(40L, savedId)
        assertFalse(viewModel.editor.value.guardando)

        coEvery { repository.guardar(any()) } throws IllegalStateException("UNIQUE constraint")
        viewModel.beginNueva()
        viewModel.onNombreChange("Agencia Max")
        viewModel.onTipoChange(TipoOrganizacion.values().first().name)
        viewModel.onComunaChange("Santiago")
        viewModel.guardar {}
        advanceUntilIdle()
        assertEquals("Ya existe una organización con ese nombre.", viewModel.editor.value.mensajeError)
    }

    @Test
    fun contactoAplicaOrganizacionPorDefectoPrecargaYPersisteDatosLimpios() = runTest {
        val organization = organizacion(id = 8)
        val existing = contacto(id = 5, organizationId = 8)
        val contactoRepository = mockk<ContactoRepository>(relaxed = true)
        val organizacionRepository = mockk<OrganizacionRepository>()
        every { contactoRepository.observeAll() } returns flowOf(listOf(existing))
        every { organizacionRepository.observeAll() } returns flowOf(listOf(organization))
        coEvery { contactoRepository.guardar(any()) } returns 50
        val viewModel = ContactoViewModel(contactoRepository, organizacionRepository)
        collectWhileSubscribed(viewModel.contactos)
        collectWhileSubscribed(viewModel.organizaciones)
        advanceUntilIdle()

        viewModel.beginNueva()
        assertEquals(8L, viewModel.editor.value.form.organizacionId)
        viewModel.beginEditar(5)
        assertEquals("Ana Pérez", viewModel.editor.value.form.nombre)
        viewModel.onNombreChange("  Ana Editada  ")
        viewModel.guardar {}
        advanceUntilIdle()
        coVerify { contactoRepository.guardar(match { it.id == 5L && it.nombre == "Ana Editada" }) }
        viewModel.beginEditar(999)
        assertTrue(viewModel.editor.value.mensajeError.orEmpty().contains("No se encontró"))

        viewModel.beginNueva()
        viewModel.onNombreChange("  Ana Pérez  ")
        viewModel.onCargoChange("  Gerenta  ")
        viewModel.onCorreoChange(" ana@example.com ")
        viewModel.onTelefonoChange(" 56912345678 ")
        viewModel.onAreaChange("  Comercial  ")
        viewModel.onObservacionesChange("  Contacto clave  ")
        var savedId: Long? = null
        viewModel.guardar { savedId = it }
        advanceUntilIdle()

        val saved = mutableListOf<ContactoEntity>()
        coVerify(exactly = 2) { contactoRepository.guardar(capture(saved)) }
        assertEquals("Ana Pérez", saved[1].nombre)
        assertEquals(8L, saved[1].organizacionId)
        assertEquals("Gerenta", saved[1].cargo)
        assertEquals("ana@example.com", saved[1].correo)
        assertEquals("56912345678", saved[1].telefono)
        assertEquals("Comercial", saved[1].area)
        assertEquals(50L, savedId)
    }

    @Test
    fun contactoRechazaDatosInvalidosYExponeErrorDelRepositorio() = runTest {
        val repository = mockk<ContactoRepository>(relaxed = true)
        val organizations = mockk<OrganizacionRepository>()
        every { repository.observeAll() } returns flowOf(emptyList())
        every { organizations.observeAll() } returns flowOf(listOf(organizacion(id = 8)))
        val viewModel = ContactoViewModel(repository, organizations)
        collectWhileSubscribed(viewModel.organizaciones)
        advanceUntilIdle()
        viewModel.beginNueva()
        viewModel.guardar {}
        assertTrue(viewModel.editor.value.errores.hayErrores)

        viewModel.onNombreChange("Ana Pérez")
        viewModel.onCargoChange("Gerenta")
        viewModel.onCorreoChange("ana@example.com")
        viewModel.onTelefonoChange("56912345678")
        coEvery { repository.guardar(any()) } throws IllegalStateException("UNIQUE")
        viewModel.guardar {}
        advanceUntilIdle()
        assertTrue(viewModel.editor.value.mensajeError.orEmpty().contains("UNIQUE"))
        assertFalse(viewModel.editor.value.guardando)
    }

    @Test
    fun eventoAplicaOrganizacionPorDefectoValidaYGuarda() = runTest {
        val organization = organizacion(id = 11)
        val existing = evento(id = 6, organizationId = 11)
        val eventRepository = mockk<EventoRepository>(relaxed = true)
        val organizationRepository = mockk<OrganizacionRepository>()
        every { eventRepository.observeAll() } returns flowOf(listOf(existing))
        every { organizationRepository.observeAll() } returns flowOf(listOf(organization))
        coEvery { eventRepository.guardar(any()) } returns 60
        val viewModel = EventoViewModel(eventRepository, organizationRepository)
        collectWhileSubscribed(viewModel.eventos)
        collectWhileSubscribed(viewModel.organizaciones)
        advanceUntilIdle()

        viewModel.beginNueva()
        assertEquals(11L, viewModel.editor.value.form.organizacionId)
        viewModel.beginEditar(6)
        assertEquals(existing.fecha, viewModel.editor.value.form.fecha)
        viewModel.onNombreChange("  Feria editada  ")
        viewModel.guardar {}
        advanceUntilIdle()
        coVerify { eventRepository.guardar(match { it.id == 6L && it.nombre == "Feria editada" }) }
        viewModel.beginEditar(99)
        assertTrue(viewModel.editor.value.mensajeError.orEmpty().contains("No se encontró"))

        viewModel.beginNueva()
        viewModel.onNombreChange("  Feria comunal  ")
        viewModel.onFechaChange(1234)
        viewModel.onComunaChange("  Maipú  ")
        var savedId: Long? = null
        viewModel.guardar { savedId = it }
        advanceUntilIdle()

        val saved = mutableListOf<EventoEntity>()
        coVerify(exactly = 2) { eventRepository.guardar(capture(saved)) }
        assertEquals("Feria comunal", saved[1].nombre)
        assertEquals(1234L, saved[1].fecha)
        assertEquals("Maipú", saved[1].comuna)
        assertEquals(11L, saved[1].organizacionId)
        assertEquals(60L, savedId)
    }

    @Test
    fun eventoRechazaValidacionYReportaErrorDeRepositorio() = runTest {
        val repository = mockk<EventoRepository>(relaxed = true)
        val organizations = mockk<OrganizacionRepository>()
        every { repository.observeAll() } returns flowOf(emptyList())
        every { organizations.observeAll() } returns flowOf(listOf(organizacion(id = 11)))
        val viewModel = EventoViewModel(repository, organizations)
        collectWhileSubscribed(viewModel.organizaciones)
        advanceUntilIdle()
        viewModel.beginNueva()
        viewModel.guardar {}
        assertTrue(viewModel.editor.value.errores.hayErrores)

        viewModel.onNombreChange("Feria")
        viewModel.onFechaChange(1234)
        viewModel.onComunaChange("Maipú")
        coEvery { repository.guardar(any()) } throws IllegalStateException("No se pudo persistir")
        viewModel.guardar {}
        advanceUntilIdle()
        assertTrue(viewModel.editor.value.mensajeError.orEmpty().contains("No se pudo persistir"))
        assertFalse(viewModel.editor.value.guardando)
    }

    private fun organizacion(id: Long) = OrganizacionEntity(
        id = id,
        nombre = "Organización $id",
        tipo = TipoOrganizacion.values().first().name,
        comuna = "Santiago"
    )

    private fun contacto(id: Long, organizationId: Long) = ContactoEntity(
        id = id,
        nombre = "Ana Pérez",
        organizacionId = organizationId,
        cargo = "Gerenta",
        area = "Comercial",
        correo = "ana@example.com",
        telefono = "56912345678"
    )

    private fun evento(id: Long, organizationId: Long) = EventoEntity(
        id = id,
        nombre = "Feria",
        fecha = 1000,
        comuna = "Santiago",
        organizacionId = organizationId
    )
}
