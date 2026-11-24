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
import androidx.compose.ui.unit.dp
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.TipoAccionSeguimiento
import cl.duocuc.evaluacion2.ui.components.CampoDropdownFormulario
import cl.duocuc.evaluacion2.ui.components.CampoTextoFormulario
import cl.duocuc.evaluacion2.ui.components.OpcionDropdown
import cl.duocuc.evaluacion2.ui.viewmodels.GestionViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarGestionScreen(
    oportunidadId: Long,
    viewModel: GestionViewModel,
    onVolver: () -> Unit,
    onGuardado: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editor by viewModel.editor.collectAsState()
    val esEjecutivo by viewModel.esEjecutivo.collectAsState()
    val puedeRegistrarSeguimiento by viewModel.puedeRegistrarSeguimiento.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(oportunidadId) {
        viewModel.seleccionarOportunidad(oportunidadId)
        viewModel.beginNueva()
    }

    var mostrarSelectorFechaGestion by remember { mutableStateOf(false) }
    var mostrarSelectorFechaProxima by remember { mutableStateOf(false) }

    val opcionesTipo = TipoAccionSeguimiento.entries.map {
        OpcionDropdown(it.name, it.etiqueta)
    }
    val opcionesEstado = listOf(OpcionDropdown("", "Sin cambiar")) +
        EstadoGestion.entries
            .filterNot {
                it == EstadoGestion.ACTIVACION_REALIZADA ||
                    it == EstadoGestion.ACTIVACION_CANCELADA
            }
            .map { OpcionDropdown(it.name, it.etiqueta) }
    val formatoFecha = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    val formatoHora = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    if (mostrarSelectorFechaGestion) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editor.form.fechaGestion
        )

        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFechaGestion = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(viewModel::onFechaGestionChange)
                        mostrarSelectorFechaGestion = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorFechaGestion = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (mostrarSelectorFechaProxima) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editor.form.fechaProximaAccion
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFechaProxima = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.let(viewModel::onFechaProximaAccionChange)
                        mostrarSelectorFechaProxima = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorFechaProxima = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    fun abrirSelectorHora(valorActual: String, onChange: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        valorActual.split(':').takeIf { it.size == 2 }?.let {
            calendar.set(Calendar.HOUR_OF_DAY, it[0].toIntOrNull() ?: 9)
            calendar.set(Calendar.MINUTE, it[1].toIntOrNull() ?: 0)
        }
        TimePickerDialog(
            context,
            { _, hora, minuto -> onChange("%02d:%02d".format(hora, minuto)) },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Registrar seguimiento", fontWeight = FontWeight.SemiBold) },
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
            if (!puedeRegistrarSeguimiento) {
                Text(
                    text = "No tienes permiso para registrar seguimientos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = if (esEjecutivo) {
                    "Registra la acción comercial. El estado de la oportunidad solo cambiará " +
                        "si seleccionas un estado resultante."
                } else {
                    "Registra un seguimiento general de supervisión para esta oportunidad."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CampoDropdownFormulario(
                label = "Tipo de acción *",
                selected = editor.form.tipo,
                options = opcionesTipo,
                placeholder = "Selecciona el tipo",
                error = editor.errores.tipo,
                onSelected = viewModel::onTipoChange
            )

            if (esEjecutivo) {
                CampoDropdownFormulario(
                    label = "Estado resultante (opcional)",
                    selected = editor.form.estado,
                    options = opcionesEstado,
                    placeholder = "Sin cambiar",
                    error = editor.errores.estado,
                    onSelected = viewModel::onEstadoChange
                )
            }

            OutlinedTextField(
                value = editor.form.fechaGestion?.let {
                    "${formatoFecha.format(Date(it))} " +
                        editor.form.horaGestion.ifBlank { formatoHora.format(Date(it)) }
                }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha y hora *") },
                trailingIcon = {
                    IconButton(onClick = { mostrarSelectorFechaGestion = true }) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = "Seleccionar fecha"
                        )
                    }
                },
                isError = editor.errores.fechaGestion != null,
                supportingText = editor.errores.fechaGestion?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                OutlinedButton(
                    onClick = {
                        abrirSelectorHora(
                            editor.form.horaGestion.ifBlank {
                                editor.form.fechaGestion
                                    ?.let { formatoHora.format(Date(it)) }.orEmpty()
                            },
                            viewModel::onHoraGestionChange
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Hora de la gestión: " +
                            editor.form.horaGestion.ifBlank {
                                editor.form.fechaGestion
                                    ?.let { formatoHora.format(Date(it)) }.orEmpty()
                            }
                    )
                }
                editor.errores.horaGestion?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }

            CampoTextoFormulario(
                label = "Resultado *",
                value = editor.form.resultado,
                error = editor.errores.descripcion,
                onValueChange = viewModel::onResultadoChange,
                singleLine = false
            )

            CampoTextoFormulario(
                label = "Próxima acción",
                value = editor.form.proximaAccion,
                error = editor.errores.proximaAccion,
                onValueChange = viewModel::onProximaAccionChange
            )

            OutlinedTextField(
                value = editor.form.fechaProximaAccion?.let {
                    "${formatoFecha.format(Date(it))} " +
                        editor.form.horaProximaAccion.ifBlank { formatoHora.format(Date(it)) }
                }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha del próximo seguimiento") },
                placeholder = { Text("Opcional") },
                trailingIcon = {
                    IconButton(onClick = { mostrarSelectorFechaProxima = true }) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Seleccionar fecha"
                        )
                    }
                },
                isError = editor.errores.fechaProximaAccion != null,
                supportingText = editor.errores.fechaProximaAccion?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                OutlinedButton(
                    onClick = {
                        abrirSelectorHora(
                            editor.form.horaProximaAccion.ifBlank {
                                editor.form.fechaProximaAccion
                                    ?.let { formatoHora.format(Date(it)) }.orEmpty()
                            },
                            viewModel::onHoraProximaAccionChange
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Hora del próximo seguimiento: " +
                            editor.form.horaProximaAccion.ifBlank {
                                editor.form.fechaProximaAccion
                                    ?.let { formatoHora.format(Date(it)) } ?: "Seleccionar"
                            }
                    )
                }
                editor.errores.horaProximaAccion?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }

            if (editor.form.fechaProximaAccion != null) {
                TextButton(
                    onClick = { viewModel.onFechaProximaAccionChange(null) },
                    enabled = !editor.guardando
                ) {
                    Text("Quitar fecha")
                }
            }

            CampoTextoFormulario(
                label = "Observaciones",
                value = editor.form.observaciones,
                error = editor.errores.observaciones,
                onValueChange = viewModel::onObservacionesChange,
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
                    onClick = { viewModel.guardar { onGuardado() } },
                    modifier = Modifier.weight(1f),
                    enabled = !editor.guardando && puedeRegistrarSeguimiento
                ) {
                    if (editor.guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("Guardar seguimiento")
                    }
                }
            }
        }
    }
}
