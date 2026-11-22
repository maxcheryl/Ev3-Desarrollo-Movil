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
import cl.duocuc.evaluacion2.ui.components.CampoDropdownFormulario
import cl.duocuc.evaluacion2.ui.components.CampoTextoFormulario
import cl.duocuc.evaluacion2.ui.components.OpcionDropdown
import cl.duocuc.evaluacion2.ui.viewmodels.EventoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventoFormScreen(
    viewModel: EventoViewModel,
    eventoId: Long?,
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

    var mostrarSelectorFecha by remember { mutableStateOf(false) }

    val esEdicion = editor.form.id != 0L
    val opcionesOrganizacion = organizaciones.map {
        OpcionDropdown(valor = it.id.toString(), etiqueta = it.nombre)
    }
    val errorOrganizacion = editor.errores.organizacionId
        ?: if (organizaciones.isEmpty()) "Crea primero una organización." else null
    val formatoFecha = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    if (mostrarSelectorFecha) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editor.form.fecha
        )

        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFecha = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(viewModel::onFechaChange)
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

            CampoTextoFormulario(
                label = "Nombre *",
                value = editor.form.nombre,
                error = editor.errores.nombre,
                onValueChange = viewModel::onNombreChange
            )

            OutlinedTextField(
                value = editor.form.fecha?.let { formatoFecha.format(Date(it)) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha *") },
                placeholder = { Text("Selecciona una fecha") },
                trailingIcon = {
                    IconButton(onClick = { mostrarSelectorFecha = true }) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Seleccionar fecha"
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

            CampoTextoFormulario(
                label = "Comuna *",
                value = editor.form.comuna,
                error = editor.errores.comuna,
                onValueChange = viewModel::onComunaChange
            )

            CampoDropdownFormulario(
                label = "Organización *",
                selected = editor.form.organizacionId?.toString().orEmpty(),
                options = opcionesOrganizacion,
                placeholder = "Selecciona una organización",
                error = errorOrganizacion,
                onSelected = { valor -> viewModel.onOrganizacionChange(valor.toLongOrNull()) }
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
                    enabled = !editor.guardando && organizaciones.isNotEmpty()
                ) {
                    if (editor.guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text(if (esEdicion) "Guardar cambios" else "Crear evento")
                    }
                }
            }
        }
    }
}
