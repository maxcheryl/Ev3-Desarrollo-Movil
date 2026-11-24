package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.historial.HistorialDescripciones
import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.DatabaseSeed
import cl.duocuc.evaluacion2.data.local.GestionDao
import cl.duocuc.evaluacion2.data.local.HistorialCambioEntity
import cl.duocuc.evaluacion2.data.local.HistorialCambioDao
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import cl.duocuc.evaluacion2.data.local.OpportunityDao
import cl.duocuc.evaluacion2.data.local.UsuarioDao
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.PotencialComercial
import cl.duocuc.evaluacion2.data.model.Prioridad
import cl.duocuc.evaluacion2.data.model.TipoOportunidad
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de oportunidades: única puerta de acceso a la persistencia
 * local (Room). La UI y los ViewModels no conocen la base de datos.
 */
class OportunidadRepository(
    private val dao: OpportunityDao,
    private val historialDao: HistorialCambioDao,
    private val usuarioDao: UsuarioDao,
    private val gestionDao: GestionDao,
    private val ejecutorTransacciones: EjecutorTransacciones
) {

    constructor(database: AppDatabase) : this(
        dao = database.opportunityDao(),
        historialDao = database.historialCambioDao(),
        usuarioDao = database.usuarioDao(),
        gestionDao = database.gestionDao(),
        ejecutorTransacciones = RoomEjecutorTransacciones(database)
    )

    fun observeAll(): Flow<List<OpportunityEntity>> = dao.observeAll()

    suspend fun contar(): Int = dao.count()

    suspend fun buscarPorId(id: Long): OpportunityEntity? = dao.buscarPorId(id)

    suspend fun asociarEvento(oportunidadId: Long, eventoId: Long) {
        val filasActualizadas = dao.asociarEvento(
            oportunidadId = oportunidadId,
            eventoId = eventoId,
            fechaActualizacion = System.currentTimeMillis()
        )
        check(filasActualizadas == 1) { "La oportunidad ya no está disponible." }
    }

    suspend fun asignarResponsablesPendientes(responsableId: Long): Int =
        dao.assignNullResponsible(responsableId)

    suspend fun eliminar(id: Long): Int = ejecutorTransacciones.ejecutar {
        gestionDao.deleteByOportunidad(id)
        historialDao.deleteByOportunidad(id)
        dao.deleteById(id)
    }

    /** Inserta una oportunidad nueva o actualiza una existente. Devuelve el id. */
    suspend fun guardar(oportunidad: OpportunityEntity, usuarioId: Long): Long =
        ejecutorTransacciones.ejecutar {
            if (oportunidad.id == 0L) {
                val id = dao.insert(oportunidad)
                val cambio = HistorialDescripciones.creacion(
                    oportunidad = oportunidad,
                    responsable = nombreResponsable(oportunidad.responsableId)
                )
                historialDao.insert(
                    HistorialCambioEntity(
                        oportunidadId = id,
                        usuarioId = usuarioId,
                        tipoAccion = cambio.tipoAccion.name,
                        descripcion = cambio.descripcion,
                        fecha = oportunidad.fechaCreacion
                    )
                )
                id
            } else {
                val anterior = dao.buscarPorId(oportunidad.id)
                    ?: error("La oportunidad ya no está disponible.")
                val cambios = HistorialDescripciones.actualizacion(
                    anterior = anterior,
                    nueva = oportunidad,
                    responsableAnterior = nombreResponsable(anterior.responsableId),
                    responsableNuevo = nombreResponsable(oportunidad.responsableId)
                )
                val ahora = System.currentTimeMillis()
                // Al editar se conserva la fecha de creación original y solo se mueve la de actualización
                dao.update(
                    oportunidad.copy(
                        fechaCreacion = anterior.fechaCreacion,
                        fechaActualizacion = ahora
                    )
                )
                cambios.forEach { cambio ->
                    historialDao.insert(
                        HistorialCambioEntity(
                            oportunidadId = oportunidad.id,
                            usuarioId = usuarioId,
                            tipoAccion = cambio.tipoAccion.name,
                            descripcion = cambio.descripcion,
                            fecha = ahora
                        )
                    )
                }
                oportunidad.id
            }
        }

    /** Completa el historial de creación de oportunidades antiguas o de ejemplo. */
    suspend fun completarHistorialCreacion(usuarioId: Long) {
        ejecutorTransacciones.ejecutar {
            dao.listarSinHistorial().forEach { oportunidad ->
                val cambio = HistorialDescripciones.creacion(
                    oportunidad = oportunidad,
                    responsable = nombreResponsable(oportunidad.responsableId)
                )
                historialDao.insert(
                    HistorialCambioEntity(
                        oportunidadId = oportunidad.id,
                        usuarioId = oportunidad.responsableId ?: usuarioId,
                        tipoAccion = cambio.tipoAccion.name,
                        descripcion = cambio.descripcion,
                        fecha = oportunidad.fechaCreacion
                    )
                )
            }
        }
    }

    /** Carga datos ficticios de ejemplo solo si la tabla está vacía. */
    suspend fun semillarSiVacia() {
        if (contar() > 0) return
        datosDeEjemplo().forEach { dao.insert(it) }
    }

    private suspend fun nombreResponsable(id: Long?): String? =
        id?.let { usuarioDao.buscarPorId(it)?.nombre ?: "Usuario #$it" }

    private fun datosDeEjemplo(): List<OpportunityEntity> {
        val ahora = System.currentTimeMillis()
        val unDia = 24 * 60 * 60 * 1000L

        return listOf(
            OpportunityEntity(
                nombre = "Feria de bienestar laboral",
                tipo = TipoOportunidad.JORNADA_DE_BENEFICIOS.name,
                descripcion = "Jornada de beneficios dirigida a colaboradores de Corporación Horizonte, con stands de salud, servicios y actividades para las familias.",
                fuente = "Referencia",
                prioridad = Prioridad.ALTA.name,
                potencialComercial = PotencialComercial.ALTO.name,
                estado = EstadoGestion.PROPUESTA_ENVIADA.name,
                observaciones = "Datos de demostración ficticios. Buscar contacto del área de eventos.",
                codigoQr = "MAX:OP-001",
                responsableId = DatabaseSeed.USUARIO_DALI_ID,
                publicoEsperado = "120 colaboradores",
                fechaCreacion = ahora - 5 * unDia,
                fechaActualizacion = ahora - 1 * unDia
            ),
            OpportunityEntity(
                nombre = "Encuentro de proveedores locales",
                tipo = TipoOportunidad.ACTIVIDAD_MUNICIPAL.name,
                descripcion = "Actividad municipal que reúne a proveedores de la zona; oportunidad de activación con público familiar y comercio local.",
                fuente = "Redes sociales",
                prioridad = Prioridad.MEDIA.name,
                potencialComercial = PotencialComercial.MEDIO.name,
                estado = EstadoGestion.CONTACTO_PENDIENTE.name,
                observaciones = "Datos de demostración ficticios.",
                codigoQr = "MAX:OP-002",
                responsableId = DatabaseSeed.USUARIO_DALI_ID,
                publicoEsperado = "300 asistentes",
                fechaCreacion = ahora - 3 * unDia,
                fechaActualizacion = ahora - 2 * unDia
            ),
            OpportunityEntity(
                nombre = "Expo empresas y beneficios",
                tipo = TipoOportunidad.FERIA_EMPRESARIAL.name,
                descripcion = "Feria empresarial en el Centro Empresarial Andino donde las empresas presentan beneficios corporativos a trabajadores de la zona.",
                fuente = "Sitio web",
                prioridad = Prioridad.ALTA.name,
                potencialComercial = PotencialComercial.ALTO.name,
                estado = EstadoGestion.EVENTO_IDENTIFICADO.name,
                observaciones = "Datos de demostración ficticios. Evaluar costo de stand.",
                codigoQr = "MAX:OP-003",
                responsableId = DatabaseSeed.USUARIO_DALI_ID,
                publicoEsperado = "500 visitantes",
                fechaCreacion = ahora - 1 * unDia,
                fechaActualizacion = ahora
            )
        )
    }
}
