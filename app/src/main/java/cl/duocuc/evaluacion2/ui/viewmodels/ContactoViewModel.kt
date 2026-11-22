package cl.duocuc.evaluacion2.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.ContactoEntity
import cl.duocuc.evaluacion2.data.local.OrganizacionEntity
import cl.duocuc.evaluacion2.data.repository.ContactoRepository
import cl.duocuc.evaluacion2.data.repository.OrganizacionRepository
import cl.duocuc.evaluacion2.validation.ContactoValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactoEditorUiState(
    val form: ContactoFormState = ContactoFormState(),
    val errores: ContactoErrores = ContactoErrores(),
    val guardando: Boolean = false,
    val mensajeError: String? = null
)

class ContactoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = ContactoRepository(database.contactoDao())
    private val organizacionRepository = OrganizacionRepository(database.organizacionDao())

    val contactos: StateFlow<List<ContactoEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val organizaciones: StateFlow<List<OrganizacionEntity>> = organizacionRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _editor = MutableStateFlow(ContactoEditorUiState())
    val editor: StateFlow<ContactoEditorUiState> = _editor.asStateFlow()

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
        _editor.value = ContactoEditorUiState(
            form = ContactoFormState(
                organizacionId = organizaciones.value.firstOrNull()?.id
            )
        )
    }

    fun beginEditar(id: Long) {
        val existente = contactos.value.firstOrNull { it.id == id }
        if (existente == null) {
            _editor.update {
                it.copy(mensajeError = "No se encontró el contacto solicitado.")
            }
            return
        }
        _editor.value = ContactoEditorUiState(
            form = ContactoFormState(
                id = existente.id,
                nombre = existente.nombre,
                organizacionId = existente.organizacionId,
                cargo = existente.cargo,
                area = existente.area,
                correo = existente.correo,
                telefono = existente.telefono,
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

    fun onOrganizacionChange(valor: Long?) {
        _editor.update {
            it.copy(
                form = it.form.copy(organizacionId = valor),
                errores = it.errores.copy(organizacionId = null)
            )
        }
    }

    fun onCargoChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(cargo = valor),
                errores = it.errores.copy(cargo = null)
            )
        }
    }

    fun onAreaChange(valor: String) {
        _editor.update {
            it.copy(form = it.form.copy(area = valor))
        }
    }

    fun onCorreoChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(correo = valor),
                errores = it.errores.copy(correo = null)
            )
        }
    }

    fun onTelefonoChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(telefono = valor),
                errores = it.errores.copy(telefono = null)
            )
        }
    }

    fun onObservacionesChange(valor: String) {
        _editor.update {
            it.copy(form = it.form.copy(observaciones = valor))
        }
    }

    fun guardar(onListo: (Long) -> Unit) {
        val actual = _editor.value
        val errores = ContactoValidator.validar(actual.form)

        if (errores.hayErrores) {
            _editor.update { it.copy(errores = errores) }
            return
        }

        if (actual.guardando) return
        _editor.update { it.copy(guardando = true, mensajeError = null) }

        val entidad = ContactoEntity(
            id = actual.form.id,
            nombre = actual.form.nombre.trim(),
            organizacionId = actual.form.organizacionId ?: 0L,
            cargo = actual.form.cargo.trim(),
            area = actual.form.area.trim(),
            correo = actual.form.correo.trim(),
            telefono = actual.form.telefono.trim(),
            observaciones = actual.form.observaciones.trim()
        )

        viewModelScope.launch {
            try {
                val idGuardado = repository.guardar(entidad)
                _editor.value = ContactoEditorUiState()
                onListo(idGuardado)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _editor.update {
                    it.copy(
                        guardando = false,
                        mensajeError = "No se pudo guardar el contacto: ${e.message}"
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
                    require(modelClass.isAssignableFrom(ContactoViewModel::class.java))
                    return ContactoViewModel(application) as T
                }
            }
    }
}
