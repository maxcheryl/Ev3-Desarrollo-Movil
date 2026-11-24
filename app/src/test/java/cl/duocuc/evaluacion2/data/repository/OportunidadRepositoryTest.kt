package cl.duocuc.evaluacion2.data.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

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

class OportunidadRepositoryTest : StringSpec({
    "Nueva oportunidad inserta historial de creacion" { OportunidadRepositoryTestCases().nuevaOportunidadInsertaHistorialDeCreacion() }
    "Historial usa nombre alternativo osin responsable" { OportunidadRepositoryTestCases().historialUsaNombreAlternativoOSinResponsable() }
    "Edicion conserva fecha creacion y registra diferencias" { OportunidadRepositoryTestCases().edicionConservaFechaCreacionYRegistraDiferencias() }
    "Edicion de oportunidad inexistente falla sin actualizar" { OportunidadRepositoryTestCases().edicionDeOportunidadInexistenteFallaSinActualizar() }
    "Eliminar borra gestiones historial y oportunidad en transaccion" { OportunidadRepositoryTestCases().eliminarBorraGestionesHistorialYOportunidadEnTransaccion() }
    "Completa historial usando responsable ousuario predeterminado" { OportunidadRepositoryTestCases().completaHistorialUsandoResponsableOUsuarioPredeterminado() }
    "Semilla solo inserta cuando no hay oportunidades" { OportunidadRepositoryTestCases().semillaSoloInsertaCuandoNoHayOportunidades() }
    "Metodos de consulta y asignacion delegan al dao" { OportunidadRepositoryTestCases().metodosDeConsultaYAsignacionDeleganAlDao() }
    "Asocia evento a la oportunidad y falla si desaparece" { OportunidadRepositoryTestCases().asociaEventoALaOportunidadYFallaSiDesaparece() }
})

private class OportunidadRepositoryTestCases {

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

    fun nuevaOportunidadInsertaHistorialDeCreacion() = runBlocking {
        val oportunidad = oportunidad(responsableId = 8)
        coEvery { opportunityDao.insert(oportunidad) } returns 15
        coEvery { usuarioDao.buscarPorId(8) } returns usuario(8)
        coEvery { historialDao.insert(any()) } returns 1

        (repository.guardar(oportunidad, usuarioId = 8)) shouldBe (15L)

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

    fun edicionConservaFechaCreacionYRegistraDiferencias() = runBlocking {
        val anterior = oportunidad(id = 4, nombre = "Anterior", fechaCreacion = 100)
        val editada = anterior.copy(nombre = "Actualizada", fechaCreacion = 900)
        coEvery { opportunityDao.buscarPorId(4) } returns anterior
        coEvery { usuarioDao.buscarPorId(any()) } returns usuario(8)
        coEvery { opportunityDao.update(any()) } returns Unit
        coEvery { historialDao.insert(any()) } returns 1

        (repository.guardar(editada, usuarioId = 8)) shouldBe (4L)

        val guardada = slot<OpportunityEntity>()
        coVerify { opportunityDao.update(capture(guardada)) }
        (guardada.captured.fechaCreacion) shouldBe (100L)
        (guardada.captured.fechaActualizacion >= 900L) shouldBe true
        coVerify(exactly = 1) {
            historialDao.insert(match {
                it.oportunidadId == 4L &&
                    it.tipoAccion == "EDICION" &&
                    it.descripcion.contains("Nombre")
            })
        }
    }

    fun edicionDeOportunidadInexistenteFallaSinActualizar() = runBlocking {
        coEvery { opportunityDao.buscarPorId(42) } returns null

        val error = runCatching {
            repository.guardar(oportunidad(id = 42), usuarioId = 8)
        }.exceptionOrNull()

        (error?.message) shouldBe ("La oportunidad ya no está disponible.")
        coVerify(exactly = 0) { opportunityDao.update(any()) }
        coVerify(exactly = 0) { historialDao.insert(any()) }
    }

    fun eliminarBorraGestionesHistorialYOportunidadEnTransaccion() = runBlocking {
        coEvery { gestionDao.deleteByOportunidad(5) } returns 2
        coEvery { historialDao.deleteByOportunidad(5) } returns 3
        coEvery { opportunityDao.deleteById(5) } returns 1

        (repository.eliminar(5)) shouldBe (1)

        coVerifyOrder {
            gestionDao.deleteByOportunidad(5)
            historialDao.deleteByOportunidad(5)
            opportunityDao.deleteById(5)
        }
    }

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

    fun semillaSoloInsertaCuandoNoHayOportunidades() = runBlocking {
        coEvery { opportunityDao.count() } returnsMany listOf(3, 0)
        coEvery { opportunityDao.insert(any()) } returnsMany listOf(1, 2, 3)

        repository.semillarSiVacia()
        coVerify(exactly = 0) { opportunityDao.insert(any()) }

        repository.semillarSiVacia()
        coVerify(exactly = 3) { opportunityDao.insert(any()) }
    }

    fun metodosDeConsultaYAsignacionDeleganAlDao() = runBlocking {
        val opportunity = oportunidad(id = 3)
        every { opportunityDao.observeAll() } returns kotlinx.coroutines.flow.flowOf(listOf(opportunity))
        coEvery { opportunityDao.count() } returns 1
        coEvery { opportunityDao.buscarPorId(3) } returns opportunity
        coEvery { opportunityDao.assignNullResponsible(8) } returns 2

        (repository.observeAll().single()) shouldBe (listOf(opportunity))
        (repository.contar()) shouldBe (1)
        (repository.buscarPorId(3)) shouldBe (opportunity)
        (repository.asignarResponsablesPendientes(8)) shouldBe (2)
    }

    fun asociaEventoALaOportunidadYFallaSiDesaparece() = runBlocking {
        coEvery { opportunityDao.asociarEvento(4, 12, any()) } returns 1
        repository.asociarEvento(oportunidadId = 4, eventoId = 12)
        coVerify {
            opportunityDao.asociarEvento(
                oportunidadId = 4,
                eventoId = 12,
                fechaActualizacion = match { it > 0L }
            )
        }

        coEvery { opportunityDao.asociarEvento(5, 12, any()) } returns 0
        val error = runCatching {
            repository.asociarEvento(oportunidadId = 5, eventoId = 12)
        }.exceptionOrNull()
        (error?.message) shouldBe ("La oportunidad ya no está disponible.")
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
