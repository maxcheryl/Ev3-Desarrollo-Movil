package cl.duocuc.evaluacion2.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.DatabaseSeed
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import cl.duocuc.evaluacion2.data.model.PerfilUsuario
import cl.duocuc.evaluacion2.data.repository.OportunidadRepository
import cl.duocuc.evaluacion2.data.session.SessionManager
import cl.duocuc.evaluacion2.validation.OportunidadValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del editor de formulario (creación/edición). */
data class OportunidadEditorUiState(
    val form: OportunidadFormState = OportunidadFormState(),
    val errores: OportunidadErrores = OportunidadErrores(),
    val guardando: Boolean = false,
    val mensajeError: String? = null
)

/**
 * ViewModel del módulo de oportunidades.
 * Expone la lista reactiva (Room + Flow) y el estado del editor
 * del formulario de creación/edición.
 */
class OportunidadViewModel(
    application: Application,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    private val repository = OportunidadRepository(
        AppDatabase.getInstance(application).opportunityDao()
    )

    val oportunidades: StateFlow<List<OpportunityEntity>> =
        combine(repository.observeAll(), sessionManager.usuario) { lista, usuario ->
            if (usuario == null) {
                emptyList()
            } else when (usuario.perfil) {
                PerfilUsuario.SUPERVISOR_COMERCIAL -> lista
                PerfilUsuario.EJECUTIVO_COMERCIAL ->
                    lista.filter { it.responsableId == usuario.id }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val esSupervisor: StateFlow<Boolean> = sessionManager.usuario
        .map { it?.perfil == PerfilUsuario.SUPERVISOR_COMERCIAL }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _editor = MutableStateFlow(OportunidadEditorUiState())
    val editor: StateFlow<OportunidadEditorUiState> = _editor.asStateFlow()

    init {
        // Al abrir el módulo se cargan los datos de ejemplo si la tabla está vacía
        viewModelScope.launch {
            try {
                repository.semillarSiVacia()
                repository.asignarResponsablesPendientes(DatabaseSeed.USUARIO_DALI_ID)
            } finally {
                _cargando.value = false
            }
        }
    }

    // ---------------------------------------------------------------------
    // Editor del formulario
    // ---------------------------------------------------------------------

    /** Prepara el formulario para una oportunidad nueva. */
    fun beginNueva() {
        _editor.value = OportunidadEditorUiState(
            form = OportunidadFormState(responsableId = sessionManager.actual?.id)
        )
    }

    /** Prepara el formulario con los datos actuales de una oportunidad. */
    fun beginEditar(id: Long) {
        val existente = oportunidades.value.firstOrNull { it.id == id }
        if (existente == null) {
            _editor.update {
                it.copy(mensajeError = "No se encontró la oportunidad solicitada.")
            }
            return
        }
        if (!puedeGestionar(existente)) {
            _editor.update {
                it.copy(mensajeError = "No tienes permiso para editar esta oportunidad.")
            }
            return
        }
        _editor.value = OportunidadEditorUiState(
            form = OportunidadFormState(
                id = existente.id,
                nombre = existente.nombre,
                tipo = existente.tipo,
                descripcion = existente.descripcion,
                fuente = existente.fuente,
                prioridad = existente.prioridad,
                potencialComercial = existente.potencialComercial,
                estado = existente.estado,
                observaciones = existente.observaciones,
                codigoQr = existente.codigoQr,
                eventoId = existente.eventoId,
                organizacionId = existente.organizacionId,
                contactoId = existente.contactoId,
                responsableId = existente.responsableId,
                publicoEsperado = existente.publicoEsperado,
                fechaCreacion = existente.fechaCreacion
            )
        )
    }

    // Cada actualización limpia el error del campo correspondiente
    // para informar visualmente que el dato fue corregido.

    fun onNombreChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(nombre = valor),
                errores = it.errores.copy(nombre = null)
            )
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

    fun onDescripcionChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(descripcion = valor),
                errores = it.errores.copy(descripcion = null)
            )
        }
    }

    fun onFuenteChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(fuente = valor),
                errores = it.errores.copy(fuente = null)
            )
        }
    }

    fun onPrioridadChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(prioridad = valor),
                errores = it.errores.copy(prioridad = null)
            )
        }
    }

    fun onPotencialComercialChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(potencialComercial = valor),
                errores = it.errores.copy(potencialComercial = null)
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

    fun onObservacionesChange(valor: String) {
        _editor.update {
            it.copy(form = it.form.copy(observaciones = valor))
        }
    }

    /** Valida el formulario y, si es válido, guarda en Room. */
    fun guardarOportunidad(onListo: (Long) -> Unit) {
        val usuario = sessionManager.actual
        if (usuario == null) {
            _editor.update {
                it.copy(mensajeError = "La sesión no está activa. Inicia sesión nuevamente.")
            }
            return
        }

        val actual = _editor.value
        val esNueva = actual.form.id == 0L

        if (!esNueva) {
            val existente = oportunidades.value.firstOrNull { it.id == actual.form.id }
            if (existente == null) {
                _editor.update {
                    it.copy(mensajeError = "No se encontró la oportunidad solicitada.")
                }
                return
            }
            if (!puedeGestionar(existente)) {
                _editor.update {
                    it.copy(mensajeError = "No tienes permiso para editar esta oportunidad.")
                }
                return
            }
        }

        val errores = OportunidadValidator.validar(actual.form)

        if (errores.hayErrores) {
            _editor.update { it.copy(errores = errores) }
            return
        }

        if (actual.guardando) return
        _editor.update { it.copy(guardando = true, mensajeError = null) }

        val entidad = OpportunityEntity(
            id = actual.form.id,
            nombre = actual.form.nombre.trim(),
            tipo = actual.form.tipo,
            descripcion = actual.form.descripcion.trim(),
            fuente = actual.form.fuente,
            prioridad = actual.form.prioridad,
            potencialComercial = actual.form.potencialComercial,
            estado = actual.form.estado,
            observaciones = actual.form.observaciones.trim(),
            codigoQr = actual.form.codigoQr,
            eventoId = actual.form.eventoId,
            organizacionId = actual.form.organizacionId,
            contactoId = actual.form.contactoId,
            responsableId = if (esNueva) usuario.id else actual.form.responsableId ?: usuario.id,
            publicoEsperado = actual.form.publicoEsperado.trim(),
            fechaCreacion = actual.form.fechaCreacion ?: System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                val idGuardado = repository.guardar(entidad)
                _editor.value = OportunidadEditorUiState()
                onListo(idGuardado)
            } catch (e: Exception) {
                _editor.update {
                    it.copy(
                        guardando = false,
                        mensajeError = "No se pudo guardar la oportunidad: ${e.message}"
                    )
                }
            }
        }
    }

    fun eliminarOportunidad(id: Long, onListo: () -> Unit, onError: (String) -> Unit) {
        val existente = oportunidades.value.firstOrNull { it.id == id }
        if (existente == null) {
            onError("No se encontró la oportunidad para eliminarla.")
            return
        }
        if (!puedeGestionar(existente)) {
            onError("No tienes permiso para eliminar esta oportunidad.")
            return
        }

        viewModelScope.launch {
            try {
                if (repository.eliminar(id) == 0) {
                    onError("No se encontró la oportunidad para eliminarla.")
                    return@launch
                }
                onListo()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError("No se pudo eliminar la oportunidad: ${e.message}")
            }
        }
    }

    private fun puedeGestionar(oportunidad: OpportunityEntity): Boolean {
        val usuario = sessionManager.actual ?: return false
        return when (usuario.perfil) {
            PerfilUsuario.SUPERVISOR_COMERCIAL -> true
            PerfilUsuario.EJECUTIVO_COMERCIAL -> oportunidad.responsableId == usuario.id
        }
    }

    companion object {
        // Factory clásica para inyectarle el Application (Room lo necesita)
        fun factory(
            application: Application,
            sessionManager: SessionManager
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(OportunidadViewModel::class.java))
                    return OportunidadViewModel(application, sessionManager) as T
                }
            }
    }
}
