package cl.duocuc.evaluacion2.data.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

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

class GestionRepositoryTest : StringSpec({
    "Registra gestion actualiza estado y agrega historial" { GestionRepositoryTestCases().registraGestionActualizaEstadoYAgregaHistorial() }
    "Registra seguimiento sin cambiar estado" { GestionRepositoryTestCases().registraSeguimientoSinCambiarEstado() }
    "Oportunidad inexistente no registra gestion" { GestionRepositoryTestCases().oportunidadInexistenteNoRegistraGestion() }
    "Flujos y consulta de oportunidad se delegan" { GestionRepositoryTestCases().flujosYConsultaDeOportunidadSeDelegan() }
})

private class GestionRepositoryTestCases {

    private val gestionDao = mockk<GestionDao>()
    private val opportunityDao = mockk<OpportunityDao>()
    private val historialDao = mockk<HistorialCambioDao>()
    private val repository = GestionRepository(
        gestionDao = gestionDao,
        opportunityDao = opportunityDao,
        historialDao = historialDao,
        ejecutorTransacciones = ImmediateTransactionExecutor
    )

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

        (repository.registrar(gestion, "CONTACTO_REALIZADO")) shouldBe (21L)

        val updated = slot<OpportunityEntity>()
        coVerify { opportunityDao.update(capture(updated)) }
        (updated.captured.estado) shouldBe ("CONTACTO_REALIZADO")
        (updated.captured.fechaActualizacion >= oportunidad.fechaActualizacion) shouldBe true
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

        (error?.message) shouldBe ("La oportunidad ya no está disponible.")
        coVerify(exactly = 0) { gestionDao.insert(any()) }
        coVerify(exactly = 0) { historialDao.insert(any()) }
    }

    fun registraSeguimientoSinCambiarEstado() = runBlocking {
        val oportunidad = oportunidad(estado = "CONTACTO_PENDIENTE")
        val gestion = GestionEntity(
            oportunidadId = 2,
            usuarioId = 7,
            tipo = "SEGUIMIENTO",
            descripcion = "Respuesta pendiente"
        )
        coEvery { opportunityDao.buscarPorId(2) } returns oportunidad
        coEvery { gestionDao.insert(gestion) } returns 22
        coEvery { historialDao.insert(any()) } returns 1

        (repository.registrar(gestion, null)) shouldBe (22L)

        coVerify(exactly = 0) { opportunityDao.update(any()) }
        coVerify {
            historialDao.insert(match {
                it.oportunidadId == 2L &&
                    it.usuarioId == 7L &&
                    it.tipoAccion == "GESTION" &&
                    it.descripcion.contains("Seguimiento")
            })
        }
    }

    fun flujosYConsultaDeOportunidadSeDelegan() = runBlocking {
        val oportunidad = oportunidad()
        val gestiones = listOf(
            GestionEntity(oportunidadId = 2, usuarioId = 7, tipo = "GESTION", descripcion = "Seguimiento")
        )
        io.mockk.every { gestionDao.observePorOportunidad(2) } returns kotlinx.coroutines.flow.flowOf(gestiones)
        coEvery { opportunityDao.buscarPorId(2) } returns oportunidad

        (repository.observePorOportunidad(2).single()) shouldBe (gestiones)
        (repository.buscarOportunidad(2)) shouldBe (oportunidad)
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
