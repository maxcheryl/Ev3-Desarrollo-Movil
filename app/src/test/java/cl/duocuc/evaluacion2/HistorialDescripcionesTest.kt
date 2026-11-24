package cl.duocuc.evaluacion2

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

import cl.duocuc.evaluacion2.data.historial.HistorialDescripciones
import cl.duocuc.evaluacion2.data.local.GestionEntity
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.PotencialComercial
import cl.duocuc.evaluacion2.data.model.Prioridad
import cl.duocuc.evaluacion2.data.model.TipoAccionHistorial
import cl.duocuc.evaluacion2.data.model.TipoGestion
import cl.duocuc.evaluacion2.data.model.TipoOportunidad

class HistorialDescripcionesTest : StringSpec({
    "Creacion incluye estado prioridad y responsable" { HistorialDescripcionesTestCases().creacionIncluyeEstadoPrioridadYResponsable() }
    "Creacion sin responsable lo indica" { HistorialDescripcionesTestCases().creacionSinResponsableLoIndica() }
    "Actualizacion detecta cambio de estado" { HistorialDescripcionesTestCases().actualizacionDetectaCambioDeEstado() }
    "Actualizacion detecta cambio de prioridad" { HistorialDescripcionesTestCases().actualizacionDetectaCambioDePrioridad() }
    "Actualizacion detecta asignacion de responsable" { HistorialDescripcionesTestCases().actualizacionDetectaAsignacionDeResponsable() }
    "Actualizacion detecta reasignacion de responsable" { HistorialDescripcionesTestCases().actualizacionDetectaReasignacionDeResponsable() }
    "Actualizacion sin cambios devuelve lista vacia" { HistorialDescripcionesTestCases().actualizacionSinCambiosDevuelveListaVacia() }
    "Actualizacion detecta edicion de datos" { HistorialDescripcionesTestCases().actualizacionDetectaEdicionDeDatos() }
    "Gestion incluye tipo detalle y cambio de estado" { HistorialDescripcionesTestCases().gestionIncluyeTipoDetalleYCambioDeEstado() }
    "Gestion sin cambio de estado no menciona estado" { HistorialDescripcionesTestCases().gestionSinCambioDeEstadoNoMencionaEstado() }
    "Gestion sin descripcion solo indica el tipo" { HistorialDescripcionesTestCases().gestionSinDescripcionSoloIndicaElTipo() }
    "Gestion de tipo generico no repite la palabra" { HistorialDescripcionesTestCases().gestionDeTipoGenericoNoRepiteLaPalabra() }
})

private class HistorialDescripcionesTestCases {

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

    fun creacionIncluyeEstadoPrioridadYResponsable() {
        val cambio = HistorialDescripciones.creacion(oportunidad(), "Dalí Morales")

        (cambio.tipoAccion) shouldBe (TipoAccionHistorial.CREACION)
        (cambio.descripcion.contains("Evento identificado")) shouldBe true
        (cambio.descripcion.contains("Media")) shouldBe true
        (cambio.descripcion.contains("Dalí Morales")) shouldBe true
    }

    fun creacionSinResponsableLoIndica() {
        val cambio = HistorialDescripciones.creacion(
            oportunidad(responsableId = null),
            null
        )

        (cambio.descripcion.contains("Sin responsable")) shouldBe true
    }

    fun actualizacionDetectaCambioDeEstado() {
        val cambios = HistorialDescripciones.actualizacion(
            anterior = oportunidad(estado = EstadoGestion.EVENTO_IDENTIFICADO.name),
            nueva = oportunidad(estado = EstadoGestion.CONTACTO_REALIZADO.name),
            responsableAnterior = "Dalí Morales",
            responsableNuevo = "Dalí Morales"
        )

        (cambios.size) shouldBe (1)
        (cambios.first().tipoAccion) shouldBe (TipoAccionHistorial.CAMBIO_ESTADO)
        (cambios.first().descripcion.contains("Evento identificado")) shouldBe true
        (cambios.first().descripcion.contains("Contacto realizado")) shouldBe true
    }

    fun actualizacionDetectaCambioDePrioridad() {
        val cambios = HistorialDescripciones.actualizacion(
            anterior = oportunidad(prioridad = Prioridad.ALTA.name),
            nueva = oportunidad(prioridad = Prioridad.BAJA.name),
            responsableAnterior = "Dalí Morales",
            responsableNuevo = "Dalí Morales"
        )

        (cambios.size) shouldBe (1)
        (cambios.first().tipoAccion) shouldBe (TipoAccionHistorial.CAMBIO_PRIORIDAD)
        (cambios.first().descripcion.contains("Alta")) shouldBe true
        (cambios.first().descripcion.contains("Baja")) shouldBe true
    }

    fun actualizacionDetectaAsignacionDeResponsable() {
        val cambios = HistorialDescripciones.actualizacion(
            anterior = oportunidad(responsableId = null),
            nueva = oportunidad(responsableId = 2L),
            responsableAnterior = null,
            responsableNuevo = "Maxine Moreno"
        )

        (cambios.size) shouldBe (1)
        (cambios.first().tipoAccion) shouldBe (TipoAccionHistorial.ASIGNACION_RESPONSABLE)
        (cambios.first().descripcion.contains("Responsable asignado: Maxine Moreno")) shouldBe true
    }

    fun actualizacionDetectaReasignacionDeResponsable() {
        val cambios = HistorialDescripciones.actualizacion(
            anterior = oportunidad(responsableId = 1L),
            nueva = oportunidad(responsableId = 2L),
            responsableAnterior = "Dalí Morales",
            responsableNuevo = "Maxine Moreno"
        )

        (cambios.size) shouldBe (1)
        (cambios.first().tipoAccion) shouldBe (TipoAccionHistorial.REASIGNACION_RESPONSABLE)
        (cambios.first().descripcion.contains("Responsable reasignado: Dalí Morales → Maxine Moreno")) shouldBe true
    }

    fun actualizacionSinCambiosDevuelveListaVacia() {
        val cambios = HistorialDescripciones.actualizacion(
            anterior = oportunidad(),
            nueva = oportunidad(),
            responsableAnterior = "Dalí Morales",
            responsableNuevo = "Dalí Morales"
        )

        (cambios.isEmpty()) shouldBe true
    }

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

        (cambios.size) shouldBe (1)
        (cambios.first().tipoAccion) shouldBe (TipoAccionHistorial.EDICION)
        (cambios.first().descripcion.contains("Nombre")) shouldBe true
        (cambios.first().descripcion.contains("Descripción")) shouldBe true
    }

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

        (cambio.tipoAccion) shouldBe (TipoAccionHistorial.GESTION)
        (cambio.descripcion.contains("Gestión de Contacto registrada")) shouldBe true
        (cambio.descripcion.contains("Llamada realizada con la organización")) shouldBe true
        (cambio.descripcion.contains("Estado: Evento identificado → Contacto realizado")) shouldBe true
    }

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

        (cambio.tipoAccion) shouldBe (TipoAccionHistorial.GESTION)
        (cambio.descripcion.contains("Estado:")) shouldBe false
    }

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

        (cambio.descripcion) shouldBe ("Gestión de Contacto registrada.")
    }

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

        (cambio.descripcion) shouldBe ("Gestión registrada.")
    }
}
