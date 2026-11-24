package cl.duocuc.evaluacion2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duocuc.evaluacion2.data.local.EventoEntity
import cl.duocuc.evaluacion2.ui.viewmodels.EventoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Muestra los eventos y permite abrir uno para editarlo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventoListScreen(
    viewModel: EventoViewModel,
    onVolver: () -> Unit,
    onNuevo: () -> Unit,
    onSeleccionar: (Long) -> Unit,
    onConsultarApi: () -> Unit,
    puedeEditar: Boolean,
    modifier: Modifier = Modifier
) {
    val eventos by viewModel.eventos.collectAsState()
    val organizaciones by viewModel.organizaciones.collectAsState()
    val cargando by viewModel.cargando.collectAsState()

    // Busca el nombre de cada organización para mostrarlo junto al evento.
    val nombresOrganizacion = remember(organizaciones) {
        organizaciones.associate { it.id to it.nombre }
    }

    val formatoFecha = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Eventos", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = onConsultarApi) {
                        Text("API de prueba")
                    }
                }
            )
        },
        floatingActionButton = {
            if (puedeEditar) {
                ExtendedFloatingActionButton(
                    onClick = onNuevo,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nuevo evento") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                cargando -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                eventos.isEmpty() -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Aún no hay eventos registrados.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (puedeEditar) {
                            "Toca «Nuevo evento» para comenzar."
                        } else {
                            "Los eventos registrados aparecerán aquí."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(eventos, key = { it.id }) { evento ->
                        TarjetaEvento(
                            evento = evento,
                            organizacion = nombresOrganizacion[evento.organizacionId] ?: "Sin organización",
                            fechaFormateada = formatoFecha.format(Date(evento.fecha)),
                            onClick = { onSeleccionar(evento.id) },
                            puedeEditar = puedeEditar,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaEvento(
    evento: EventoEntity,
    organizacion: String,
    fechaFormateada: String,
    onClick: () -> Unit,
    puedeEditar: Boolean,
    modifier: Modifier = Modifier
) {
    @Composable
    fun Contenido() {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = evento.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "$fechaFormateada · ${evento.comuna}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = organizacion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (puedeEditar) {
        ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
            Contenido()
        }
    } else {
        ElevatedCard(modifier = modifier.fillMaxWidth()) {
            Contenido()
        }
    }
}
