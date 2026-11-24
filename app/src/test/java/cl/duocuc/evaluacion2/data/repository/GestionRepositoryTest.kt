package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.local.GestionDao
import cl.duocuc.evaluacion2.data.local.GestionEntity
import cl.duocuc.evaluacion2.data.local.HistorialCambioDao
import cl.duocuc.evaluacion2.data.local.OpportunityDao
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestionRepositoryTest {

    private val gestionDao = mockk<GestionDao>()
    private val opportunityDao = mockk<OpportunityDao>()
    private val historialDao = mockk<HistorialCambioDao>()
    private val repository = GestionRepository(
        gestionDao = gestionDao,
        opportunityDao = opportunityDao,
        historialDao = historialDao,
        ejecutorTransacciones = ImmediateTransactionExecutor
    )

    @Test
    fun registraGestionActualizaEstadoYAgregaHistorial() = runBlocking {
        val oportunidad = oportunidad(estado = "EVENTO_IDENTIFICADO")
        val gestion = GestionEntity(
            oportunidadId = 2,
            usuarioId = 7,
            tipo = "CONTACTO",
            descripcion = "Llamada realizada"
        )
        coEvery { opportunityDao.buscarPorId(2) } returns oportunidad
        coEvery { gestionDao.insert(gestion) } returns 21
        coEvery { opportunityDao.update(any()) } just Runs
        coEvery { historialDao.insert(any()) } returns 1

        assertEquals(21L, repository.registrar(gestion, "CONTACTO_REALIZADO"))

        val updated = slot<OpportunityEntity>()
        coVerify { opportunityDao.update(capture(updated)) }
        assertEquals("CONTACTO_REALIZADO", updated.captured.estado)
        assertTrue(updated.captured.fechaActualizacion >= oportunidad.fechaActualizacion)
        coVerify {
            historialDao.insert(match {
                it.oportunidadId == 2L &&
                    it.usuarioId == 7L &&
                    it.tipoAccion == "GESTION" &&
                    it.descripcion.contains("Llamada realizada") &&
                    it.descripcion.contains("Evento identificado") &&
                    it.descripcion.contains("Contacto realizado")
            })
        }
    }

    @Test
    fun oportunidadInexistenteNoRegistraGestion() = runBlocking {
        val gestion = GestionEntity(
            oportunidadId = 99,
            usuarioId = 7,
            tipo = "CONTACTO",
            descripcion = "Llamada"
        )
        coEvery { opportunityDao.buscarPorId(99) } returns null

        val error = runCatching { repository.registrar(gestion, "CONTACTO_REALIZADO") }
            .exceptionOrNull()

        assertEquals("La oportunidad ya no está disponible.", error?.message)
        coVerify(exactly = 0) { gestionDao.insert(any()) }
        coVerify(exactly = 0) { historialDao.insert(any()) }
    }

    @Test
    fun flujosYConsultaDeOportunidadSeDelegan() = runBlocking {
        val oportunidad = oportunidad()
        val gestiones = listOf(
            GestionEntity(oportunidadId = 2, usuarioId = 7, tipo = "GESTION", descripcion = "Seguimiento")
        )
        io.mockk.every { gestionDao.observePorOportunidad(2) } returns kotlinx.coroutines.flow.flowOf(gestiones)
        coEvery { opportunityDao.buscarPorId(2) } returns oportunidad

        assertEquals(gestiones, repository.observePorOportunidad(2).single())
        assertEquals(oportunidad, repository.buscarOportunidad(2))
    }

    private fun oportunidad(estado: String = "EVENTO_IDENTIFICADO") = OpportunityEntity(
        id = 2,
        nombre = "Feria",
        tipo = "FERIA_EMPRESARIAL",
        descripcion = "Descripción",
        fuente = "Referencia",
        prioridad = "MEDIA",
        potencialComercial = "ALTO",
        estado = estado,
        responsableId = 7
    )
}
