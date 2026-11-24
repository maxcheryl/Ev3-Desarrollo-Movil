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
import androidx.compose.ui.unit.dp
import cl.duocuc.evaluacion2.data.model.PostResponse
import cl.duocuc.evaluacion2.ui.viewmodels.PostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicacionesExternasScreen(
    viewModel: PostViewModel,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var nuevoTitulo by remember { mutableStateOf("") }
    var nuevoContenido by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("API de prueba", fontWeight = FontWeight.SemiBold) },
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

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "JSONPlaceholder simula estos datos y no conserva los cambios.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Crear publicación (POST)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = nuevoTitulo,
                                onValueChange = { nuevoTitulo = it },
                                label = { Text("Título") },
                                singleLine = true,
                                enabled = !uiState.mutacionEnCurso,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = nuevoContenido,
                                onValueChange = { nuevoContenido = it },
                                label = { Text("Contenido") },
                                enabled = !uiState.mutacionEnCurso,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    viewModel.crearPublicacion(nuevoTitulo, nuevoContenido)
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
                    uiState.mensajeMutacion?.let { mensaje ->
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
                    if (uiState.publicaciones.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "La API no devolvió publicaciones.",
                                modifier = Modifier.padding(24.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.publicaciones, key = PostResponse::id) { publicacion ->
                                TarjetaPublicacion(
                                    publicacion = publicacion,
                                    mutacionEnCurso = uiState.mutacionEnCurso,
                                    onActualizar = { titulo, contenido ->
                                        viewModel.actualizarPublicacion(
                                            publicacion,
                                            titulo,
                                            contenido
                                        )
                                    },
                                    onEliminar = {
                                        viewModel.eliminarPublicacion(publicacion.id)
                                    }
                                )
                            }
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
    onActualizar: (String, String) -> Unit,
    onEliminar: () -> Unit
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

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (editando) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Editar título") },
                    singleLine = true,
                    enabled = !mutacionEnCurso,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contenido,
                    onValueChange = { contenido = it },
                    label = { Text("Editar contenido") },
                    enabled = !mutacionEnCurso,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = publicacion.title,
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
