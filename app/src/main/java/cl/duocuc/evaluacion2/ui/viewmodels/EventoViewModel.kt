package cl.duocuc.evaluacion2.ui.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.EventoEntity
import cl.duocuc.evaluacion2.data.local.OrganizacionEntity
import cl.duocuc.evaluacion2.data.repository.EventoRepository
import cl.duocuc.evaluacion2.data.repository.OrganizacionRepository
import cl.duocuc.evaluacion2.validation.EventoValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventoEditorUiState(
    val form: EventoFormState = EventoFormState(),
    val errores: EventoErrores = EventoErrores(),
    val guardando: Boolean = false,
    val mensajeError: String? = null
)

class EventoViewModel(
    private val repository: EventoRepository,
    private val organizacionRepository: OrganizacionRepository
) : ViewModel() {

    val eventos: StateFlow<List<EventoEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val organizaciones: StateFlow<List<OrganizacionEntity>> = organizacionRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _editor = MutableStateFlow(EventoEditorUiState())
    val editor: StateFlow<EventoEditorUiState> = _editor.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.observeAll().first()
            } finally {
                _cargando.value = false
            }
        }
    }

    fun beginNueva() {
        _editor.value = EventoEditorUiState(
            form = EventoFormState(
                organizacionId = organizaciones.value.firstOrNull()?.id
            )
        )
    }

    fun beginEditar(id: Long) {
        val existente = eventos.value.firstOrNull { it.id == id }
        if (existente == null) {
            _editor.update {
                it.copy(mensajeError = "No se encontró el evento solicitado.")
            }
            return
        }
        _editor.value = EventoEditorUiState(
            form = EventoFormState(
                id = existente.id,
                nombre = existente.nombre,
                fecha = existente.fecha,
                comuna = existente.comuna,
                organizacionId = existente.organizacionId
            )
        )
    }

    fun onNombreChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(nombre = valor),
                errores = it.errores.copy(nombre = null)
            )
        }
    }

    fun onFechaChange(valor: Long) {
        _editor.update {
            it.copy(
                form = it.form.copy(fecha = valor),
                errores = it.errores.copy(fecha = null)
            )
        }
    }

    fun onComunaChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(comuna = valor),
                errores = it.errores.copy(comuna = null)
            )
        }
    }

    fun onOrganizacionChange(valor: Long?) {
        _editor.update {
            it.copy(
                form = it.form.copy(organizacionId = valor),
                errores = it.errores.copy(organizacionId = null)
            )
        }
    }

    fun guardar(onListo: (Long) -> Unit) {
        val actual = _editor.value
        val errores = EventoValidator.validar(actual.form)

        if (errores.hayErrores) {
            _editor.update { it.copy(errores = errores) }
            return
        }

        if (actual.guardando) return
        _editor.update { it.copy(guardando = true, mensajeError = null) }

        val entidad = EventoEntity(
            id = actual.form.id,
            nombre = actual.form.nombre.trim(),
            fecha = actual.form.fecha ?: 0L,
            comuna = actual.form.comuna.trim(),
            organizacionId = actual.form.organizacionId ?: 0L
        )

        viewModelScope.launch {
            try {
                val idGuardado = repository.guardar(entidad)
                _editor.value = EventoEditorUiState()
                onListo(idGuardado)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _editor.update {
                    it.copy(
                        guardando = false,
                        mensajeError = "No se pudo guardar el evento: ${e.message}"
                    )
                }
            }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(EventoViewModel::class.java))
                    val database = AppDatabase.getInstance(application)
                    return EventoViewModel(
                        repository = EventoRepository(database.eventoDao()),
                        organizacionRepository =
                            OrganizacionRepository(database.organizacionDao())
                    ) as T
                }
            }
    }
}
