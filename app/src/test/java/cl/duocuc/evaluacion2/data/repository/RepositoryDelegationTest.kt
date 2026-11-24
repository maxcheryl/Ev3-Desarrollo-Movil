package cl.duocuc.evaluacion2.data.repository

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
import org.junit.Assert.assertEquals
import org.junit.Test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.just
import io.mockk.Runs

class RepositoryDelegationTest {

    @Test
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

        assertEquals(11L, OrganizacionRepository(orgDao).guardar(org))
        assertEquals(12L, ContactoRepository(contactoDao).guardar(contacto))
        assertEquals(13L, EventoRepository(eventoDao).guardar(evento))

        val orgEditado = org.copy(id = 5)
        val contactoEditado = contacto.copy(id = 6)
        val eventoEditado = evento.copy(id = 7)
        assertEquals(5L, OrganizacionRepository(orgDao).guardar(orgEditado))
        assertEquals(6L, ContactoRepository(contactoDao).guardar(contactoEditado))
        assertEquals(7L, EventoRepository(eventoDao).guardar(eventoEditado))

        coVerify(exactly = 1) { orgDao.update(orgEditado) }
        coVerify(exactly = 1) { contactoDao.update(contactoEditado) }
        coVerify(exactly = 1) { eventoDao.update(eventoEditado) }
    }

    @Test
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

        assertEquals(2, OrganizacionRepository(orgDao).contar())
        assertEquals(3, ContactoRepository(contactoDao).contar())
        assertEquals(4, EventoRepository(eventoDao).contar())
        assertEquals(emptyList<OrganizacionEntity>(), OrganizacionRepository(orgDao).observeAll().single())
        assertEquals(emptyList<ContactoEntity>(), ContactoRepository(contactoDao).observeAll().single())
        assertEquals(emptyList<EventoEntity>(), EventoRepository(eventoDao).observeAll().single())
        assertEquals(listOf(cambio), HistorialRepository(historialDao).observePorOportunidad(8).single())
        assertEquals(1, UsuarioRepository(usuarioDao).observeAll().single().size)
    }
}
