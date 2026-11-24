package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.local.GestionDao
import cl.duocuc.evaluacion2.data.local.HistorialCambioDao
import cl.duocuc.evaluacion2.data.local.OpportunityDao
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import cl.duocuc.evaluacion2.data.local.UsuarioDao
import cl.duocuc.evaluacion2.data.local.UsuarioEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OportunidadRepositoryTest {

    private val opportunityDao = mockk<OpportunityDao>()
    private val gestionDao = mockk<GestionDao>()
    private val historialDao = mockk<HistorialCambioDao>()
    private val usuarioDao = mockk<UsuarioDao>()
    private val repository = OportunidadRepository(
        dao = opportunityDao,
        historialDao = historialDao,
        usuarioDao = usuarioDao,
        gestionDao = gestionDao,
        ejecutorTransacciones = ImmediateTransactionExecutor
    )

    @Test
    fun nuevaOportunidadInsertaHistorialDeCreacion() = runBlocking {
        val oportunidad = oportunidad(responsableId = 8)
        coEvery { opportunityDao.insert(oportunidad) } returns 15
        coEvery { usuarioDao.buscarPorId(8) } returns usuario(8)
        coEvery { historialDao.insert(any()) } returns 1

        assertEquals(15L, repository.guardar(oportunidad, usuarioId = 8))

        coVerify(exactly = 1) {
            historialDao.insert(match {
                it.oportunidadId == 15L &&
                    it.usuarioId == 8L &&
                    it.tipoAccion == "CREACION" &&
                    it.descripcion.contains("Usuario 8") &&
                    it.fecha == oportunidad.fechaCreacion
            })
        }
    }

    @Test
    fun historialUsaNombreAlternativoOSinResponsable() = runBlocking {
        coEvery { opportunityDao.insert(any()) } returnsMany listOf(16, 17)
        coEvery { usuarioDao.buscarPorId(73) } returns null
        coEvery { historialDao.insert(any()) } returns 1

        repository.guardar(oportunidad(responsableId = 73), usuarioId = 8)
        repository.guardar(oportunidad(responsableId = null), usuarioId = 8)

        coVerify {
            historialDao.insert(match { it.descripcion.contains("Usuario #73") })
            historialDao.insert(match { it.descripcion.contains("Sin responsable") })
        }
    }

    @Test
    fun edicionConservaFechaCreacionYRegistraDiferencias() = runBlocking {
        val anterior = oportunidad(id = 4, nombre = "Anterior", fechaCreacion = 100)
        val editada = anterior.copy(nombre = "Actualizada", fechaCreacion = 900)
        coEvery { opportunityDao.buscarPorId(4) } returns anterior
        coEvery { usuarioDao.buscarPorId(any()) } returns usuario(8)
        coEvery { opportunityDao.update(any()) } returns Unit
        coEvery { historialDao.insert(any()) } returns 1

        assertEquals(4L, repository.guardar(editada, usuarioId = 8))

        val guardada = slot<OpportunityEntity>()
        coVerify { opportunityDao.update(capture(guardada)) }
        assertEquals(100L, guardada.captured.fechaCreacion)
        assertTrue(guardada.captured.fechaActualizacion >= 900L)
        coVerify(exactly = 1) {
            historialDao.insert(match {
                it.oportunidadId == 4L &&
                    it.tipoAccion == "EDICION" &&
                    it.descripcion.contains("Nombre")
            })
        }
    }

    @Test
    fun edicionDeOportunidadInexistenteFallaSinActualizar() = runBlocking {
        coEvery { opportunityDao.buscarPorId(42) } returns null

        val error = runCatching {
            repository.guardar(oportunidad(id = 42), usuarioId = 8)
        }.exceptionOrNull()

        assertEquals("La oportunidad ya no está disponible.", error?.message)
        coVerify(exactly = 0) { opportunityDao.update(any()) }
        coVerify(exactly = 0) { historialDao.insert(any()) }
    }

    @Test
    fun eliminarBorraGestionesHistorialYOportunidadEnTransaccion() = runBlocking {
        coEvery { gestionDao.deleteByOportunidad(5) } returns 2
        coEvery { historialDao.deleteByOportunidad(5) } returns 3
        coEvery { opportunityDao.deleteById(5) } returns 1

        assertEquals(1, repository.eliminar(5))

        coVerifyOrder {
            gestionDao.deleteByOportunidad(5)
            historialDao.deleteByOportunidad(5)
            opportunityDao.deleteById(5)
        }
    }

    @Test
    fun completaHistorialUsandoResponsableOUsuarioPredeterminado() = runBlocking {
        val asignada = oportunidad(id = 1, responsableId = 8)
        val pendiente = oportunidad(id = 2, responsableId = null)
        coEvery { opportunityDao.listarSinHistorial() } returns listOf(asignada, pendiente)
        coEvery { usuarioDao.buscarPorId(8) } returns usuario(8)
        coEvery { historialDao.insert(any()) } returns 1

        repository.completarHistorialCreacion(usuarioId = 99)

        coVerify {
            historialDao.insert(match { it.oportunidadId == 1L && it.usuarioId == 8L })
            historialDao.insert(match { it.oportunidadId == 2L && it.usuarioId == 99L })
        }
    }

    @Test
    fun semillaSoloInsertaCuandoNoHayOportunidades() = runBlocking {
        coEvery { opportunityDao.count() } returnsMany listOf(3, 0)
        coEvery { opportunityDao.insert(any()) } returnsMany listOf(1, 2, 3)

        repository.semillarSiVacia()
        coVerify(exactly = 0) { opportunityDao.insert(any()) }

        repository.semillarSiVacia()
        coVerify(exactly = 3) { opportunityDao.insert(any()) }
    }

    @Test
    fun metodosDeConsultaYAsignacionDeleganAlDao() = runBlocking {
        val opportunity = oportunidad(id = 3)
        every { opportunityDao.observeAll() } returns kotlinx.coroutines.flow.flowOf(listOf(opportunity))
        coEvery { opportunityDao.count() } returns 1
        coEvery { opportunityDao.buscarPorId(3) } returns opportunity
        coEvery { opportunityDao.assignNullResponsible(8) } returns 2

        assertEquals(listOf(opportunity), repository.observeAll().single())
        assertEquals(1, repository.contar())
        assertEquals(opportunity, repository.buscarPorId(3))
        assertEquals(2, repository.asignarResponsablesPendientes(8))
    }

    private fun oportunidad(
        id: Long = 0,
        nombre: String = "Oportunidad",
        responsableId: Long? = 8,
        fechaCreacion: Long = 100
    ) = OpportunityEntity(
        id = id,
        nombre = nombre,
        tipo = "FERIA_EMPRESARIAL",
        descripcion = "Descripción",
        fuente = "Referencia",
        prioridad = "MEDIA",
        potencialComercial = "ALTO",
        estado = "EVENTO_IDENTIFICADO",
        responsableId = responsableId,
        fechaCreacion = fechaCreacion,
        fechaActualizacion = fechaCreacion
    )

    private fun usuario(id: Long) = UsuarioEntity(
        id = id,
        nombre = "Usuario $id",
        correo = "user$id@example.com",
        clave = "password",
        perfil = "EJECUTIVO_COMERCIAL"
    )
}

internal object ImmediateTransactionExecutor : EjecutorTransacciones {
    override suspend fun <T> ejecutar(bloque: suspend () -> T): T = bloque()
}
