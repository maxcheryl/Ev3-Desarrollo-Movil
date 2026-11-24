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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.Prioridad
import cl.duocuc.evaluacion2.ui.components.TarjetaOportunidad
import cl.duocuc.evaluacion2.ui.viewmodels.OportunidadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OportunidadListScreen(
    viewModel: OportunidadViewModel,
    onVolver: () -> Unit,
    onNueva: () -> Unit,
    onEscanear: () -> Unit,
    onSeleccionar: (Long) -> Unit,
    usuarios: Map<Long, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val oportunidades by viewModel.oportunidades.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val esSupervisor by viewModel.esSupervisor.collectAsState()
    var busqueda by remember { mutableStateOf("") }
    var estadoFiltro by remember { mutableStateOf("") }
    var prioridadFiltro by remember { mutableStateOf("") }
    var responsableFiltro by remember { mutableStateOf("") }

    val opcionesEstado = remember {
        listOf("" to "Todos") + EstadoGestion.entries.map { it.name to it.etiqueta }
    }
    val opcionesPrioridad = remember {
        listOf("" to "Todas") + Prioridad.entries.map { it.name to it.etiqueta }
    }
    val opcionesResponsable = remember(oportunidades, usuarios) {
        val asignados = oportunidades.mapNotNull { oportunidad ->
            oportunidad.responsableId?.let { id ->
                id.toString() to (usuarios[id] ?: "Responsable #$id")
            }
        }.distinctBy { it.first }.sortedBy { it.second }
        listOf("" to "Todos") +
            asignados +
            if (oportunidades.any { it.responsableId == null }) {
                listOf("SIN_ASIGNAR" to "Sin responsable")
            } else {
                emptyList()
            }
    }
    val termino = busqueda.trim()
    val oportunidadesFiltradas = oportunidades.filter { oportunidad ->
        val responsable = oportunidad.responsableId?.let {
            usuarios[it] ?: "Responsable #$it"
        }.orEmpty()
        val coincideBusqueda = termino.isBlank() || listOf(
            oportunidad.nombre,
            oportunidad.descripcion,
            oportunidad.comuna,
            EstadoGestion.desdeNombre(oportunidad.estado).etiqueta,
            Prioridad.desdeNombre(oportunidad.prioridad).etiqueta,
            responsable
        ).any { it.contains(termino, ignoreCase = true) }
        coincideBusqueda &&
            (estadoFiltro.isBlank() || oportunidad.estado == estadoFiltro) &&
            (prioridadFiltro.isBlank() || oportunidad.prioridad == prioridadFiltro) &&
            when (responsableFiltro) {
                "" -> true
                "SIN_ASIGNAR" -> oportunidad.responsableId == null
                else -> oportunidad.responsableId?.toString() == responsableFiltro
            }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Oportunidades", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (esSupervisor) {
                                "Todas las oportunidades"
                            } else {
                                "Mis oportunidades"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (!esSupervisor) {
                        IconButton(onClick = onEscanear) {
                            Icon(
                                Icons.Outlined.QrCodeScanner,
                                contentDescription = "Escanear código QR"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!esSupervisor) {
                ExtendedFloatingActionButton(
                    onClick = onNueva,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nueva oportunidad") }
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

                oportunidades.isEmpty() && esSupervisor -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { IndicadoresGenerales(emptyList()) }
                    item {
                        Text(
                            text = "Aún no hay oportunidades registradas.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                oportunidades.isEmpty() -> Text(
                    text = "Aún no tienes oportunidades registradas.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = if (esSupervisor) 24.dp else 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (esSupervisor) {
                        item {
                            IndicadoresGenerales(oportunidades)
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = busqueda,
                            onValueChange = { busqueda = it },
                            label = { Text("Buscar oportunidades") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FiltroDropdown(
                                label = "Estado",
                                selected = estadoFiltro,
                                opciones = opcionesEstado,
                                onSeleccionar = { estadoFiltro = it },
                                modifier = Modifier.weight(1f)
                            )
                            FiltroDropdown(
                                label = "Prioridad",
                                selected = prioridadFiltro,
                                opciones = opcionesPrioridad,
                                onSeleccionar = { prioridadFiltro = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        FiltroDropdown(
                            label = "Responsable",
                            selected = responsableFiltro,
                            opciones = opcionesResponsable,
                            onSeleccionar = { responsableFiltro = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = {
                                busqueda = ""
                                estadoFiltro = ""
                                prioridadFiltro = ""
                                responsableFiltro = ""
                            },
                            enabled = busqueda.isNotBlank() ||
                                estadoFiltro.isNotBlank() ||
                                prioridadFiltro.isNotBlank() ||
                                responsableFiltro.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Borrar filtros")
                        }
                    }
                    item {
                        Text(
                            text = "Mostrando ${oportunidadesFiltradas.size} de " +
                                "${oportunidades.size} oportunidades",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (oportunidadesFiltradas.isEmpty()) {
                        item {
                            Text(
                                text = "No hay oportunidades que coincidan con la búsqueda y los filtros.",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        items(oportunidadesFiltradas, key = { it.id }) { oportunidad ->
                            TarjetaOportunidad(
                                oportunidad = oportunidad,
                                responsableNombre = oportunidad.responsableId?.let {
                                    usuarios[it] ?: "Responsable #$it"
                                } ?: "Sin responsable asignado",
                                onClick = { onSeleccionar(oportunidad.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndicadoresGenerales(oportunidades: List<OpportunityEntity>) {
    val estadosFinales = setOf(
        EstadoGestion.OPORTUNIDAD_DESCARTADA.name,
        EstadoGestion.ACTIVACION_REALIZADA.name,
        EstadoGestion.ACTIVACION_CANCELADA.name
    )
    val enGestion = oportunidades.count { it.estado !in estadosFinales }
    val confirmadas = oportunidades.count {
        it.estado == EstadoGestion.PARTICIPACION_CONFIRMADA.name
    }
    val descartadas = oportunidades.count {
        it.estado == EstadoGestion.OPORTUNIDAD_DESCARTADA.name
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Indicadores generales",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Indicador("Total", oportunidades.size, Modifier.weight(1f))
                Indicador("En gestión", enGestion, Modifier.weight(1f))
                Indicador("Confirmadas", confirmadas, Modifier.weight(1f))
                Indicador("Descartadas", descartadas, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Indicador(etiqueta: String, valor: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = valor.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FiltroDropdown(
    label: String,
    selected: String,
    opciones: List<Pair<String, String>>,
    onSeleccionar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val seleccion = opciones.firstOrNull { it.first == selected }?.second ?: "Todos"

    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "$label: $seleccion",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            opciones.forEach { (valor, texto) ->
                DropdownMenuItem(
                    text = { Text(texto) },
                    onClick = {
                        onSeleccionar(valor)
                        expanded = false
                    }
                )
            }
        }
    }
}
