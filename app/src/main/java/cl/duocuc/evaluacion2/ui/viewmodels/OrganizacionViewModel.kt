package cl.duocuc.evaluacion2.ui.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.OrganizacionEntity
import cl.duocuc.evaluacion2.data.repository.OrganizacionRepository
import cl.duocuc.evaluacion2.validation.OrganizacionValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrganizacionEditorUiState(
    val form: OrganizacionFormState = OrganizacionFormState(),
    val errores: OrganizacionErrores = OrganizacionErrores(),
    val guardando: Boolean = false,
    val mensajeError: String? = null
)

/** Muestra organizaciones y guarda los cambios del formulario. */
class OrganizacionViewModel(
    private val repository: OrganizacionRepository
) : ViewModel() {

    val organizaciones: StateFlow<List<OrganizacionEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _editor = MutableStateFlow(OrganizacionEditorUiState())
    val editor: StateFlow<OrganizacionEditorUiState> = _editor.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // Espera la primera lectura antes de ocultar el indicador de carga.
                repository.observeAll().first()
            } finally {
                _cargando.value = false
            }
        }
    }

    fun beginNueva() {
        _editor.value = OrganizacionEditorUiState()
    }

    fun beginEditar(id: Long) {
        val existente = organizaciones.value.firstOrNull { it.id == id }
        if (existente == null) {
            _editor.update {
                it.copy(mensajeError = "No se encontró la organización solicitada.")
            }
            return
        }
        _editor.value = OrganizacionEditorUiState(
            form = OrganizacionFormState(
                id = existente.id,
                nombre = existente.nombre,
                tipo = existente.tipo,
                comuna = existente.comuna,
                sitioWeb = existente.sitioWeb,
                observaciones = existente.observaciones
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

    fun onTipoChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(tipo = valor),
                errores = it.errores.copy(tipo = null)
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

    fun onSitioWebChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(sitioWeb = valor),
                errores = it.errores.copy(sitioWeb = null)
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

    fun guardar(onListo: (Long) -> Unit) {
        val actual = _editor.value
        val errores = OrganizacionValidator.validar(actual.form)

        if (errores.hayErrores) {
            _editor.update { it.copy(errores = errores) }
            return
        }

        if (actual.guardando) return
        _editor.update { it.copy(guardando = true, mensajeError = null) }

        val entidad = OrganizacionEntity(
            id = actual.form.id,
            nombre = actual.form.nombre.trim(),
            tipo = actual.form.tipo,
            comuna = actual.form.comuna.trim(),
            sitioWeb = actual.form.sitioWeb.trim(),
            observaciones = actual.form.observaciones.trim()
        )

        viewModelScope.launch {
            try {
                val idGuardado = repository.guardar(entidad)
                _editor.value = OrganizacionEditorUiState()
                onListo(idGuardado)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // La base no permite repetir el nombre de una organización.
                val mensaje = if (e.message?.contains("UNIQUE", ignoreCase = true) == true) {
                    "Ya existe una organización con ese nombre."
                } else {
                    "No se pudo guardar la organización: ${e.message}"
                }
                _editor.update {
                    it.copy(guardando = false, mensajeError = mensaje)
                }
            }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(OrganizacionViewModel::class.java))
                    val repository = OrganizacionRepository(
                        AppDatabase.getInstance(application).organizacionDao()
                    )
                    return OrganizacionViewModel(repository) as T
                }
            }
    }
}
