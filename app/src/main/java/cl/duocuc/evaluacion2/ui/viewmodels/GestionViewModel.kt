package cl.duocuc.evaluacion2.ui.viewmodels

import android.app.Application
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

/** Carga gestiones e historial y revisa quién puede guardar cambios. */
class GestionViewModel(
    private val repository: GestionRepository,
    private val historialRepository: HistorialRepository,
    private val usuarioRepository: UsuarioRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _oportunidadId = MutableStateFlow<Long?>(null)

    // Cambia la lista y el historial cuando se elige otra oportunidad.
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
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            sessionManager.actual?.perfil == PerfilUsuario.EJECUTIVO_COMERCIAL
        )

    val puedeRegistrarSeguimiento: StateFlow<Boolean> = sessionManager.usuario
        .map {
            it != null && tienePermisoParaSeguimiento(it.perfil)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            sessionManager.actual?.perfil?.let(::tienePermisoParaSeguimiento) == true
        )

    private val _editor = MutableStateFlow(GestionEditorUiState())
    val editor: StateFlow<GestionEditorUiState> = _editor.asStateFlow()

    fun seleccionarOportunidad(id: Long) {
        if (_oportunidadId.value == id) return
        // Limpia el formulario anterior para no mezclar datos de oportunidades distintas.
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
        if (!tienePermisoParaSeguimiento(usuario.perfil)) {
            _editor.value = GestionEditorUiState(
                mensajeError = "No tienes permiso para registrar seguimientos."
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

        // Comprueba que la oportunidad siga disponible y que el usuario pueda gestionarla.
        viewModelScope.launch {
            val oportunidad = repository.buscarOportunidad(oportunidadId)
            if (oportunidad == null) {
                _editor.update {
                    it.copy(mensajeError = "La oportunidad ya no está disponible.")
                }
                return@launch
            }
            if (
                usuario.perfil == PerfilUsuario.EJECUTIVO_COMERCIAL &&
                oportunidad.responsableId != usuario.id
            ) {
                _editor.update {
                    it.copy(mensajeError = "No tienes permiso para gestionar esta oportunidad.")
                }
                return@launch
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
            it.copy(
                form = it.form.copy(resultado = valor),
                errores = it.errores.copy(descripcion = null)
            )
        }
    }

    fun onProximaAccionChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(proximaAccion = valor),
                errores = it.errores.copy(proximaAccion = null)
            )
        }
    }

    fun onFechaProximaAccionChange(valor: Long?) {
        _editor.update {
            it.copy(
                form = it.form.copy(
                    fechaProximaAccion = valor,
                    // Al elegir una fecha sin hora, propone las 09:00 como hora inicial.
                    horaProximaAccion = when {
                        valor == null -> it.form.horaProximaAccion
                        it.form.horaProximaAccion.isBlank() -> "09:00"
                        else -> it.form.horaProximaAccion
                    }
                ),
                errores = it.errores.copy(
                    fechaProximaAccion = null,
                    horaProximaAccion = null
                )
            )
        }
    }

    fun onFechaGestionChange(valor: Long?) {
        _editor.update {
            it.copy(
                form = it.form.copy(fechaGestion = valor),
                errores = it.errores.copy(
                    fechaGestion = null,
                    fechaProximaAccion = null,
                    horaProximaAccion = null
                )
            )
        }
    }

    fun onHoraGestionChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(horaGestion = valor),
                errores = it.errores.copy(horaGestion = null, horaProximaAccion = null)
            )
        }
    }

    fun onHoraProximaAccionChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(horaProximaAccion = valor),
                errores = it.errores.copy(horaProximaAccion = null)
            )
        }
    }

    fun onObservacionesChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(observaciones = valor),
                errores = it.errores.copy(observaciones = null)
            )
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
        if (!tienePermisoParaSeguimiento(usuario.perfil)) {
            _editor.update {
                it.copy(mensajeError = "No tienes permiso para registrar seguimientos.")
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
                if (
                    usuario.perfil == PerfilUsuario.EJECUTIVO_COMERCIAL &&
                    oportunidad.responsableId != usuario.id
                ) {
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
                    descripcion = actual.form.observaciones.trim(),
                    resultado = actual.form.resultado.trim(),
                    proximaAccion = actual.form.proximaAccion.trim(),
                    fechaProximaAccion = actual.form.fechaProximaAccion?.let {
                        combinarFechaHora(it, actual.form.horaProximaAccion)
                    },
                    fecha = actual.form.fechaGestion?.let {
                        combinarFechaHora(it, actual.form.horaGestion)
                    } ?: System.currentTimeMillis()
                )

                repository.registrar(
                    gestion,
                    actual.form.estado
                        .takeIf(String::isNotBlank)
                        .takeIf { usuario.perfil == PerfilUsuario.EJECUTIVO_COMERCIAL }
                )
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

    private fun combinarFechaHora(fecha: Long, hora: String): Long {
        val partes = hora.split(':')
        val horaNumerica = partes.getOrNull(0)?.toIntOrNull() ?: return fecha
        val minutoNumerico = partes.getOrNull(1)?.toIntOrNull() ?: return fecha
        val selector = java.util.Calendar.getInstance(
            java.util.TimeZone.getTimeZone("UTC")
        ).apply { timeInMillis = fecha }
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = fecha
            set(java.util.Calendar.YEAR, selector.get(java.util.Calendar.YEAR))
            set(java.util.Calendar.MONTH, selector.get(java.util.Calendar.MONTH))
            set(java.util.Calendar.DAY_OF_MONTH, selector.get(java.util.Calendar.DAY_OF_MONTH))
            set(java.util.Calendar.HOUR_OF_DAY, horaNumerica)
            set(java.util.Calendar.MINUTE, minutoNumerico)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun tienePermisoParaSeguimiento(perfil: PerfilUsuario): Boolean =
        perfil == PerfilUsuario.EJECUTIVO_COMERCIAL ||
            perfil == PerfilUsuario.SUPERVISOR_COMERCIAL

    companion object {
        fun factory(
            application: Application,
            sessionManager: SessionManager
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(GestionViewModel::class.java))
                    val database = AppDatabase.getInstance(application)
                    return GestionViewModel(
                        repository = GestionRepository(database),
                        historialRepository = HistorialRepository(database),
                        usuarioRepository = UsuarioRepository(database.usuarioDao()),
                        sessionManager = sessionManager
                    ) as T
                }
            }
    }
}
