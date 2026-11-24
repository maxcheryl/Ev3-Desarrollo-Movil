package cl.duocuc.evaluacion2.ui.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duocuc.evaluacion2.data.local.ActivacionEntity
import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.EvidenciaEntity
import cl.duocuc.evaluacion2.data.local.EventoEntity
import cl.duocuc.evaluacion2.data.local.ResultadoEntity
import cl.duocuc.evaluacion2.data.local.UsuarioEntity
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.repository.ActivacionRepository
import cl.duocuc.evaluacion2.data.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivacionUiState(
    val oportunidadId: Long = 0,
    val oportunidadNombre: String = "",
    val activacionId: Long? = null,
    val evidencias: List<EvidenciaEntity> = emptyList(),
    val evento: EventoEntity? = null,
    val resultadoGuardado: ResultadoEntity? = null,
    val usuarios: List<UsuarioEntity> = emptyList(),
    val participacionConfirmada: Boolean = false,
    val oportunidadConfirmada: Boolean = false,
    val responsableId: Long? = null,
    val espacio: String = "",
    val fecha: Long? = null,
    val costo: String = "",
    val requisitos: String = "",
    val observacionesCoordinacion: String = "",
    val resultadoFinal: String = "",
    val observaciones: String = "",
    val impacto: String = "",
    val fechaRealizacion: Long? = null,
    val asistentesEstimados: String = "",
    val contactosGenerados: String = "0",
    val personasInteresadas: String = "0",
    val inscripciones: String = "0",
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val errores: ActivacionErrores = ActivacionErrores(),
    val mensajeError: String? = null,
    val mensajeExito: String? = null
)

data class ActivacionErrores(
    val responsable: String? = null,
    val fecha: String? = null,
    val espacio: String? = null,
    val costo: String? = null,
    val requisitos: String? = null,
    val observacionesCoordinacion: String? = null,
    val resultadoFinal: String? = null,
    val fechaRealizacion: String? = null,
    val asistentesEstimados: String? = null,
    val contactosGenerados: String? = null,
    val personasInteresadas: String? = null,
    val inscripciones: String? = null,
    val observaciones: String? = null,
    val impacto: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(
            responsable,
            fecha,
            espacio,
            costo,
            requisitos,
            observacionesCoordinacion,
            resultadoFinal,
            fechaRealizacion,
            asistentesEstimados,
            contactosGenerados,
            personasInteresadas,
            inscripciones,
            observaciones,
            impacto
        ).any { it != null }
}

class ActivacionViewModel(
    private val repository: ActivacionRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActivacionUiState())
    val uiState: StateFlow<ActivacionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeUsuarios().collect { usuarios ->
                _uiState.update { it.copy(usuarios = usuarios) }
            }
        }
    }

    fun cargar(oportunidadId: Long) {
        if (_uiState.value.oportunidadId == oportunidadId && !_uiState.value.cargando) return
        val usuarios = _uiState.value.usuarios
        _uiState.value = ActivacionUiState(
            oportunidadId = oportunidadId,
            usuarios = usuarios,
            cargando = true
        )
        viewModelScope.launch {
            try {
                val oportunidad = repository.obtenerOportunidad(oportunidadId)
                    ?: error("La oportunidad ya no está disponible.")
                val activacion = repository.obtenerActivacion(oportunidadId)
                val evidencias = activacion?.let {
                    repository.obtenerEvidencias(it.id)
                }.orEmpty()
                val resultado = activacion?.let {
                    repository.obtenerResultado(it.id)
                }
                val evento = oportunidad.eventoId?.let { repository.obtenerEvento(it) }
                _uiState.update {
                    it.copy(
                        oportunidadNombre = oportunidad.nombre,
                        activacionId = activacion?.id,
                        evidencias = evidencias,
                        evento = evento,
                        resultadoGuardado = resultado,
                        participacionConfirmada = activacion?.participacionConfirmada ?: false,
                        oportunidadConfirmada = oportunidad.estado in setOf(
                            EstadoGestion.PARTICIPACION_CONFIRMADA.name,
                            EstadoGestion.ACTIVACION_REALIZADA.name,
                            EstadoGestion.ACTIVACION_CANCELADA.name
                        ),
                        responsableId = activacion?.responsableId
                            ?: sessionManager.actual?.id
                            ?: it.usuarios.firstOrNull()?.id,
                        espacio = activacion?.espacio.orEmpty(),
                        fecha = activacion?.fecha,
                        costo = activacion?.costo?.toString().orEmpty(),
                        requisitos = activacion?.requisitos.orEmpty(),
                        observacionesCoordinacion =
                            activacion?.observacionesCoordinacion.orEmpty(),
                        resultadoFinal = resultado?.resultado
                            ?.takeIf {
                                it == EstadoGestion.ACTIVACION_REALIZADA.name ||
                                    it == EstadoGestion.ACTIVACION_CANCELADA.name
                            }
                            ?: oportunidad.estado.takeIf {
                                it == EstadoGestion.ACTIVACION_REALIZADA.name ||
                                    it == EstadoGestion.ACTIVACION_CANCELADA.name
                            }.orEmpty(),
                        observaciones = resultado?.observaciones.orEmpty(),
                        impacto = resultado?.impacto.orEmpty(),
                        fechaRealizacion = resultado?.fechaRealizacion,
                        asistentesEstimados = resultado?.asistentesEstimados?.toString().orEmpty(),
                        contactosGenerados = resultado?.contactosGenerados?.toString() ?: "0",
                        personasInteresadas = resultado?.personasInteresadas?.toString() ?: "0",
                        inscripciones = resultado?.inscripciones?.toString() ?: "0",
                        cargando = false,
                        mensajeError = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        cargando = false,
                        mensajeError = "No se pudo cargar la activación: ${e.message}"
                    )
                }
            }
        }
    }

    fun onResponsableChange(valor: Long?) = cambiar(
        transform = { it.copy(responsableId = valor) },
        limpiarError = { it.copy(responsable = null) }
    )
    fun onEspacioChange(valor: String) = cambiar(
        transform = { it.copy(espacio = valor) },
        limpiarError = { it.copy(espacio = null) }
    )
    fun onFechaChange(valor: Long?) = cambiar(
        transform = { it.copy(fecha = valor) },
        limpiarError = { it.copy(fecha = null) }
    )
    fun onCostoChange(valor: String) = cambiar(
        transform = { it.copy(costo = valor) },
        limpiarError = { it.copy(costo = null) }
    )
    fun onRequisitosChange(valor: String) = cambiar(
        transform = { it.copy(requisitos = valor) },
        limpiarError = { it.copy(requisitos = null) }
    )
    fun onObservacionesCoordinacionChange(valor: String) =
        cambiar(
            transform = { it.copy(observacionesCoordinacion = valor) },
            limpiarError = { it.copy(observacionesCoordinacion = null) }
        )
    fun onResultadoFinalChange(valor: String) = cambiar(
        transform = { it.copy(resultadoFinal = valor) },
        limpiarError = {
            it.copy(
                resultadoFinal = null,
                fechaRealizacion = if (
                    valor == EstadoGestion.ACTIVACION_CANCELADA.name
                ) {
                    null
                } else {
                    it.fechaRealizacion
                }
            )
        }
    )
    fun onObservacionesChange(valor: String) = cambiar(
        transform = { it.copy(observaciones = valor) },
        limpiarError = { it.copy(observaciones = null) }
    )
    fun onImpactoChange(valor: String) = cambiar(
        transform = { it.copy(impacto = valor) },
        limpiarError = { it.copy(impacto = null) }
    )
    fun onFechaRealizacionChange(valor: Long?) = cambiar(
        transform = { it.copy(fechaRealizacion = valor) },
        limpiarError = { it.copy(fechaRealizacion = null) }
    )
    fun onAsistentesEstimadosChange(valor: String) = cambiar(
        transform = { it.copy(asistentesEstimados = valor) },
        limpiarError = { it.copy(asistentesEstimados = null) }
    )
    fun onContactosGeneradosChange(valor: String) = cambiar(
        transform = { it.copy(contactosGenerados = valor) },
        limpiarError = { it.copy(contactosGenerados = null) }
    )
    fun onPersonasInteresadasChange(valor: String) = cambiar(
        transform = { it.copy(personasInteresadas = valor) },
        limpiarError = { it.copy(personasInteresadas = null) }
    )
    fun onInscripcionesChange(valor: String) = cambiar(
        transform = { it.copy(inscripciones = valor) },
        limpiarError = { it.copy(inscripciones = null) }
    )

    fun agregarEvidencia(tipo: String, nombre: String, uri: String) {
        val state = _uiState.value
        val activacionId = state.activacionId
        val usuarioId = sessionManager.actual?.id
        if (activacionId == null || usuarioId == null) {
            informarError("Guarda la coordinación antes de adjuntar evidencias.")
            return
        }
        viewModelScope.launch {
            try {
                repository.agregarEvidencia(
                    EvidenciaEntity(
                        activacionId = activacionId,
                        tipo = tipo,
                        nombre = nombre,
                        ruta = uri
                    ),
                    usuarioId
                )
                val evidencias = repository.obtenerEvidencias(activacionId)
                _uiState.update {
                    it.copy(
                        evidencias = evidencias,
                        mensajeError = null,
                        mensajeExito = "Evidencia adjunta."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(mensajeError = "No se pudo adjuntar la evidencia: ${e.message}")
                }
            }
        }
    }

    fun guardarCoordinacion() {
        val state = _uiState.value
        val usuarioId = sessionManager.actual?.id
        val responsableId = state.responsableId
        val costo = state.costo.trim().takeIf(String::isNotEmpty)?.replace(',', '.')?.toDoubleOrNull()
        if (usuarioId == null) {
            informarError("La sesión no está activa. Inicia sesión nuevamente.")
            return
        }
        if (!state.oportunidadConfirmada) {
            informarError("La oportunidad debe estar confirmada antes de coordinar la activación.")
            return
        }
        if (state.guardando) return

        val errores = ActivacionErrores(
            responsable = if (responsableId == null || state.usuarios.none { it.id == responsableId }) {
                "Selecciona un responsable disponible."
            } else {
                null
            },
            fecha = if (state.fecha == null || state.fecha <= 0L) {
                "Selecciona la fecha de realización."
            } else {
                null
            },
            espacio = validarLongitud(state.espacio, "El espacio", 200),
            costo = if (
                state.costo.isNotBlank() &&
                (costo == null || !costo.isFinite() || costo < 0)
            ) {
                "Ingresa un costo numérico igual o mayor que cero."
            } else {
                null
            },
            requisitos = validarLongitud(state.requisitos, "Los requisitos", 2000),
            observacionesCoordinacion = validarLongitud(
                state.observacionesCoordinacion,
                "Las observaciones",
                2000
            )
        )
        if (errores.hayErrores) {
            informarError("Corrige los campos marcados antes de guardar la coordinación.", errores)
            return
        }

        cambiar(
            transform = {
                it.copy(guardando = true, mensajeError = null, mensajeExito = null)
            }
        )
        viewModelScope.launch {
            try {
                val id = repository.guardarActivacion(
                    ActivacionEntity(
                        id = state.activacionId ?: 0,
                        oportunidadId = state.oportunidadId,
                        participacionConfirmada = state.participacionConfirmada,
                        responsableId = checkNotNull(responsableId),
                        espacio = state.espacio.trim(),
                        fecha = state.fecha,
                        costo = costo,
                        requisitos = state.requisitos.trim(),
                        observacionesCoordinacion = state.observacionesCoordinacion.trim()
                    ),
                    usuarioId
                )
                _uiState.update {
                    it.copy(
                        activacionId = id,
                        guardando = false,
                        mensajeError = null,
                        mensajeExito = "Coordinación guardada."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        guardando = false,
                        mensajeError = "No se pudo guardar la coordinación: ${e.message}"
                    )
                }
            }
        }
    }

    fun guardarResultado() {
        val state = _uiState.value
        val activacionId = state.activacionId
        val usuarioId = sessionManager.actual?.id
        if (activacionId == null) {
            informarError("Guarda la coordinación antes de registrar el resultado.")
            return
        }
        if (usuarioId == null) {
            informarError("La sesión no está activa. Inicia sesión nuevamente.")
            return
        }
        if (state.resultadoFinal != EstadoGestion.ACTIVACION_REALIZADA.name &&
            state.resultadoFinal != EstadoGestion.ACTIVACION_CANCELADA.name
        ) {
            informarError(
                "Selecciona el resultado final de la oportunidad.",
                ActivacionErrores(resultadoFinal = "Selecciona un resultado final válido.")
            )
            return
        }
        val asistentesEstimados = state.asistentesEstimados.trim()
            .takeIf(String::isNotEmpty)
            ?.toIntOrNull()
        if (state.guardando) return
        val requiereFecha = state.resultadoFinal == EstadoGestion.ACTIVACION_REALIZADA.name
        val errores = ActivacionErrores(
            fechaRealizacion = when {
                requiereFecha && (state.fechaRealizacion == null || state.fechaRealizacion <= 0L) ->
                    "Selecciona la fecha de realización."
                state.fechaRealizacion != null && state.fechaRealizacion <= 0L ->
                    "Selecciona una fecha válida."
                else -> null
            },
            asistentesEstimados = validarCantidad(
                state.asistentesEstimados,
                "Ingresa un número entero igual o mayor que cero."
            ),
            contactosGenerados = validarCantidad(
                state.contactosGenerados,
                "Ingresa un número entero igual o mayor que cero."
            ),
            personasInteresadas = validarCantidad(
                state.personasInteresadas,
                "Ingresa un número entero igual o mayor que cero."
            ),
            inscripciones = validarCantidad(
                state.inscripciones,
                "Ingresa un número entero igual o mayor que cero."
            ),
            observaciones = validarLongitud(state.observaciones, "Las observaciones", 2000),
            impacto = validarLongitud(state.impacto, "La evaluación", 2000)
        )
        if (errores.hayErrores) {
            informarError("Corrige los campos marcados antes de guardar el resultado.", errores)
            return
        }
        val cantidadContactos = cantidadOpcional(state.contactosGenerados) ?: 0
        val cantidadInteresados = cantidadOpcional(state.personasInteresadas) ?: 0
        val cantidadInscripciones = cantidadOpcional(state.inscripciones) ?: 0

        cambiar(
            transform = {
                it.copy(guardando = true, mensajeError = null, mensajeExito = null)
            }
        )
        viewModelScope.launch {
            try {
                repository.guardarResultado(
                    activacionId = activacionId,
                    resultado = state.resultadoFinal,
                    observaciones = state.observaciones.trim(),
                    impacto = state.impacto.trim(),
                    nuevoEstado = state.resultadoFinal,
                    usuarioId = usuarioId,
                    fechaRealizacion = state.fechaRealizacion,
                    asistentesEstimados = asistentesEstimados,
                    contactosGenerados = cantidadContactos,
                    personasInteresadas = cantidadInteresados,
                    inscripciones = cantidadInscripciones
                )
                _uiState.update {
                    it.copy(
                        resultadoGuardado = ResultadoEntity(
                            activacionId = activacionId,
                            resultado = state.resultadoFinal,
                            observaciones = state.observaciones.trim(),
                            impacto = state.impacto.trim(),
                            fechaRealizacion = state.fechaRealizacion,
                            asistentesEstimados = asistentesEstimados,
                            contactosGenerados = cantidadContactos,
                            personasInteresadas = cantidadInteresados,
                            inscripciones = cantidadInscripciones
                        ),
                        participacionConfirmada =
                            state.resultadoFinal == EstadoGestion.ACTIVACION_REALIZADA.name,
                        guardando = false,
                        mensajeError = null,
                        mensajeExito = "Resultado de activación guardado."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        guardando = false,
                        mensajeError = "No se pudo guardar el resultado: ${e.message}"
                    )
                }
            }
        }
    }

    private fun cambiar(
        transform: (ActivacionUiState) -> ActivacionUiState,
        limpiarError: (ActivacionErrores) -> ActivacionErrores = { it }
    ) {
        _uiState.update {
            transform(it).copy(
                errores = limpiarError(it.errores),
                mensajeError = null,
                mensajeExito = null
            )
        }
    }

    private fun informarError(mensaje: String, errores: ActivacionErrores? = null) {
        _uiState.update {
            it.copy(
                errores = errores ?: it.errores,
                mensajeError = mensaje,
                mensajeExito = null
            )
        }
    }

    private fun cantidadOpcional(valor: String): Int? =
        if (valor.isBlank()) 0 else valor.trim().toIntOrNull()

    private fun validarCantidad(valor: String, mensaje: String): String? {
        if (valor.isBlank()) return null
        val cantidad = valor.trim().toIntOrNull()
        return if (cantidad == null || cantidad < 0) mensaje else null
    }

    private fun validarLongitud(valor: String, campo: String, maximo: Int): String? =
        if (valor.trim().length > maximo) {
            "$campo no puede superar $maximo caracteres."
        } else {
            null
        }

    companion object {
        fun factory(
            application: Application,
            sessionManager: SessionManager
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ActivacionViewModel::class.java))
                    val repository = ActivacionRepository(AppDatabase.getInstance(application))
                    return ActivacionViewModel(repository, sessionManager) as T
                }
            }
    }
}
