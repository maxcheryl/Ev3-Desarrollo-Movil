package cl.duocuc.evaluacion2.data.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import cl.duocuc.evaluacion2.data.local.ContactoDao
import cl.duocuc.evaluacion2.data.local.ContactoEntity
import cl.duocuc.evaluacion2.data.local.EventoDao
import cl.duocuc.evaluacion2.data.local.EventoEntity
import cl.duocuc.evaluacion2.data.local.HistorialCambioDao
import cl.duocuc.evaluacion2.data.local.HistorialCambioEntity
import cl.duocuc.evaluacion2.data.local.OrganizacionDao
import cl.duocuc.evaluacion2.data.local.OrganizacionEntity
import cl.duocuc.evaluacion2.data.local.UsuarioDao
import cl.duocuc.evaluacion2.data.local.UsuarioEntity
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.just
import io.mockk.Runs

class RepositoryDelegationTest : StringSpec({
    "Repositorios de catalogo insertan y actualizan segun id" { RepositoryDelegationTestCases().repositoriosDeCatalogoInsertanYActualizanSegunId() }
    "Repositorios delegan conteo y flujos" { RepositoryDelegationTestCases().repositoriosDeleganConteoYFlujos() }
})

private class RepositoryDelegationTestCases {

    fun repositoriosDeCatalogoInsertanYActualizanSegunId() = runBlocking {
        val orgDao = mockk<OrganizacionDao>()
        val contactoDao = mockk<ContactoDao>()
        val eventoDao = mockk<EventoDao>()
        val org = OrganizacionEntity(nombre = "Org", tipo = "MUNICIPALIDAD", comuna = "Centro")
        val contacto = ContactoEntity(
            nombre = "Ana", organizacionId = 1, cargo = "Jefa", area = "", correo = "a@b.cl", telefono = "123456"
        )
        val evento = EventoEntity(nombre = "Feria", fecha = 1, comuna = "Centro", organizacionId = 1)

        coEvery { orgDao.insert(org) } returns 11L
        coEvery { orgDao.update(any()) } just Runs
        coEvery { contactoDao.insert(contacto) } returns 12L
        coEvery { contactoDao.update(any()) } just Runs
        coEvery { eventoDao.insert(evento) } returns 13L
        coEvery { eventoDao.update(any()) } just Runs

        (OrganizacionRepository(orgDao).guardar(org)) shouldBe (11L)
        (ContactoRepository(contactoDao).guardar(contacto)) shouldBe (12L)
        (EventoRepository(eventoDao).guardar(evento)) shouldBe (13L)

        val orgEditado = org.copy(id = 5)
        val contactoEditado = contacto.copy(id = 6)
        val eventoEditado = evento.copy(id = 7)
        (OrganizacionRepository(orgDao).guardar(orgEditado)) shouldBe (5L)
        (ContactoRepository(contactoDao).guardar(contactoEditado)) shouldBe (6L)
        (EventoRepository(eventoDao).guardar(eventoEditado)) shouldBe (7L)

        coVerify(exactly = 1) { orgDao.update(orgEditado) }
        coVerify(exactly = 1) { contactoDao.update(contactoEditado) }
        coVerify(exactly = 1) { eventoDao.update(eventoEditado) }
    }

    fun repositoriosDeleganConteoYFlujos() = runBlocking {
        val orgDao = mockk<OrganizacionDao>()
        val contactoDao = mockk<ContactoDao>()
        val eventoDao = mockk<EventoDao>()
        val usuarioDao = mockk<UsuarioDao>()
        val historialDao = mockk<HistorialCambioDao>()
        coEvery { orgDao.count() } returns 2
        coEvery { contactoDao.count() } returns 3
        coEvery { eventoDao.count() } returns 4
        every { orgDao.observeAll() } returns flowOf(emptyList())
        every { contactoDao.observeAll() } returns flowOf(emptyList())
        every { eventoDao.observeAll() } returns flowOf(emptyList())
        every { usuarioDao.observeAll() } returns flowOf(
            listOf(UsuarioEntity(id = 1, nombre = "Ana", correo = "a@b.cl", clave = "x", perfil = "EJECUTIVO_COMERCIAL"))
        )
        val cambio = HistorialCambioEntity(
            oportunidadId = 1,
            usuarioId = 1,
            tipoAccion = "CREACION",
            descripcion = "Creada"
        )
        every { historialDao.observePorOportunidad(8) } returns flowOf(listOf(cambio))

        (OrganizacionRepository(orgDao).contar()) shouldBe (2)
        (ContactoRepository(contactoDao).contar()) shouldBe (3)
        (EventoRepository(eventoDao).contar()) shouldBe (4)
        (OrganizacionRepository(orgDao).observeAll().single()) shouldBe (emptyList<OrganizacionEntity>())
        (ContactoRepository(contactoDao).observeAll().single()) shouldBe (emptyList<ContactoEntity>())
        (EventoRepository(eventoDao).observeAll().single()) shouldBe (emptyList<EventoEntity>())
        (HistorialRepository(historialDao).observePorOportunidad(8).single()) shouldBe (listOf(cambio))
        (UsuarioRepository(usuarioDao).observeAll().single().size) shouldBe (1)
    }
}
