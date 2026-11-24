package cl.duocuc.evaluacion2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duocuc.evaluacion2.data.model.PostResponse
import cl.duocuc.evaluacion2.ui.viewmodels.CampoEventoPost
import cl.duocuc.evaluacion2.ui.viewmodels.CampoPublicacion
import cl.duocuc.evaluacion2.ui.viewmodels.PostViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.PublicacionErrores

/** Prueba consultas y cambios en un servicio externo; no los guarda en la app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicacionesExternasScreen(
    viewModel: PostViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var nuevoIdEvento by remember { mutableStateOf("") }
    var nuevoNombreEvento by remember { mutableStateOf("") }
    var nuevaFecha by remember { mutableStateOf("") }
    var nuevaUbicacion by remember { mutableStateOf("") }
    var nuevaOrganizacion by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Consulta de registros", fontWeight = FontWeight.SemiBold)
                        Text(
                            "API de prueba",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Muestra la carga, un error con opción de reintentar o la lista de publicaciones.
            when {
                uiState.cargando -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                uiState.mensajeError != null -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = uiState.mensajeError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = viewModel::cargarPublicaciones) {
                        Text("Reintentar")
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "JSONPlaceholder simula estos datos y no conserva los cambios.",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Crear Evento (POST)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                OutlinedTextField(
                                    value = nuevoIdEvento,
                                    onValueChange = {
                                        nuevoIdEvento = it
                                        viewModel.limpiarErrorEvento(CampoEventoPost.ID)
                                    },
                                    label = { Text("ID del evento") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    ),
                                    isError = uiState.erroresEvento.idEvento != null,
                                    supportingText = uiState.erroresEvento.idEvento?.let {
                                        { Text(it, color = MaterialTheme.colorScheme.error) }
                                    },
                                    enabled = !uiState.mutacionEnCurso,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = nuevoNombreEvento,
                                    onValueChange = {
                                        nuevoNombreEvento = it
                                        viewModel.limpiarErrorEvento(CampoEventoPost.NOMBRE)
                                    },
                                    label = { Text("Nombre del evento") },
                                    singleLine = true,
                                    isError = uiState.erroresEvento.nombreEvento != null,
                                    supportingText = uiState.erroresEvento.nombreEvento?.let {
                                        { Text(it, color = MaterialTheme.colorScheme.error) }
                                    },
                                    enabled = !uiState.mutacionEnCurso,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = nuevaFecha,
                                    onValueChange = {
                                        nuevaFecha = it
                                        viewModel.limpiarErrorEvento(CampoEventoPost.FECHA)
                                    },
                                    label = { Text("Fecha (AAAA-MM-DD)") },
                                    singleLine = true,
                                    isError = uiState.erroresEvento.fecha != null,
                                    supportingText = uiState.erroresEvento.fecha?.let {
                                        { Text(it, color = MaterialTheme.colorScheme.error) }
                                    },
                                    enabled = !uiState.mutacionEnCurso,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = nuevaUbicacion,
                                    onValueChange = {
                                        nuevaUbicacion = it
                                        viewModel.limpiarErrorEvento(CampoEventoPost.UBICACION)
                                    },
                                    label = { Text("Comuna / ubicación") },
                                    singleLine = true,
                                    isError = uiState.erroresEvento.ubicacion != null,
                                    supportingText = uiState.erroresEvento.ubicacion?.let {
                                        { Text(it, color = MaterialTheme.colorScheme.error) }
                                    },
                                    enabled = !uiState.mutacionEnCurso,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = nuevaOrganizacion,
                                    onValueChange = {
                                        nuevaOrganizacion = it
                                        viewModel.limpiarErrorEvento(CampoEventoPost.ORGANIZACION)
                                    },
                                    label = { Text("Organización") },
                                    singleLine = true,
                                    isError = uiState.erroresEvento.organizacion != null,
                                    supportingText = uiState.erroresEvento.organizacion?.let {
                                        { Text(it, color = MaterialTheme.colorScheme.error) }
                                    },
                                    enabled = !uiState.mutacionEnCurso,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        viewModel.crearEvento(
                                            nuevoIdEvento,
                                            nuevoNombreEvento,
                                            nuevaFecha,
                                            nuevaUbicacion,
                                            nuevaOrganizacion
                                        )
                                    },
                                    enabled = !uiState.mutacionEnCurso,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (uiState.mutacionEnCurso) {
                                            "Enviando..."
                                        } else {
                                            "Enviar POST"
                                        }
                                    )
                                }
                            }
                        }
                    }
                    uiState.mensajeMutacion?.let { mensaje ->
                        item {
                            Text(
                                text = mensaje,
                                color = if (uiState.mutacionFallida) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    if (uiState.publicaciones.isEmpty()) {
                        item {
                            Text(
                                text = "La API no devolvió publicaciones.",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        items(uiState.publicaciones, key = PostResponse::id) { publicacion ->
                            TarjetaPublicacion(
                                publicacion = publicacion,
                                mutacionEnCurso = uiState.mutacionEnCurso,
                                errores = uiState.erroresPublicacion,
                                onLimpiarError = viewModel::limpiarErrorPublicacion,
                                onActualizar = { titulo, contenido ->
                                    viewModel.actualizarPublicacion(
                                        publicacion,
                                        titulo,
                                        contenido
                                    )
                                },
                                onEliminar = {
                                    viewModel.eliminarPublicacion(publicacion.id)
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaPublicacion(
    publicacion: PostResponse,
    mutacionEnCurso: Boolean,
    errores: PublicacionErrores,
    onLimpiarError: (CampoPublicacion) -> Unit,
    onActualizar: (String, String) -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editando by remember(publicacion.id, publicacion.title, publicacion.body) {
        mutableStateOf(false)
    }
    var confirmarEliminacion by remember(publicacion.id) { mutableStateOf(false) }
    var titulo by remember(publicacion.id, publicacion.title) {
        mutableStateOf(publicacion.title)
    }
    var contenido by remember(publicacion.id, publicacion.body) {
        mutableStateOf(publicacion.body)
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (editando) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = {
                        titulo = it
                        onLimpiarError(CampoPublicacion.TITULO)
                    },
                    label = { Text("Editar título") },
                    singleLine = true,
                    isError = errores.titulo != null,
                    supportingText = errores.titulo?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    enabled = !mutacionEnCurso,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contenido,
                    onValueChange = {
                        contenido = it
                        onLimpiarError(CampoPublicacion.CONTENIDO)
                    },
                    label = { Text("Editar contenido") },
                    isError = errores.contenido != null,
                    supportingText = errores.contenido?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    enabled = !mutacionEnCurso,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = publicacion.eventName ?: publicacion.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = publicacion.body,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "Publicación #${publicacion.id} · Usuario #${publicacion.userId}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            publicacion.eventId?.let { id ->
                Text(
                    text = "ID del evento #$id",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editando) {
                    Button(
                        onClick = { onActualizar(titulo, contenido) },
                        enabled = !mutacionEnCurso,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar cambios (PUT)")
                    }
                    TextButton(
                        onClick = {
                            titulo = publicacion.title
                            contenido = publicacion.body
                            onLimpiarError(CampoPublicacion.TITULO)
                            onLimpiarError(CampoPublicacion.CONTENIDO)
                            editando = false
                        },
                        enabled = !mutacionEnCurso
                    ) {
                        Text("Cancelar")
                    }
                } else {
                    OutlinedButton(
                        onClick = { editando = true },
                        enabled = !mutacionEnCurso
                    ) {
                        Text("Editar")
                    }
                    OutlinedButton(
                        onClick = { confirmarEliminacion = true },
                        enabled = !mutacionEnCurso
                    ) {
                        Text("Eliminar")
                    }
                }
            }
        }
    }

    if (confirmarEliminacion) {
        AlertDialog(
            onDismissRequest = {
                if (!mutacionEnCurso) {
                    confirmarEliminacion = false
                }
            },
            title = { Text("Eliminar publicación") },
            text = {
                Text("JSONPlaceholder simulará DELETE, pero no conservará el cambio.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarEliminacion = false
                        onEliminar()
                    },
                    enabled = !mutacionEnCurso
                ) {
                    Text("Eliminar (DELETE)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmarEliminacion = false },
                    enabled = !mutacionEnCurso
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
