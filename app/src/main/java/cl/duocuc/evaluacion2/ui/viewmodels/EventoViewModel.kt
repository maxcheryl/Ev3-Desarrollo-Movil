package cl.duocuc.evaluacion2.ui.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.EventoEntity
import cl.duocuc.evaluacion2.data.local.OrganizacionEntity
import cl.duocuc.evaluacion2.data.repository.EventoRepository
import cl.duocuc.evaluacion2.data.repository.OportunidadRepository
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
    private val organizacionRepository: OrganizacionRepository,
    private val oportunidadRepository: OportunidadRepository? = null
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
                tipo = existente.tipo,
                descripcion = existente.descripcion,
                fecha = existente.fecha,
                fechaTermino = existente.fechaTermino,
                horaInicio = existente.horaInicio,
                horaTermino = existente.horaTermino,
                direccion = existente.direccion,
                region = existente.region,
                comuna = existente.comuna,
                organizacionId = existente.organizacionId,
                publicoEsperado = existente.publicoEsperado,
                publicoObjetivo = existente.publicoObjetivo
                    .split('|')
                    .filter(String::isNotBlank),
                asistentesEstimados = existente.asistentesEstimados?.toString().orEmpty(),
                condicionesParticipacion = existente.condicionesParticipacion,
                requisitos = existente.requisitos,
                costoReferencial = existente.costoReferencial?.toString().orEmpty(),
                espaciosDisponibles = existente.espaciosDisponibles
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

    fun onTipoChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(tipo = valor),
                errores = it.errores.copy(tipo = null)
            )
        }
    }

    fun onDescripcionChange(valor: String) = cambiar { it.copy(descripcion = valor) }
    fun onFechaTerminoChange(valor: Long) {
        _editor.update {
            it.copy(
                form = it.form.copy(fechaTermino = valor),
                errores = it.errores.copy(fechaTermino = null)
            )
        }
    }
    fun onHoraInicioChange(valor: String) = cambiar { it.copy(horaInicio = valor) }
    fun onHoraTerminoChange(valor: String) = cambiar { it.copy(horaTermino = valor) }
    fun onDireccionChange(valor: String) = cambiar { it.copy(direccion = valor) }
    fun onRegionChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(region = valor),
                errores = it.errores.copy(region = null)
            )
        }
    }
    fun onPublicoEsperadoChange(valor: String) = cambiar { it.copy(publicoEsperado = valor) }
    fun onAsistentesEstimadosChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(asistentesEstimados = valor),
                errores = it.errores.copy(asistentesEstimados = null)
            )
        }
    }
    fun onCondicionesChange(valor: String) = cambiar { it.copy(condicionesParticipacion = valor) }
    fun onRequisitosChange(valor: String) = cambiar { it.copy(requisitos = valor) }
    fun onCostoReferencialChange(valor: String) {
        _editor.update {
            it.copy(
                form = it.form.copy(costoReferencial = valor),
                errores = it.errores.copy(costoReferencial = null)
            )
        }
    }
    fun onEspaciosDisponiblesChange(valor: String) = cambiar { it.copy(espaciosDisponibles = valor) }

    fun onPublicoObjetivoChange(valor: String, seleccionado: Boolean) {
        _editor.update { state ->
            val seleccionados = state.form.publicoObjetivo.toMutableSet()
            if (seleccionado) seleccionados.add(valor) else seleccionados.remove(valor)
            state.copy(form = state.form.copy(publicoObjetivo = seleccionados.toList()))
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

    private fun cambiar(transform: (EventoFormState) -> EventoFormState) {
        _editor.update { state -> state.copy(form = transform(state.form)) }
    }

    fun guardar(oportunidadId: Long? = null, onListo: (Long) -> Unit) {
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
            tipo = actual.form.tipo,
            descripcion = actual.form.descripcion.trim(),
            fechaTermino = actual.form.fechaTermino,
            horaInicio = actual.form.horaInicio,
            horaTermino = actual.form.horaTermino,
            direccion = actual.form.direccion.trim(),
            region = actual.form.region.trim(),
            comuna = actual.form.comuna.trim(),
            organizacionId = actual.form.organizacionId ?: 0L,
            publicoEsperado = actual.form.publicoEsperado.trim(),
            publicoObjetivo = actual.form.publicoObjetivo.joinToString("|"),
            asistentesEstimados = actual.form.asistentesEstimados
                .trim()
                .takeIf(String::isNotEmpty)
                ?.toIntOrNull(),
            condicionesParticipacion = actual.form.condicionesParticipacion.trim(),
            requisitos = actual.form.requisitos.trim(),
            costoReferencial = actual.form.costoReferencial
                .trim()
                .takeIf(String::isNotEmpty)
                ?.replace(',', '.')
                ?.toDoubleOrNull(),
            espaciosDisponibles = actual.form.espaciosDisponibles.trim()
        )

        viewModelScope.launch {
            try {
                val idGuardado = repository.guardar(entidad)
                _editor.update { it.copy(form = it.form.copy(id = idGuardado)) }
                if (oportunidadId != null) {
                    checkNotNull(oportunidadRepository) {
                        "No está disponible el vínculo con la oportunidad."
                    }.asociarEvento(oportunidadId, idGuardado)
                }
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
                        organizacionRepository = OrganizacionRepository(database.organizacionDao()),
                        oportunidadRepository = OportunidadRepository(database)
                    ) as T
                }
            }
    }
}
