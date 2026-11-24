package cl.duocuc.evaluacion2.data.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import cl.duocuc.evaluacion2.data.local.ActivacionDao
import cl.duocuc.evaluacion2.data.local.ActivacionEntity
import cl.duocuc.evaluacion2.data.local.EvidenciaDao
import cl.duocuc.evaluacion2.data.local.EvidenciaEntity
import cl.duocuc.evaluacion2.data.local.HistorialCambioDao
import cl.duocuc.evaluacion2.data.local.OpportunityDao
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import cl.duocuc.evaluacion2.data.local.ResultadoDao
import cl.duocuc.evaluacion2.data.local.ResultadoEntity
import cl.duocuc.evaluacion2.data.local.UsuarioDao
import cl.duocuc.evaluacion2.data.local.UsuarioEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking

class ActivacionRepositoryTest : StringSpec({
    "Guarda nueva activacion y actualiza una existente sin duplicar" { ActivacionRepositoryTestCases().guardaNuevaActivacionYActualizaUnaExistenteSinDuplicar() }
    "Guardar activacion rechaza oportunidad oresponsable inexistente" { ActivacionRepositoryTestCases().guardarActivacionRechazaOportunidadOResponsableInexistente() }
    "Adjuntar evidencia registra historial y falla sin activacion" { ActivacionRepositoryTestCases().adjuntarEvidenciaRegistraHistorialYFallaSinActivacion() }
    "Resultado valida referencias y actualiza todas las entidades" { ActivacionRepositoryTestCases().resultadoValidaReferenciasYActualizaTodasLasEntidades() }
    "Delega consultas y flujos" { ActivacionRepositoryTestCases().delegaConsultasYFlujos() }
})

private class ActivacionRepositoryTestCases {

    private val activacionDao = mockk<ActivacionDao>()
    private val evidenciaDao = mockk<EvidenciaDao>()
    private val historialDao = mockk<HistorialCambioDao>()
    private val opportunityDao = mockk<OpportunityDao>()
    private val resultadoDao = mockk<ResultadoDao>()
    private val usuarioDao = mockk<UsuarioDao>()
    private val repository = ActivacionRepository(
        activacionDao = activacionDao,
        evidenciaDao = evidenciaDao,
        historialDao = historialDao,
        opportunityDao = opportunityDao,
        resultadoDao = resultadoDao,
        usuarioDao = usuarioDao,
        ejecutorTransacciones = ImmediateTransactionExecutor
    )

    fun guardaNuevaActivacionYActualizaUnaExistenteSinDuplicar() = runBlocking {
        val activacion = ActivacionEntity(oportunidadId = 4, responsableId = 9)
        coEvery { opportunityDao.buscarPorId(4) } returns oportunidad()
        coEvery { usuarioDao.buscarPorId(9) } returns usuario()
        coEvery { activacionDao.obtenerPorOportunidad(4) } returnsMany listOf(
            null,
            ActivacionEntity(id = 12, oportunidadId = 4, responsableId = 9)
        )
        coEvery { activacionDao.insert(any()) } returns 31
        coEvery { activacionDao.update(any()) } just Runs
        coEvery { historialDao.insert(any()) } returns 1

        (repository.guardarActivacion(activacion, usuarioId = 9)) shouldBe (31L)
        (repository.guardarActivacion(activacion, usuarioId = 9)) shouldBe (12L)

        coVerify { activacionDao.insert(match { it.id == 0L && it.oportunidadId == 4L }) }
        coVerify { activacionDao.update(match { it.id == 12L }) }
        coVerify(exactly = 2) { historialDao.insert(match { it.tipoAccion == "ACTIVACION" }) }
    }

    fun guardarActivacionRechazaOportunidadOResponsableInexistente() = runBlocking {
        val activacion = ActivacionEntity(oportunidadId = 4, responsableId = 9)
        coEvery { opportunityDao.buscarPorId(4) } returns null
        (runCatching { repository.guardarActivacion(activacion, 9) }.exceptionOrNull()?.message) shouldBe ("La oportunidad ya no está disponible.")

        coEvery { opportunityDao.buscarPorId(4) } returns oportunidad()
        coEvery { usuarioDao.buscarPorId(9) } returns null
        (runCatching { repository.guardarActivacion(activacion, 9) }.exceptionOrNull()?.message) shouldBe ("Selecciona un responsable disponible.")
        coVerify(exactly = 0) { activacionDao.insert(any()) }
    }

    fun adjuntarEvidenciaRegistraHistorialYFallaSinActivacion() = runBlocking {
        val evidencia = EvidenciaEntity(
            activacionId = 5,
            tipo = "FOTO",
            nombre = "stand.jpg",
            ruta = "content://stand"
        )
        coEvery { activacionDao.buscarPorId(5) } returnsMany listOf(
            null,
            ActivacionEntity(id = 5, oportunidadId = 4, responsableId = 9)
        )
        coEvery { evidenciaDao.insert(evidencia) } returns 22
        coEvery { historialDao.insert(any()) } returns 1

        (runCatching { repository.agregarEvidencia(evidencia, 9) }.exceptionOrNull()?.message) shouldBe ("No se encontró la activación asociada.")
        (repository.agregarEvidencia(evidencia, 9)) shouldBe (22L)
        coVerify {
            historialDao.insert(match {
                it.oportunidadId == 4L && it.descripcion == "Evidencia adjunta: stand.jpg."
            })
        }
    }

    fun resultadoValidaReferenciasYActualizaTodasLasEntidades() = runBlocking {
        val activation = ActivacionEntity(id = 5, oportunidadId = 4, responsableId = 9)
        val opportunity = oportunidad()
        coEvery { activacionDao.buscarPorId(5) } returnsMany listOf(
            null,
            activation,
            activation
        )
        coEvery { opportunityDao.buscarPorId(4) } returnsMany listOf(null, opportunity)
        coEvery { resultadoDao.obtenerPorActivacion(5) } returnsMany listOf(
            null,
            ResultadoEntity(id = 8, activacionId = 5, resultado = "PARTICIPACION_CONFIRMADA")
        )
        coEvery { resultadoDao.insert(any()) } returns 14
        coEvery { resultadoDao.update(any()) } just Runs
        coEvery { activacionDao.update(any()) } just Runs
        coEvery { opportunityDao.update(any()) } just Runs
        coEvery { historialDao.insert(any()) } returns 1

        (runCatching {
                repository.guardarResultado(5, "OTRO", "", "", "OTRO", 9)
            }.exceptionOrNull()?.message) shouldBe ("Selecciona un resultado final válido.")
        (runCatching {
                repository.guardarResultado(5, "RESULTADO", "", "", "PARTICIPACION_CONFIRMADA", 9)
            }.exceptionOrNull()?.message) shouldBe ("No se encontró la activación asociada.")
        (runCatching {
                repository.guardarResultado(5, "RESULTADO", "", "", "PARTICIPACION_CONFIRMADA", 9)
            }.exceptionOrNull()?.message) shouldBe ("La oportunidad ya no está disponible.")

        repository.guardarResultado(
            activacionId = 5,
            resultado = "PARTICIPACION_CONFIRMADA",
            observaciones = " Confirmada ",
            impacto = " Alto ",
            nuevoEstado = "PARTICIPACION_CONFIRMADA",
            usuarioId = 9
        )

        coVerify { resultadoDao.insert(match { it.activacionId == 5L && it.fecha > 0L }) }
        coVerify {
            activacionDao.update(match { it.participacionConfirmada })
            opportunityDao.update(match { it.estado == "PARTICIPACION_CONFIRMADA" })
            historialDao.insert(match { it.tipoAccion == "RESULTADO" })
        }

        repository.guardarResultado(5, "OPORTUNIDAD_DESCARTADA", "", "", "OPORTUNIDAD_DESCARTADA", 9)
        coVerify { resultadoDao.update(match { it.id == 8L }) }
        coVerify { activacionDao.update(match { !it.participacionConfirmada }) }
        coVerify { opportunityDao.update(match { it.estado == "OPORTUNIDAD_DESCARTADA" }) }
    }

    fun delegaConsultasYFlujos() = runBlocking {
        val user = usuario()
        val opportunity = oportunidad()
        every { usuarioDao.observeAll() } returns flowOf(listOf(user))
        every { activacionDao.observePorOportunidad(4) } returns flowOf(null)
        every { evidenciaDao.observePorActivacion(5) } returns flowOf(emptyList())
        every { resultadoDao.observePorActivacion(5) } returns flowOf(null)
        coEvery { opportunityDao.buscarPorId(4) } returns opportunity
        coEvery { activacionDao.obtenerPorOportunidad(4) } returns null
        coEvery { evidenciaDao.listarPorActivacion(5) } returns emptyList()
        coEvery { resultadoDao.obtenerPorActivacion(5) } returns null

        (repository.observeUsuarios().single()) shouldBe (listOf(user))
        (repository.obtenerOportunidad(4)) shouldBe (opportunity)
        (repository.obtenerActivacion(4)) shouldBe (null)
        (repository.obtenerEvidencias(5)) shouldBe (emptyList<EvidenciaEntity>())
        (repository.obtenerResultado(5)) shouldBe (null)
        (repository.observeResultado(5).single()) shouldBe (null)
        (repository.observeUsuarios().single().isNotEmpty()) shouldBe true
    }

    private fun oportunidad() = OpportunityEntity(
        id = 4,
        nombre = "Feria",
        tipo = "FERIA_EMPRESARIAL",
        descripcion = "Descripción",
        fuente = "Referencia",
        prioridad = "MEDIA",
        potencialComercial = "ALTO",
        estado = "EVENTO_IDENTIFICADO",
        responsableId = 9
    )

    private fun usuario() = UsuarioEntity(
        id = 9,
        nombre = "Ana",
        correo = "ana@example.com",
        clave = "secret",
        perfil = "EJECUTIVO_COMERCIAL"
    )
}
