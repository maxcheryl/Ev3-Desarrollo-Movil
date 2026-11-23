package cl.duocuc.evaluacion2.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.GestionEntity
import cl.duocuc.evaluacion2.data.local.HistorialCambioEntity
import cl.duocuc.evaluacion2.data.model.PerfilUsuario
import cl.duocuc.evaluacion2.data.repository.GestionRepository
import cl.duocuc.evaluacion2.data.repository.HistorialRepository
import cl.duocuc.evaluacion2.data.repository.UsuarioRepository
import cl.duocuc.evaluacion2.data.session.SessionManager
import cl.duocuc.evaluacion2.validation.GestionValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GestionViewModel(
    application: Application,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = GestionRepository(database)
    private val historialRepository = HistorialRepository(database)
    private val usuarioRepository = UsuarioRepository(database.usuarioDao())

    private val _oportunidadId = MutableStateFlow<Long?>(null)

    val gestiones: StateFlow<List<GestionEntity>> = _oportunidadId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList<GestionEntity>()) else repository.observePorOportunidad(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val historial: StateFlow<List<HistorialCambioEntity>> = _oportunidadId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(emptyList<HistorialCambioEntity>())
            } else {
                historialRepository.observePorOportunidad(id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val usuarios: StateFlow<Map<Long, String>> = usuarioRepository.observeAll()
        .map { lista -> lista.associate { it.id to it.nombre } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val esEjecutivo: StateFlow<Boolean> = sessionManager.usuario
        .map { it?.perfil == PerfilUsuario.EJECUTIVO_COMERCIAL }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _editor = MutableStateFlow(GestionEditorUiState())
    val editor: StateFlow<GestionEditorUiState> = _editor.asStateFlow()

    fun seleccionarOportunidad(id: Long) {
        if (_oportunidadId.value == id) return
        _oportunidadId.value = id
        _editor.value = GestionEditorUiState()
    }

    fun beginNueva() {
        val usuario = sessionManager.actual
        if (usuario == null) {
            _editor.value = GestionEditorUiState(
                mensajeError = "La sesión no está activa. Inicia sesión nuevamente."
            )
            return
        }
        if (usuario.perfil != PerfilUsuario.EJECUTIVO_COMERCIAL) {
            _editor.value = GestionEditorUiState(
                mensajeError = "Solo el Ejecutivo comercial puede registrar gestiones."
            )
            return
        }
        val oportunidadId = _oportunidadId.value
        if (oportunidadId == null) {
            _editor.value = GestionEditorUiState(
                mensajeError = "No se encontró la oportunidad solicitada."
            )
            return
        }

        _editor.value = GestionEditorUiState()

        viewModelScope.launch {
            val oportunidad = repository.buscarOportunidad(oportunidadId)
            if (oportunidad == null) {
                _editor.update {
                    it.copy(mensajeError = "La oportunidad ya no está disponible.")
                }
                return@launch
            }
            if (oportunidad.responsableId != usuario.id) {
                _editor.update {
                    it.copy(mensajeError = "No tienes permiso para gestionar esta oportunidad.")
                }
                return@launch
            }
            _editor.update { state ->
                if (state.form.estado.isBlank()) {
                    state.copy(
                        form = state.form.copy(estado = oportunidad.estado),
                        errores = state.errores.copy(estado = null)
                    )
                } else {
                    state
                }
            }
        }
    }

    fun onTipoChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(tipo = valor),
                errores = it.errores.copy(tipo = null)
            )
        }
    }

    fun onEstadoChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(estado = valor),
                errores = it.errores.copy(estado = null)
            )
        }
    }

    fun onDescripcionChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(descripcion = valor),
                errores = it.errores.copy(descripcion = null)
            )
        }
    }

    fun onResultadoChange(valor: String) {
        _editor.update {
            it.copy(form = it.form.copy(resultado = valor))
        }
    }

    fun onProximaAccionChange(valor: String) {
        _editor.update {
            it.copy(form = it.form.copy(proximaAccion = valor))
        }
    }

    fun onFechaProximaAccionChange(valor: Long?) {
        _editor.update {
            it.copy(form = it.form.copy(fechaProximaAccion = valor))
        }
    }

    fun guardar(onListo: () -> Unit) {
        val usuario = sessionManager.actual
        if (usuario == null) {
            _editor.update {
                it.copy(mensajeError = "La sesión no está activa. Inicia sesión nuevamente.")
            }
            return
        }
        if (usuario.perfil != PerfilUsuario.EJECUTIVO_COMERCIAL) {
            _editor.update {
                it.copy(mensajeError = "Solo el Ejecutivo comercial puede registrar gestiones.")
            }
            return
        }
        val oportunidadId = _oportunidadId.value
        if (oportunidadId == null) {
            _editor.update {
                it.copy(mensajeError = "No se encontró la oportunidad solicitada.")
            }
            return
        }

        val actual = _editor.value
        val errores = GestionValidator.validar(actual.form)

        if (errores.hayErrores) {
            _editor.update { it.copy(errores = errores) }
            return
        }

        if (actual.guardando) return
        _editor.update { it.copy(guardando = true, mensajeError = null) }

        viewModelScope.launch {
            try {
                val oportunidad = repository.buscarOportunidad(oportunidadId)
                if (oportunidad == null) {
                    _editor.update {
                        it.copy(
                            guardando = false,
                            mensajeError = "La oportunidad ya no está disponible."
                        )
                    }
                    return@launch
                }
                if (oportunidad.responsableId != usuario.id) {
                    _editor.update {
                        it.copy(
                            guardando = false,
                            mensajeError = "No tienes permiso para gestionar esta oportunidad."
                        )
                    }
                    return@launch
                }

                val gestion = GestionEntity(
                    oportunidadId = oportunidadId,
                    usuarioId = usuario.id,
                    tipo = actual.form.tipo,
                    descripcion = actual.form.descripcion.trim(),
                    resultado = actual.form.resultado.trim(),
                    proximaAccion = actual.form.proximaAccion.trim(),
                    fechaProximaAccion = actual.form.fechaProximaAccion,
                    fecha = System.currentTimeMillis()
                )

                repository.registrar(gestion, actual.form.estado)
                _editor.value = GestionEditorUiState()
                onListo()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _editor.update {
                    it.copy(
                        guardando = false,
                        mensajeError = "No se pudo registrar la gestión: ${e.message}"
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            application: Application,
            sessionManager: SessionManager
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(GestionViewModel::class.java))
                    return GestionViewModel(application, sessionManager) as T
                }
            }
    }
}
