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
    val mensajeError: String? = null,
    val mensajeExito: String? = null
)

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

    fun onResponsableChange(valor: Long?) = cambiar { it.copy(responsableId = valor) }
    fun onEspacioChange(valor: String) = cambiar { it.copy(espacio = valor) }
    fun onFechaChange(valor: Long?) = cambiar { it.copy(fecha = valor) }
    fun onCostoChange(valor: String) = cambiar { it.copy(costo = valor) }
    fun onRequisitosChange(valor: String) = cambiar { it.copy(requisitos = valor) }
    fun onObservacionesCoordinacionChange(valor: String) =
        cambiar { it.copy(observacionesCoordinacion = valor) }
    fun onResultadoFinalChange(valor: String) = cambiar { it.copy(resultadoFinal = valor) }
    fun onObservacionesChange(valor: String) = cambiar { it.copy(observaciones = valor) }
    fun onImpactoChange(valor: String) = cambiar { it.copy(impacto = valor) }
    fun onFechaRealizacionChange(valor: Long?) = cambiar { it.copy(fechaRealizacion = valor) }
    fun onAsistentesEstimadosChange(valor: String) =
        cambiar { it.copy(asistentesEstimados = valor) }
    fun onContactosGeneradosChange(valor: String) =
        cambiar { it.copy(contactosGenerados = valor) }
    fun onPersonasInteresadasChange(valor: String) =
        cambiar { it.copy(personasInteresadas = valor) }
    fun onInscripcionesChange(valor: String) = cambiar { it.copy(inscripciones = valor) }

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
        if (responsableId == null || state.usuarios.none { it.id == responsableId }) {
            informarError("Selecciona un responsable disponible.")
            return
        }
        if (state.costo.isNotBlank() && (costo == null || !costo.isFinite() || costo < 0)) {
            informarError("El costo debe ser un número igual o mayor que cero.")
            return
        }
        if (state.guardando) return
        if (!state.oportunidadConfirmada) {
            informarError("La oportunidad debe estar confirmada antes de coordinar la activación.")
            return
        }

        cambiar { it.copy(guardando = true, mensajeError = null, mensajeExito = null) }
        viewModelScope.launch {
            try {
                val id = repository.guardarActivacion(
                    ActivacionEntity(
                        id = state.activacionId ?: 0,
                        oportunidadId = state.oportunidadId,
                        participacionConfirmada = state.participacionConfirmada,
                        responsableId = responsableId,
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
            informarError("Selecciona el resultado final de la oportunidad.")
            return
        }
        val asistentesEstimados = state.asistentesEstimados.toIntOrNull()
        val contactosGenerados = cantidadOpcional(state.contactosGenerados)
        val personasInteresadas = cantidadOpcional(state.personasInteresadas)
        val inscripciones = cantidadOpcional(state.inscripciones)
        if (state.asistentesEstimados.isNotBlank() &&
            (asistentesEstimados == null || asistentesEstimados < 0) ||
            contactosGenerados == null || contactosGenerados < 0 ||
            personasInteresadas == null || personasInteresadas < 0 ||
            inscripciones == null || inscripciones < 0
        ) {
            informarError("Las cantidades deben ser números iguales o mayores que cero.")
            return
        }
        if (state.guardando) return

        cambiar { it.copy(guardando = true, mensajeError = null, mensajeExito = null) }
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
                    contactosGenerados = contactosGenerados,
                    personasInteresadas = personasInteresadas,
                    inscripciones = inscripciones
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
                            contactosGenerados = contactosGenerados,
                            personasInteresadas = personasInteresadas,
                            inscripciones = inscripciones
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

    private fun cambiar(transform: (ActivacionUiState) -> ActivacionUiState) {
        _uiState.update { transform(it).copy(mensajeError = null, mensajeExito = null) }
    }

    private fun informarError(mensaje: String) {
        _uiState.update { it.copy(mensajeError = mensaje, mensajeExito = null) }
    }

    private fun cantidadOpcional(valor: String): Int? =
        if (valor.isBlank()) 0 else valor.toIntOrNull()

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
