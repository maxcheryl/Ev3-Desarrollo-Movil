package cl.duocuc.evaluacion2.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duocuc.evaluacion2.data.model.TipoOportunidad
import cl.duocuc.evaluacion2.data.model.TipoOrganizacion
import cl.duocuc.evaluacion2.ui.components.CampoDropdownFormulario
import cl.duocuc.evaluacion2.ui.components.CampoTextoFormulario
import cl.duocuc.evaluacion2.ui.components.OpcionDropdown
import cl.duocuc.evaluacion2.ui.viewmodels.EventoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventoFormScreen(
    viewModel: EventoViewModel,
    eventoId: Long?,
    oportunidadId: Long? = null,
    onVolver: () -> Unit,
    onGuardado: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editor by viewModel.editor.collectAsState()
    val organizaciones by viewModel.organizaciones.collectAsState()

    LaunchedEffect(eventoId) {
        if (eventoId == null || eventoId == 0L) {
            viewModel.beginNueva()
        } else {
            viewModel.beginEditar(eventoId)
        }
    }

    var mostrarSelectorFechaInicio by remember { mutableStateOf(false) }
    var mostrarSelectorFechaTermino by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val esEdicion = editor.form.id != 0L
    val opcionesOrganizacion = organizaciones.map {
        OpcionDropdown(valor = it.id.toString(), etiqueta = it.nombre)
    }
    val errorOrganizacion = editor.errores.organizacionId
        ?: if (organizaciones.isEmpty()) "Crea primero una organización." else null
    val organizacionSeleccionada = organizaciones.firstOrNull {
        it.id == editor.form.organizacionId
    }
    val formatoFecha = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    if (mostrarSelectorFechaInicio) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editor.form.fecha
        )

        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFechaInicio = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(viewModel::onFechaChange)
                        mostrarSelectorFechaInicio = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorFechaInicio = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (mostrarSelectorFechaTermino) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editor.form.fechaTermino ?: editor.form.fecha
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFechaTermino = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(viewModel::onFechaTerminoChange)
                        mostrarSelectorFechaTermino = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorFechaTermino = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (esEdicion) "Editar evento" else "Nuevo evento",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (organizaciones.isEmpty()) {
                Text(
                    text = "No hay organizaciones registradas. Crea una en Catálogos → Organizaciones antes de agregar eventos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text("Información general", style = MaterialTheme.typography.titleMedium)
            CampoTextoFormulario(
                label = "Nombre del evento *",
                value = editor.form.nombre,
                error = editor.errores.nombre,
                onValueChange = viewModel::onNombreChange
            )

            CampoDropdownFormulario(
                label = "Tipo de evento *",
                selected = editor.form.tipo,
                options = TipoOportunidad.entries.map {
                    OpcionDropdown(it.name, it.etiqueta)
                },
                placeholder = "Selecciona un tipo",
                error = editor.errores.tipo,
                onSelected = viewModel::onTipoChange
            )

            CampoTextoFormulario(
                label = "Descripción",
                value = editor.form.descripcion,
                error = editor.errores.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                singleLine = false
            )

            Text("Fecha y ubicación", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = editor.form.fecha?.let { formatoFecha.format(Date(it)) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha de inicio *") },
                placeholder = { Text("Selecciona una fecha") },
                trailingIcon = {
                    IconButton(onClick = { mostrarSelectorFechaInicio = true }) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Seleccionar fecha de inicio"
                        )
                    }
                },
                isError = editor.errores.fecha != null,
                supportingText = editor.errores.fecha?.let {
                    {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = editor.form.fechaTermino?.let { formatoFecha.format(Date(it)) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha de término *") },
                placeholder = { Text("Selecciona una fecha") },
                trailingIcon = {
                    IconButton(onClick = { mostrarSelectorFechaTermino = true }) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Seleccionar fecha de término"
                        )
                    }
                },
                isError = editor.errores.fechaTermino != null,
                supportingText = editor.errores.fechaTermino?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        editor.form.horaInicio.split(':').takeIf { it.size == 2 }?.let {
                            calendar.set(Calendar.HOUR_OF_DAY, it[0].toIntOrNull() ?: 9)
                            calendar.set(Calendar.MINUTE, it[1].toIntOrNull() ?: 0)
                        }
                        TimePickerDialog(
                            context,
                            { _, hora, minuto ->
                                viewModel.onHoraInicioChange("%02d:%02d".format(hora, minuto))
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Inicio: ${editor.form.horaInicio.ifBlank { "Hora" }}")
                }
                OutlinedButton(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        editor.form.horaTermino.split(':').takeIf { it.size == 2 }?.let {
                            calendar.set(Calendar.HOUR_OF_DAY, it[0].toIntOrNull() ?: 18)
                            calendar.set(Calendar.MINUTE, it[1].toIntOrNull() ?: 0)
                        }
                        TimePickerDialog(
                            context,
                            { _, hora, minuto ->
                                viewModel.onHoraTerminoChange("%02d:%02d".format(hora, minuto))
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Término: ${editor.form.horaTermino.ifBlank { "Hora" }}")
                }
            }
            editor.errores.horaInicio?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            editor.errores.horaTermino?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            CampoTextoFormulario(
                label = "Dirección o ubicación",
                value = editor.form.direccion,
                error = editor.errores.direccion,
                onValueChange = viewModel::onDireccionChange
            )

            CampoTextoFormulario(
                label = "Región *",
                value = editor.form.region,
                error = editor.errores.region,
                onValueChange = viewModel::onRegionChange
            )

            CampoTextoFormulario(
                label = "Comuna *",
                value = editor.form.comuna,
                error = editor.errores.comuna,
                onValueChange = viewModel::onComunaChange
            )

            Text("Organización responsable", style = MaterialTheme.typography.titleMedium)
            CampoDropdownFormulario(
                label = "Organización *",
                selected = editor.form.organizacionId?.toString().orEmpty(),
                options = opcionesOrganizacion,
                placeholder = "Selecciona una organización",
                error = errorOrganizacion,
                onSelected = { valor -> viewModel.onOrganizacionChange(valor.toLongOrNull()) }
            )
            organizacionSeleccionada?.let {
                Text(
                    text = "Tipo de organización: " +
                        TipoOrganizacion.desdeNombre(it.tipo).etiqueta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text("Público y potencial", style = MaterialTheme.typography.titleMedium)
            CampoTextoFormulario(
                label = "Público esperado",
                value = editor.form.publicoEsperado,
                error = editor.errores.publicoEsperado,
                onValueChange = viewModel::onPublicoEsperadoChange
            )
            Text("Público objetivo", style = MaterialTheme.typography.labelLarge)
            listOf("Trabajadores", "Colaboradores", "Familias", "Asistentes", "Otro").forEach {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(
                        checked = it in editor.form.publicoObjetivo,
                        onCheckedChange = { seleccionado ->
                            viewModel.onPublicoObjetivoChange(it, seleccionado)
                        }
                    )
                    Text(it)
                }
            }
            editor.errores.publicoObjetivo?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            CampoTextoFormulario(
                label = "Cantidad estimada de asistentes",
                value = editor.form.asistentesEstimados,
                error = editor.errores.asistentesEstimados,
                onValueChange = viewModel::onAsistentesEstimadosChange,
                keyboardType = KeyboardType.Number
            )

            Text("Condiciones de participación", style = MaterialTheme.typography.titleMedium)
            CampoTextoFormulario(
                label = "Condiciones",
                value = editor.form.condicionesParticipacion,
                error = editor.errores.condicionesParticipacion,
                onValueChange = viewModel::onCondicionesChange,
                singleLine = false
            )
            CampoTextoFormulario(
                label = "Requisitos",
                value = editor.form.requisitos,
                error = editor.errores.requisitos,
                onValueChange = viewModel::onRequisitosChange,
                singleLine = false
            )
            CampoTextoFormulario(
                label = "Costo referencial",
                value = editor.form.costoReferencial,
                error = editor.errores.costoReferencial,
                onValueChange = viewModel::onCostoReferencialChange,
                keyboardType = KeyboardType.Decimal
            )
            CampoTextoFormulario(
                label = "Espacios disponibles",
                value = editor.form.espaciosDisponibles,
                error = editor.errores.espaciosDisponibles,
                onValueChange = viewModel::onEspaciosDisponiblesChange,
                singleLine = false
            )

            AnimatedVisibility(
                visible = editor.mensajeError != null,
                enter = fadeIn(tween(150)) + expandVertically(tween(150)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
            ) {
                Text(
                    text = editor.mensajeError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onVolver,
                    modifier = Modifier.weight(1f),
                    enabled = !editor.guardando
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        viewModel.guardar(oportunidadId) { onGuardado() }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !editor.guardando && organizaciones.isNotEmpty()
                ) {
                    if (editor.guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text(if (esEdicion) "Guardar evento" else "Guardar evento")
                    }
                }
            }
        }
    }
}
