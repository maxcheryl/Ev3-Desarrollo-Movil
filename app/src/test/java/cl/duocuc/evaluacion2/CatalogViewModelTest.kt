package cl.duocuc.evaluacion2

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

import cl.duocuc.evaluacion2.data.local.ContactoEntity
import cl.duocuc.evaluacion2.data.local.EventoEntity
import cl.duocuc.evaluacion2.data.local.OrganizacionEntity
import cl.duocuc.evaluacion2.data.model.TipoOrganizacion
import cl.duocuc.evaluacion2.data.repository.ContactoRepository
import cl.duocuc.evaluacion2.data.repository.EventoRepository
import cl.duocuc.evaluacion2.data.repository.OportunidadRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest : StringSpec({
    val dispatcher = StandardTestDispatcher()

    beforeSpec { Dispatchers.setMain(dispatcher) }
    afterSpec { Dispatchers.resetMain() }

    "Organizacion precarga valida recorta guarda y traduce duplicados" { CatalogViewModelTestCases().organizacionPrecargaValidaRecortaGuardaYTraduceDuplicados() }
    "Contacto aplica organizacion por defecto precarga y persiste datos limpios" { CatalogViewModelTestCases().contactoAplicaOrganizacionPorDefectoPrecargaYPersisteDatosLimpios() }
    "Contacto rechaza datos invalidos y expone error del repositorio" { CatalogViewModelTestCases().contactoRechazaDatosInvalidosYExponeErrorDelRepositorio() }
    "Evento aplica organizacion por defecto valida y guarda" { CatalogViewModelTestCases().eventoAplicaOrganizacionPorDefectoValidaYGuarda() }
    "Evento rechaza validacion y reporta error de repositorio" { CatalogViewModelTestCases().eventoRechazaValidacionYReportaErrorDeRepositorio() }
})

@OptIn(ExperimentalCoroutinesApi::class)
private class CatalogViewModelTestCases {


    fun organizacionPrecargaValidaRecortaGuardaYTraduceDuplicados() = runTest {
        val existing = organizacion(id = 3)
        val repository = mockk<OrganizacionRepository>(relaxed = true)
        every { repository.observeAll() } returns flowOf(listOf(existing))
        coEvery { repository.guardar(any()) } returns 40
        val viewModel = OrganizacionViewModel(repository)
        collectWhileSubscribed(viewModel.organizaciones)
        advanceUntilIdle()

        viewModel.beginEditar(3)
        (viewModel.editor.value.form.nombre) shouldBe (existing.nombre)
        viewModel.onNombreChange("  Agencia Max Editada  ")
        viewModel.guardar {}
        advanceUntilIdle()
        coVerify {
            repository.guardar(match { it.id == 3L && it.nombre == "Agencia Max Editada" })
        }
        viewModel.beginEditar(999)
        (viewModel.editor.value.mensajeError.orEmpty().contains("No se encontró")) shouldBe true

        viewModel.beginNueva()
        viewModel.guardar {}
        (viewModel.editor.value.errores.hayErrores) shouldBe true
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
        (saved[1].nombre) shouldBe ("Agencia Max")
        (saved[1].comuna) shouldBe ("Santiago")
        (saved[1].sitioWeb) shouldBe ("https://example.com")
        (saved[1].observaciones) shouldBe ("Nota")
        (savedId) shouldBe (40L)
        (viewModel.editor.value.guardando) shouldBe false

        coEvery { repository.guardar(any()) } throws IllegalStateException("UNIQUE constraint")
        viewModel.beginNueva()
        viewModel.onNombreChange("Agencia Max")
        viewModel.onTipoChange(TipoOrganizacion.values().first().name)
        viewModel.onComunaChange("Santiago")
        viewModel.guardar {}
        advanceUntilIdle()
        (viewModel.editor.value.mensajeError) shouldBe ("Ya existe una organización con ese nombre.")
    }

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
        (viewModel.editor.value.form.organizacionId) shouldBe (8L)
        viewModel.beginEditar(5)
        (viewModel.editor.value.form.nombre) shouldBe ("Ana Pérez")
        viewModel.onNombreChange("  Ana Editada  ")
        viewModel.guardar {}
        advanceUntilIdle()
        coVerify { contactoRepository.guardar(match { it.id == 5L && it.nombre == "Ana Editada" }) }
        viewModel.beginEditar(999)
        (viewModel.editor.value.mensajeError.orEmpty().contains("No se encontró")) shouldBe true

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
        (saved[1].nombre) shouldBe ("Ana Pérez")
        (saved[1].organizacionId) shouldBe (8L)
        (saved[1].cargo) shouldBe ("Gerenta")
        (saved[1].correo) shouldBe ("ana@example.com")
        (saved[1].telefono) shouldBe ("56912345678")
        (saved[1].area) shouldBe ("Comercial")
        (savedId) shouldBe (50L)
    }

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
        (viewModel.editor.value.errores.hayErrores) shouldBe true

        viewModel.onNombreChange("Ana Pérez")
        viewModel.onCargoChange("Gerenta")
        viewModel.onCorreoChange("ana@example.com")
        viewModel.onTelefonoChange("56912345678")
        coEvery { repository.guardar(any()) } throws IllegalStateException("UNIQUE")
        viewModel.guardar {}
        advanceUntilIdle()
        (viewModel.editor.value.mensajeError.orEmpty().contains("UNIQUE")) shouldBe true
        (viewModel.editor.value.guardando) shouldBe false
    }

    fun eventoAplicaOrganizacionPorDefectoValidaYGuarda() = runTest {
        val organization = organizacion(id = 11)
        val existing = evento(id = 6, organizationId = 11)
        val eventRepository = mockk<EventoRepository>(relaxed = true)
        val opportunityRepository = mockk<OportunidadRepository>(relaxed = true)
        val organizationRepository = mockk<OrganizacionRepository>()
        every { eventRepository.observeAll() } returns flowOf(listOf(existing))
        every { organizationRepository.observeAll() } returns flowOf(listOf(organization))
        coEvery { eventRepository.guardar(any()) } returns 60
        val viewModel = EventoViewModel(
            eventRepository,
            organizationRepository,
            opportunityRepository
        )
        collectWhileSubscribed(viewModel.eventos)
        collectWhileSubscribed(viewModel.organizaciones)
        advanceUntilIdle()

        viewModel.beginNueva()
        (viewModel.editor.value.form.organizacionId) shouldBe (11L)
        viewModel.beginEditar(6)
        (viewModel.editor.value.form.fecha) shouldBe (existing.fecha)
        viewModel.onNombreChange("  Feria editada  ")
        viewModel.guardar {}
        advanceUntilIdle()
        coVerify { eventRepository.guardar(match { it.id == 6L && it.nombre == "Feria editada" }) }
        viewModel.beginEditar(99)
        (viewModel.editor.value.mensajeError.orEmpty().contains("No se encontró")) shouldBe true

        viewModel.beginNueva()
        viewModel.onNombreChange("  Feria comunal  ")
        viewModel.onTipoChange("FERIA_EMPRESARIAL")
        viewModel.onDescripcionChange("  Actividad para familias  ")
        viewModel.onFechaChange(1234)
        viewModel.onFechaTerminoChange(5678)
        viewModel.onHoraInicioChange("09:00")
        viewModel.onHoraTerminoChange("18:00")
        viewModel.onDireccionChange("  Plaza central  ")
        viewModel.onRegionChange("  Metropolitana  ")
        viewModel.onComunaChange("  Maipú  ")
        viewModel.onPublicoEsperadoChange("Familias")
        viewModel.onPublicoObjetivoChange("Familias", true)
        viewModel.onAsistentesEstimadosChange("250")
        viewModel.onCondicionesChange("Acceso gratuito")
        viewModel.onRequisitosChange("Permiso municipal")
        viewModel.onCostoReferencialChange("120000")
        viewModel.onEspaciosDisponiblesChange("Dos stands")
        var savedId: Long? = null
        viewModel.guardar(oportunidadId = 22L) { savedId = it }
        advanceUntilIdle()

        val saved = mutableListOf<EventoEntity>()
        coVerify(exactly = 2) { eventRepository.guardar(capture(saved)) }
        (saved[1].nombre) shouldBe ("Feria comunal")
        (saved[1].tipo) shouldBe ("FERIA_EMPRESARIAL")
        (saved[1].descripcion) shouldBe ("Actividad para familias")
        (saved[1].fecha) shouldBe (1234L)
        (saved[1].fechaTermino) shouldBe (5678L)
        (saved[1].horaInicio) shouldBe ("09:00")
        (saved[1].horaTermino) shouldBe ("18:00")
        (saved[1].direccion) shouldBe ("Plaza central")
        (saved[1].region) shouldBe ("Metropolitana")
        (saved[1].comuna) shouldBe ("Maipú")
        (saved[1].publicoEsperado) shouldBe ("Familias")
        (saved[1].publicoObjetivo) shouldBe ("Familias")
        (saved[1].asistentesEstimados) shouldBe (250)
        (saved[1].condicionesParticipacion) shouldBe ("Acceso gratuito")
        (saved[1].requisitos) shouldBe ("Permiso municipal")
        (saved[1].costoReferencial) shouldBe (120000.0)
        (saved[1].espaciosDisponibles) shouldBe ("Dos stands")
        (saved[1].organizacionId) shouldBe (11L)
        (savedId) shouldBe (60L)
        coVerify { opportunityRepository.asociarEvento(22L, 60L) }
    }

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
        (viewModel.editor.value.errores.hayErrores) shouldBe true

        viewModel.onNombreChange("Feria")
        viewModel.onFechaChange(1234)
        viewModel.onFechaTerminoChange(5678)
        viewModel.onRegionChange("Metropolitana")
        viewModel.onComunaChange("Maipú")
        coEvery { repository.guardar(any()) } throws IllegalStateException("No se pudo persistir")
        viewModel.guardar {}
        advanceUntilIdle()
        (viewModel.editor.value.mensajeError.orEmpty().contains("No se pudo persistir")) shouldBe true
        (viewModel.editor.value.guardando) shouldBe false
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
        organizacionId = organizationId,
        fechaTermino = 2000,
        region = "Metropolitana"
    )
}
