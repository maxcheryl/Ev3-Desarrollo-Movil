package cl.duocuc.evaluacion2.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import cl.duocuc.evaluacion2.data.local.EvidenciaEntity
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.PerfilUsuario
import cl.duocuc.evaluacion2.data.model.TipoEvidencia
import cl.duocuc.evaluacion2.ui.components.CampoDropdownFormulario
import cl.duocuc.evaluacion2.ui.components.CampoTextoFormulario
import cl.duocuc.evaluacion2.ui.components.OpcionDropdown
import cl.duocuc.evaluacion2.ui.viewmodels.ActivacionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivacionScreen(
    oportunidadId: Long,
    viewModel: ActivacionViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    var mostrarSelectorFechaResultado by remember { mutableStateOf(false) }
    var errorArchivo by remember { mutableStateOf<String?>(null) }
    val formatoFecha = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun adjuntar(uri: Uri?, tipo: TipoEvidencia) {
        if (uri == null) return
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            errorArchivo = "Android no permitió conservar el acceso a este archivo. Intenta elegirlo nuevamente."
            return
        }

        val nombre = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val columna = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columna >= 0 && cursor.moveToFirst()) cursor.getString(columna) else null
        } ?: uri.lastPathSegment ?: tipo.etiqueta
        errorArchivo = null
        viewModel.agregarEvidencia(tipo.name, nombre, uri.toString())
    }

    val selectorFotos = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> adjuntar(uri, TipoEvidencia.FOTO) }
    val selectorDocumentos = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> adjuntar(uri, TipoEvidencia.DOCUMENTO) }

    LaunchedEffect(oportunidadId) {
        viewModel.cargar(oportunidadId)
    }

    if (mostrarSelectorFecha) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.fecha
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

    if (mostrarSelectorFechaResultado) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.fechaRealizacion
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFechaResultado = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.let(viewModel::onFechaRealizacionChange)
                        mostrarSelectorFechaResultado = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorFechaResultado = false }) {
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
                title = { Text("Gestión de activación", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.cargando) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = state.oportunidadNombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (!state.oportunidadConfirmada) {
                    Text(
                        text = "La oportunidad debe estar confirmada antes de coordinar una activación.",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "Evento asociado",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                state.evento?.let { evento ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(evento.nombre, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${formatoFecha.format(Date(evento.fecha))} · " +
                                    "${formatoFecha.format(Date(evento.fechaTermino ?: evento.fecha))}"
                            )
                            Text(
                                listOf(evento.direccion, evento.comuna, evento.region)
                                    .filter(String::isNotBlank)
                                    .joinToString(", ")
                            )
                            if (evento.condicionesParticipacion.isNotBlank()) {
                                Text("Condiciones: ${evento.condicionesParticipacion}")
                            }
                            if (evento.espaciosDisponibles.isNotBlank()) {
                                Text("Espacios: ${evento.espaciosDisponibles}")
                            }
                            if (evento.costoReferencial != null) {
                                Text("Costo referencial: ${evento.costoReferencial}")
                            }
                            if (evento.requisitos.isNotBlank()) {
                                Text("Requisitos: ${evento.requisitos}")
                            }
                        }
                    }
                } ?: Text(
                    "No hay un evento vinculado a esta oportunidad.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                state.mensajeError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                state.mensajeExito?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }
                errorArchivo?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Text(
                    text = "Coordinación y condiciones",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                val responsables = state.usuarios.map {
                    OpcionDropdown(
                        it.id.toString(),
                        "${it.nombre} (${PerfilUsuario.desdeNombre(it.perfil).etiqueta})"
                    )
                }
                CampoDropdownFormulario(
                    label = "Responsable de la activación *",
                    selected = state.responsableId?.toString().orEmpty(),
                    options = responsables,
                    placeholder = "Selecciona un responsable",
                    error = if (responsables.isEmpty()) "No hay usuarios disponibles." else null,
                    onSelected = { viewModel.onResponsableChange(it.toLongOrNull()) }
                )

                CampoTextoFormulario(
                    label = "Espacio",
                    value = state.espacio,
                    error = null,
                    onValueChange = viewModel::onEspacioChange
                )

                OutlinedTextField(
                    value = state.fecha?.let { formatoFecha.format(Date(it)) }.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha") },
                    placeholder = { Text("Selecciona una fecha") },
                    trailingIcon = {
                        IconButton(onClick = { mostrarSelectorFecha = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Seleccionar fecha")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                CampoTextoFormulario(
                    label = "Costo",
                    value = state.costo,
                    error = null,
                    onValueChange = viewModel::onCostoChange,
                    keyboardType = KeyboardType.Decimal
                )

                CampoTextoFormulario(
                    label = "Requisitos",
                    value = state.requisitos,
                    error = null,
                    onValueChange = viewModel::onRequisitosChange,
                    singleLine = false
                )

                CampoTextoFormulario(
                    label = "Observaciones de coordinación",
                    value = state.observacionesCoordinacion,
                    error = null,
                    onValueChange = viewModel::onObservacionesCoordinacionChange,
                    singleLine = false
                )

                Button(
                    onClick = viewModel::guardarCoordinacion,
                    enabled = !state.guardando && !state.cargando && state.usuarios.isNotEmpty() &&
                        state.oportunidadConfirmada,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.guardando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Guardar coordinación")
                    }
                }

                Text(
                    text = "Evidencias",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Selecciona fotografías o documentos desde el dispositivo. " +
                        "El acceso se conserva para poder abrirlos después.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectorFotos.launch(arrayOf("image/*")) },
                        enabled = state.activacionId != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null)
                        Text("Foto", modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            selectorDocumentos.launch(arrayOf("application/*", "text/*"))
                        },
                        enabled = state.activacionId != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null)
                        Text("Documento", modifier = Modifier.padding(start = 6.dp))
                    }
                }
                if (state.activacionId == null) {
                    Text(
                        text = "Guarda primero la coordinación para habilitar los adjuntos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.evidencias.isEmpty()) {
                    Text(
                        text = "No hay evidencias adjuntas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.evidencias.forEach { evidencia ->
                        TarjetaEvidencia(evidencia = evidencia) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(evidencia.ruta), context.contentResolver.getType(Uri.parse(evidencia.ruta)))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                errorArchivo = "No hay una aplicación disponible para abrir «${evidencia.nombre}»."
                            } catch (_: SecurityException) {
                                errorArchivo = "Se perdió el acceso al archivo «${evidencia.nombre}»."
                            }
                        }
                    }
                }

                Text(
                    text = "Resultado y cierre",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                val opcionesResultado = listOf(
                    OpcionDropdown(
                        EstadoGestion.ACTIVACION_REALIZADA.name,
                        EstadoGestion.ACTIVACION_REALIZADA.etiqueta
                    ),
                    OpcionDropdown(
                        EstadoGestion.ACTIVACION_CANCELADA.name,
                        EstadoGestion.ACTIVACION_CANCELADA.etiqueta
                    )
                )
                CampoDropdownFormulario(
                    label = "Resultado final *",
                    selected = state.resultadoFinal,
                    options = opcionesResultado,
                    placeholder = "Selecciona el resultado",
                    error = null,
                    onSelected = viewModel::onResultadoFinalChange
                )
                OutlinedTextField(
                    value = state.fechaRealizacion?.let { formatoFecha.format(Date(it)) }.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha de realización") },
                    placeholder = { Text("Selecciona una fecha") },
                    trailingIcon = {
                        IconButton(onClick = { mostrarSelectorFechaResultado = true }) {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = "Seleccionar fecha de realización"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                CampoTextoFormulario(
                    label = "Asistentes estimados",
                    value = state.asistentesEstimados,
                    error = null,
                    onValueChange = viewModel::onAsistentesEstimadosChange,
                    keyboardType = KeyboardType.Number
                )
                CampoTextoFormulario(
                    label = "Contactos generados",
                    value = state.contactosGenerados,
                    error = null,
                    onValueChange = viewModel::onContactosGeneradosChange,
                    keyboardType = KeyboardType.Number
                )
                CampoTextoFormulario(
                    label = "Personas interesadas",
                    value = state.personasInteresadas,
                    error = null,
                    onValueChange = viewModel::onPersonasInteresadasChange,
                    keyboardType = KeyboardType.Number
                )
                CampoTextoFormulario(
                    label = "Inscripciones",
                    value = state.inscripciones,
                    error = null,
                    onValueChange = viewModel::onInscripcionesChange,
                    keyboardType = KeyboardType.Number
                )
                CampoTextoFormulario(
                    label = "Observaciones del resultado",
                    value = state.observaciones,
                    error = null,
                    onValueChange = viewModel::onObservacionesChange,
                    singleLine = false
                )
                CampoTextoFormulario(
                    label = "Impacto o evaluación",
                    value = state.impacto,
                    error = null,
                    onValueChange = viewModel::onImpactoChange,
                    singleLine = false
                )
                Button(
                    onClick = viewModel::guardarResultado,
                    enabled = !state.guardando && state.activacionId != null &&
                        state.resultadoFinal.isNotBlank() && state.oportunidadConfirmada,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (state.resultadoGuardado == null) {
                            "Guardar resultado"
                        } else {
                            "Actualizar resultado"
                        }
                    )
                }

                state.resultadoGuardado?.let {
                    Text(
                        text = "Estado actual: ${EstadoGestion.desdeNombre(it.resultado).etiqueta}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TarjetaEvidencia(evidencia: EvidenciaEntity, onAbrir: () -> Unit) {
    Card(
        onClick = onAbrir,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "${TipoEvidencia.desdeNombre(evidencia.tipo).etiqueta}: ${evidencia.nombre}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Toca para abrir",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
