package cl.duocuc.evaluacion2.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.TipoGestion
import cl.duocuc.evaluacion2.ui.components.CampoDropdownFormulario
import cl.duocuc.evaluacion2.ui.components.CampoTextoFormulario
import cl.duocuc.evaluacion2.ui.components.OpcionDropdown
import cl.duocuc.evaluacion2.ui.viewmodels.GestionViewModel
import java.text.SimpleDateFormat
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

    LaunchedEffect(oportunidadId) {
        viewModel.seleccionarOportunidad(oportunidadId)
        viewModel.beginNueva()
    }

    var mostrarSelectorFecha by remember { mutableStateOf(false) }

    val opcionesTipo = TipoGestion.entries.map { OpcionDropdown(it.name, it.etiqueta) }
    val opcionesEstado = EstadoGestion.entries.map { OpcionDropdown(it.name, it.etiqueta) }
    val formatoFecha = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    if (mostrarSelectorFecha) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editor.form.fechaProximaAccion
        )

        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFecha = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(viewModel::onFechaProximaAccionChange)
                        mostrarSelectorFecha = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorFecha = false }) {
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
                title = { Text("Registrar gestión", fontWeight = FontWeight.SemiBold) },
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
            if (!esEjecutivo) {
                Text(
                    text = "Solo el Ejecutivo comercial puede registrar gestiones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Registra la interacción comercial y actualiza el estado de la oportunidad.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CampoDropdownFormulario(
                label = "Tipo de gestión *",
                selected = editor.form.tipo,
                options = opcionesTipo,
                placeholder = "Selecciona el tipo",
                error = editor.errores.tipo,
                onSelected = viewModel::onTipoChange
            )

            CampoDropdownFormulario(
                label = "Estado resultante *",
                selected = editor.form.estado,
                options = opcionesEstado,
                placeholder = "Selecciona el nuevo estado",
                error = editor.errores.estado,
                onSelected = viewModel::onEstadoChange
            )

            CampoTextoFormulario(
                label = "Descripción *",
                value = editor.form.descripcion,
                error = editor.errores.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                singleLine = false
            )

            CampoTextoFormulario(
                label = "Resultado",
                value = editor.form.resultado,
                error = null,
                onValueChange = viewModel::onResultadoChange,
                singleLine = false
            )

            CampoTextoFormulario(
                label = "Próxima acción",
                value = editor.form.proximaAccion,
                error = null,
                onValueChange = viewModel::onProximaAccionChange
            )

            OutlinedTextField(
                value = editor.form.fechaProximaAccion?.let { formatoFecha.format(Date(it)) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha de próxima acción") },
                placeholder = { Text("Opcional") },
                trailingIcon = {
                    IconButton(onClick = { mostrarSelectorFecha = true }) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Seleccionar fecha"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (editor.form.fechaProximaAccion != null) {
                TextButton(
                    onClick = { viewModel.onFechaProximaAccionChange(null) },
                    enabled = !editor.guardando
                ) {
                    Text("Quitar fecha")
                }
            }

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
                    enabled = !editor.guardando && esEjecutivo
                ) {
                    if (editor.guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}
