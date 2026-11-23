package cl.duocuc.evaluacion2

import cl.duocuc.evaluacion2.data.historial.HistorialDescripciones
import cl.duocuc.evaluacion2.data.local.GestionEntity
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.PotencialComercial
import cl.duocuc.evaluacion2.data.model.Prioridad
import cl.duocuc.evaluacion2.data.model.TipoAccionHistorial
import cl.duocuc.evaluacion2.data.model.TipoGestion
import cl.duocuc.evaluacion2.data.model.TipoOportunidad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistorialDescripcionesTest {

    private fun oportunidad(
        nombre: String = "Feria de bienestar",
        estado: String = EstadoGestion.EVENTO_IDENTIFICADO.name,
        prioridad: String = Prioridad.MEDIA.name,
        responsableId: Long? = 1L
    ) = OpportunityEntity(
        id = 1L,
        nombre = nombre,
        tipo = TipoOportunidad.FERIA_EMPRESARIAL.name,
        descripcion = "Descripción de la oportunidad",
        fuente = "Referencia",
        prioridad = prioridad,
        potencialComercial = PotencialComercial.ALTO.name,
        estado = estado,
        responsableId = responsableId
    )

    @Test
    fun creacionIncluyeEstadoPrioridadYResponsable() {
        val cambio = HistorialDescripciones.creacion(oportunidad(), "Dalí Morales")

        assertEquals(TipoAccionHistorial.CREACION, cambio.tipoAccion)
        assertTrue(cambio.descripcion.contains("Evento identificado"))
        assertTrue(cambio.descripcion.contains("Media"))
        assertTrue(cambio.descripcion.contains("Dalí Morales"))
    }

    @Test
    fun creacionSinResponsableLoIndica() {
        val cambio = HistorialDescripciones.creacion(
            oportunidad(responsableId = null),
            null
        )

        assertTrue(cambio.descripcion.contains("Sin responsable"))
    }

    @Test
    fun actualizacionDetectaCambioDeEstado() {
        val cambios = HistorialDescripciones.actualizacion(
            anterior = oportunidad(estado = EstadoGestion.EVENTO_IDENTIFICADO.name),
            nueva = oportunidad(estado = EstadoGestion.CONTACTO_REALIZADO.name),
            responsableAnterior = "Dalí Morales",
            responsableNuevo = "Dalí Morales"
        )

        assertEquals(1, cambios.size)
        assertEquals(TipoAccionHistorial.CAMBIO_ESTADO, cambios.first().tipoAccion)
        assertTrue(cambios.first().descripcion.contains("Evento identificado"))
        assertTrue(cambios.first().descripcion.contains("Contacto realizado"))
    }

    @Test
    fun actualizacionDetectaCambioDePrioridad() {
        val cambios = HistorialDescripciones.actualizacion(
            anterior = oportunidad(prioridad = Prioridad.ALTA.name),
            nueva = oportunidad(prioridad = Prioridad.BAJA.name),
            responsableAnterior = "Dalí Morales",
            responsableNuevo = "Dalí Morales"
        )

        assertEquals(1, cambios.size)
        assertEquals(TipoAccionHistorial.CAMBIO_PRIORIDAD, cambios.first().tipoAccion)
        assertTrue(cambios.first().descripcion.contains("Alta"))
        assertTrue(cambios.first().descripcion.contains("Baja"))
    }

    @Test
    fun actualizacionDetectaAsignacionDeResponsable() {
        val cambios = HistorialDescripciones.actualizacion(
            anterior = oportunidad(responsableId = null),
            nueva = oportunidad(responsableId = 2L),
            responsableAnterior = null,
            responsableNuevo = "Maxine Moreno"
        )

        assertEquals(1, cambios.size)
        assertEquals(TipoAccionHistorial.ASIGNACION_RESPONSABLE, cambios.first().tipoAccion)
        assertTrue(cambios.first().descripcion.contains("Responsable asignado: Maxine Moreno"))
    }

    @Test
    fun actualizacionDetectaReasignacionDeResponsable() {
        val cambios = HistorialDescripciones.actualizacion(
            anterior = oportunidad(responsableId = 1L),
            nueva = oportunidad(responsableId = 2L),
            responsableAnterior = "Dalí Morales",
            responsableNuevo = "Maxine Moreno"
        )

        assertEquals(1, cambios.size)
        assertEquals(TipoAccionHistorial.REASIGNACION_RESPONSABLE, cambios.first().tipoAccion)
        assertTrue(
            cambios.first().descripcion.contains("Responsable reasignado: Dalí Morales → Maxine Moreno")
        )
    }

    @Test
    fun actualizacionSinCambiosDevuelveListaVacia() {
        val cambios = HistorialDescripciones.actualizacion(
            anterior = oportunidad(),
            nueva = oportunidad(),
            responsableAnterior = "Dalí Morales",
            responsableNuevo = "Dalí Morales"
        )

        assertTrue(cambios.isEmpty())
    }

    @Test
    fun actualizacionDetectaEdicionDeDatos() {
        val anterior = oportunidad()
        val nueva = anterior.copy(
            nombre = "Nuevo nombre",
            descripcion = "Nueva descripción"
        )

        val cambios = HistorialDescripciones.actualizacion(
            anterior = anterior,
            nueva = nueva,
            responsableAnterior = "Dalí Morales",
            responsableNuevo = "Dalí Morales"
        )

        assertEquals(1, cambios.size)
        assertEquals(TipoAccionHistorial.EDICION, cambios.first().tipoAccion)
        assertTrue(cambios.first().descripcion.contains("Nombre"))
        assertTrue(cambios.first().descripcion.contains("Descripción"))
    }

    @Test
    fun gestionIncluyeTipoDetalleYCambioDeEstado() {
        val gestion = GestionEntity(
            oportunidadId = 1L,
            usuarioId = 1L,
            tipo = TipoGestion.CONTACTO.name,
            descripcion = "Llamada realizada con la organización."
        )

        val cambio = HistorialDescripciones.gestion(
            gestion = gestion,
            estadoAnterior = EstadoGestion.EVENTO_IDENTIFICADO.name,
            estadoNuevo = EstadoGestion.CONTACTO_REALIZADO.name
        )

        assertEquals(TipoAccionHistorial.GESTION, cambio.tipoAccion)
        assertTrue(cambio.descripcion.contains("Gestión de Contacto registrada"))
        assertTrue(cambio.descripcion.contains("Llamada realizada con la organización"))
        assertTrue(cambio.descripcion.contains("Estado: Evento identificado → Contacto realizado"))
    }

    @Test
    fun gestionSinCambioDeEstadoNoMencionaEstado() {
        val gestion = GestionEntity(
            oportunidadId = 1L,
            usuarioId = 1L,
            tipo = TipoGestion.GESTION.name,
            descripcion = "Seguimiento interno"
        )

        val cambio = HistorialDescripciones.gestion(
            gestion = gestion,
            estadoAnterior = EstadoGestion.CONTACTO_PENDIENTE.name,
            estadoNuevo = EstadoGestion.CONTACTO_PENDIENTE.name
        )

        assertEquals(TipoAccionHistorial.GESTION, cambio.tipoAccion)
        assertFalse(cambio.descripcion.contains("Estado:"))
    }

    @Test
    fun gestionSinDescripcionSoloIndicaElTipo() {
        val gestion = GestionEntity(
            oportunidadId = 1L,
            usuarioId = 1L,
            tipo = TipoGestion.CONTACTO.name,
            descripcion = ""
        )

        val cambio = HistorialDescripciones.gestion(
            gestion = gestion,
            estadoAnterior = EstadoGestion.CONTACTO_PENDIENTE.name,
            estadoNuevo = EstadoGestion.CONTACTO_PENDIENTE.name
        )

        assertEquals("Gestión de Contacto registrada.", cambio.descripcion)
    }

    @Test
    fun gestionDeTipoGenericoNoRepiteLaPalabra() {
        val gestion = GestionEntity(
            oportunidadId = 1L,
            usuarioId = 1L,
            tipo = TipoGestion.GESTION.name,
            descripcion = ""
        )

        val cambio = HistorialDescripciones.gestion(
            gestion = gestion,
            estadoAnterior = EstadoGestion.CONTACTO_PENDIENTE.name,
            estadoNuevo = EstadoGestion.CONTACTO_PENDIENTE.name
        )

        assertEquals("Gestión registrada.", cambio.descripcion)
    }
}
