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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import cl.duocuc.evaluacion2.data.model.PotencialComercial
import cl.duocuc.evaluacion2.data.model.Prioridad
import cl.duocuc.evaluacion2.data.model.TipoOportunidad
import cl.duocuc.evaluacion2.ui.viewmodels.FuentesOportunidad
import cl.duocuc.evaluacion2.ui.viewmodels.ContactoViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.EventoViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.OrganizacionViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.OportunidadViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formulario para crear o editar una oportunidad.
 * Los errores aparecen junto a los campos que hay que corregir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioOportunidadScreen(
    id: Long,
    viewModel: OportunidadViewModel,
    organizacionViewModel: OrganizacionViewModel,
    contactoViewModel: ContactoViewModel,
    eventoViewModel: EventoViewModel,
    onVolver: () -> Unit,
    onGuardado: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val editor by viewModel.editor.collectAsState()
    val organizaciones by organizacionViewModel.organizaciones.collectAsState()
    val contactos by contactoViewModel.contactos.collectAsState()
    val eventos by eventoViewModel.eventos.collectAsState()

    LaunchedEffect(id) {
        if (id == 0L) viewModel.beginNueva() else viewModel.beginEditar(id)
    }

    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    val formatoFecha = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    if (mostrarSelectorFecha) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editor.form.fechaCreacion
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFecha = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(
                            viewModel::onFechaIdentificacionChange
                        )
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
                        text = if (id == 0L) "Nueva oportunidad" else "Editar oportunidad",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Datos de la oportunidad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            if (id == 0L) {
                Text(
                    text = "La oportunidad quedará asignada a tu usuario.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FormTextField(
                label = "Nombre de la oportunidad *",
                value = editor.form.nombre,
                error = editor.errores.nombre,
                onValueChange = viewModel::onNombreChange
            )

            FormDropdown(
                label = "Tipo de Evento / Actividad *",
                selected = editor.form.tipo,
                options = TipoOportunidad.entries.map { OpcionDropdown(it.name, it.etiqueta) },
                placeholder = "Seleccione un tipo",
                error = editor.errores.tipo,
                onSelected = viewModel::onTipoChange
            )

            FormTextField(
                label = "Público esperado (opcional)",
                value = editor.form.publicoEsperado,
                error = editor.errores.publicoEsperado,
                onValueChange = viewModel::onPublicoEsperadoChange
            )

            OutlinedTextField(
                value = editor.form.fechaCreacion?.let {
                    formatoFecha.format(Date(it))
                }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha de identificación *") },
                trailingIcon = {
                    IconButton(onClick = { mostrarSelectorFecha = true }) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Seleccionar fecha de identificación"
                        )
                    }
                },
                isError = editor.errores.fechaIdentificacion != null,
                supportingText = editor.errores.fechaIdentificacion?.let {
                    {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            FormTextField(
                label = "Comuna *",
                value = editor.form.comuna,
                error = editor.errores.comuna,
                onValueChange = viewModel::onComunaChange
            )

            FormTextField(
                label = "Descripción *",
                value = editor.form.descripcion,
                error = editor.errores.descripcion,
                singleLine = false,
                onValueChange = viewModel::onDescripcionChange
            )

            FormDropdown(
                label = "Fuente u origen *",
                selected = editor.form.fuente,
                options = FuentesOportunidad.opciones.map { OpcionDropdown(it, it) },
                placeholder = "Seleccione una fuente",
                error = editor.errores.fuente,
                onSelected = viewModel::onFuenteChange
            )

            Text(
                text = "Gestión",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            FormDropdown(
                label = "Prioridad *",
                selected = editor.form.prioridad,
                options = Prioridad.entries.map { OpcionDropdown(it.name, it.etiqueta) },
                placeholder = "Seleccione una prioridad",
                error = editor.errores.prioridad,
                onSelected = viewModel::onPrioridadChange
            )

            FormDropdown(
                label = "Potencial comercial *",
                selected = editor.form.potencialComercial,
                options = PotencialComercial.entries.map { OpcionDropdown(it.name, it.etiqueta) },
                placeholder = "Seleccione el potencial",
                error = editor.errores.potencialComercial,
                onSelected = viewModel::onPotencialComercialChange
            )

            FormDropdown(
                label = "Estado de gestión *",
                selected = editor.form.estado,
                options = EstadoGestion.entries.map { OpcionDropdown(it.name, it.etiqueta) },
                placeholder = "Seleccione un estado",
                error = editor.errores.estado,
                onSelected = viewModel::onEstadoChange
            )

            Text(
                text = "Registros relacionados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            FormDropdown(
                label = "Organización (opcional)",
                selected = editor.form.organizacionId?.toString().orEmpty(),
                options = listOf(OpcionDropdown("", "Sin organización")) +
                    organizaciones.map { OpcionDropdown(it.id.toString(), it.nombre) },
                placeholder = "Selecciona una organización",
                error = editor.errores.organizacionId,
                onSelected = { viewModel.onOrganizacionChange(it.toLongOrNull()) }
            )

            FormDropdown(
                label = "Contacto (opcional)",
                selected = editor.form.contactoId?.toString().orEmpty(),
                options = listOf(OpcionDropdown("", "Sin contacto")) +
                    contactos.map { OpcionDropdown(it.id.toString(), it.nombre) },
                placeholder = "Selecciona un contacto",
                error = editor.errores.contactoId,
                onSelected = { viewModel.onContactoChange(it.toLongOrNull()) }
            )

            FormDropdown(
                label = "Evento o actividad (opcional)",
                selected = editor.form.eventoId?.toString().orEmpty(),
                options = listOf(OpcionDropdown("", "Sin evento")) +
                    eventos.map { OpcionDropdown(it.id.toString(), it.nombre) },
                placeholder = "Selecciona un evento",
                error = editor.errores.eventoId,
                onSelected = { viewModel.onEventoChange(it.toLongOrNull()) }
            )

            Text(
                text = "Observaciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            FormTextField(
                label = "Observaciones adicionales (opcional)",
                value = editor.form.observaciones,
                error = editor.errores.observaciones,
                singleLine = false,
                onValueChange = viewModel::onObservacionesChange
            )

            // Muestra errores generales, como un fallo al guardar.
            AnimatedVisibility(
                visible = editor.mensajeError != null,
                enter = fadeIn(animationSpec = tween(250)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(200)) + shrinkVertically()
            ) {
                Text(
                    text = editor.mensajeError.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onVolver,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = { viewModel.guardarOportunidad(onGuardado) },
                    enabled = !editor.guardando,
                    modifier = Modifier.weight(1f)
                ) {
                    if (editor.guardando) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Guardar")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private data class OpcionDropdown(val valor: String, val etiqueta: String)

@Composable
private fun FormTextField(
    label: String,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = error?.let {
            {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 4
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormDropdown(
    label: String,
    selected: String,
    options: List<OpcionDropdown>,
    placeholder: String,
    error: String?,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.firstOrNull { it.valor == selected }?.etiqueta ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            isError = error != null,
            supportingText = error?.let {
                {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion.etiqueta) },
                    onClick = {
                        onSelected(opcion.valor)
                        expanded = false
                    }
                )
            }
        }
    }
}
