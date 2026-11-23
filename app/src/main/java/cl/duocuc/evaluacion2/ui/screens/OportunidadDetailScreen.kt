package cl.duocuc.evaluacion2.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duocuc.evaluacion2.data.local.GestionEntity
import cl.duocuc.evaluacion2.data.local.HistorialCambioEntity
import cl.duocuc.evaluacion2.data.local.ContactoEntity
import cl.duocuc.evaluacion2.data.local.EventoEntity
import cl.duocuc.evaluacion2.data.local.OrganizacionEntity
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.PotencialComercial
import cl.duocuc.evaluacion2.data.model.Prioridad
import cl.duocuc.evaluacion2.data.model.TipoAccionHistorial
import cl.duocuc.evaluacion2.data.model.TipoGestion
import cl.duocuc.evaluacion2.data.model.TipoOportunidad
import cl.duocuc.evaluacion2.ui.viewmodels.GestionViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.ContactoViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.EventoViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.OrganizacionViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.OportunidadViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Detalle de una oportunidad seleccionada, con opción de editarla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OportunidadDetailScreen(
    id: Long,
    viewModel: OportunidadViewModel,
    gestionViewModel: GestionViewModel,
    organizacionViewModel: OrganizacionViewModel,
    contactoViewModel: ContactoViewModel,
    eventoViewModel: EventoViewModel,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onRegistrarGestion: () -> Unit,
    onGestionarActivacion: () -> Unit,
    onEliminada: () -> Unit,
    modifier: Modifier = Modifier
) {
    val oportunidades by viewModel.oportunidades.collectAsState()
    val oportunidad = oportunidades.firstOrNull { it.id == id }
    val gestiones by gestionViewModel.gestiones.collectAsState()
    val historial by gestionViewModel.historial.collectAsState()
    val usuarios by gestionViewModel.usuarios.collectAsState()
    val esEjecutivo by gestionViewModel.esEjecutivo.collectAsState()
    val organizaciones by organizacionViewModel.organizaciones.collectAsState()
    val contactos by contactoViewModel.contactos.collectAsState()
    val eventos by eventoViewModel.eventos.collectAsState()
    var confirmarEliminacion by remember(id) { mutableStateOf(false) }
    var eliminando by remember(id) { mutableStateOf(false) }
    var errorEliminacion by remember(id) { mutableStateOf<String?>(null) }

    LaunchedEffect(id) {
        gestionViewModel.seleccionarOportunidad(id)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Detalle de oportunidad", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (oportunidad != null) {
                        IconButton(onClick = onEditar) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar oportunidad")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (oportunidad == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "La oportunidad ya no está disponible.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            DetalleContenido(
                oportunidad = oportunidad,
                gestiones = gestiones,
                historial = historial,
                usuarios = usuarios,
                organizaciones = organizaciones,
                contactos = contactos,
                eventos = eventos,
                puedeRegistrarGestion = esEjecutivo,
                onRegistrarGestion = onRegistrarGestion,
                onGestionarActivacion = onGestionarActivacion,
                onEliminar = { confirmarEliminacion = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }

    if (confirmarEliminacion && oportunidad != null) {
        AlertDialog(
            onDismissRequest = {
                if (!eliminando) {
                    confirmarEliminacion = false
                    errorEliminacion = null
                }
            },
            title = { Text("Eliminar oportunidad") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Seguro que deseas eliminar «${oportunidad.nombre}»? Esta acción no se puede deshacer.")
                    errorEliminacion?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !eliminando,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        eliminando = true
                        errorEliminacion = null
                        viewModel.eliminarOportunidad(
                            id = id,
                            onListo = onEliminada,
                            onError = { mensaje ->
                                eliminando = false
                                errorEliminacion = mensaje
                            }
                        )
                    }
                ) {
                    Text(if (eliminando) "Eliminando..." else "Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !eliminando,
                    onClick = {
                        confirmarEliminacion = false
                        errorEliminacion = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun DetalleContenido(
    oportunidad: OpportunityEntity,
    gestiones: List<GestionEntity>,
    historial: List<HistorialCambioEntity>,
    usuarios: Map<Long, String>,
    organizaciones: List<OrganizacionEntity>,
    contactos: List<ContactoEntity>,
    eventos: List<EventoEntity>,
    puedeRegistrarGestion: Boolean,
    onRegistrarGestion: () -> Unit,
    onGestionarActivacion: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tipo = TipoOportunidad.desdeNombre(oportunidad.tipo)
    val prioridad = Prioridad.desdeNombre(oportunidad.prioridad)
    val potencial = PotencialComercial.desdeNombre(oportunidad.potencialComercial)
    val estado = EstadoGestion.desdeNombre(oportunidad.estado)
    val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val organizacion = organizaciones.firstOrNull { it.id == oportunidad.organizacionId }
    val contacto = contactos.firstOrNull { it.id == oportunidad.contactoId }
    val evento = eventos.firstOrNull { it.id == oportunidad.eventoId }
    val formatoFechaEvento = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    val organizacionEvento = evento?.let { relacionado ->
        organizaciones.firstOrNull { it.id == relacionado.organizacionId }?.nombre
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = tipo.etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = oportunidad.nombre,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = estado.etiqueta,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CampoDetalle("Descripción", oportunidad.descripcion)
                CampoDetalle("Fuente u origen", oportunidad.fuente)
                CampoDetalle("Prioridad", prioridad.etiqueta)
                CampoDetalle("Potencial comercial", potencial.etiqueta)
                CampoDetalle("Estado de gestión", estado.etiqueta)
                CampoDetalle("Organización", organizacion?.nombre ?: "Sin organización asociada")
                CampoDetalle(
                    "Contacto",
                    contacto?.let { "${it.nombre} · ${it.cargo}" } ?: "Sin contacto asociado"
                )
                CampoDetalle(
                    "Evento o actividad",
                    evento?.let {
                        "${it.nombre} · ${formatoFechaEvento.format(Date(it.fecha))} · " +
                            "${it.comuna} · ${organizacionEvento ?: "Sin organización"}"
                    } ?: "Sin evento asociado"
                )
                if (oportunidad.observaciones.isNotBlank()) {
                    CampoDetalle("Observaciones", oportunidad.observaciones)
                }
                if (oportunidad.codigoQr.isNotBlank()) {
                    CampoDetalle("Referencia QR", oportunidad.codigoQr)
                }
            }
        }

        Text(
            text = "Creada: ${formatoFecha.format(Date(oportunidad.fechaCreacion))}" +
                "\nÚltima actualización: ${formatoFecha.format(Date(oportunidad.fechaActualizacion))}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        GestionesSeccion(
            gestiones = gestiones,
            usuarios = usuarios,
            puedeRegistrar = puedeRegistrarGestion,
            onRegistrar = onRegistrarGestion
        )

        OutlinedButton(
            onClick = onGestionarActivacion,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.EventAvailable, contentDescription = null)
            Text("Gestionar activación", modifier = Modifier.padding(start = 8.dp))
        }

        HistorialSeccion(
            historial = historial,
            usuarios = usuarios
        )

        OutlinedButton(
            onClick = onEliminar,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null)
            Text("Eliminar oportunidad", modifier = Modifier.padding(start = 8.dp))
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CampoDetalle(etiqueta: String, valor: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun GestionesSeccion(
    gestiones: List<GestionEntity>,
    usuarios: Map<Long, String>,
    puedeRegistrar: Boolean,
    onRegistrar: () -> Unit
) {
    val formatoFechaHora = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }
    val formatoFecha = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Gestiones y seguimientos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        if (gestiones.isEmpty()) {
            Text(
                text = "Aún no hay gestiones registradas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            gestiones.forEach { gestion ->
                TarjetaGestion(
                    gestion = gestion,
                    usuario = usuarios[gestion.usuarioId] ?: "Usuario",
                    fechaHora = formatoFechaHora.format(Date(gestion.fecha)),
                    fechaProximaAccion = gestion.fechaProximaAccion?.let {
                        formatoFecha.format(Date(it))
                    }
                )
            }
        }

        if (puedeRegistrar) {
            Button(
                onClick = onRegistrar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("Registrar gestión", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun TarjetaGestion(
    gestion: GestionEntity,
    usuario: String,
    fechaHora: String,
    fechaProximaAccion: String?
) {
    val tipo = TipoGestion.desdeNombre(gestion.tipo)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tipo.etiqueta,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = fechaHora,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = gestion.descripcion,
                style = MaterialTheme.typography.bodyMedium
            )

            if (gestion.resultado.isNotBlank()) {
                Text(
                    text = "Resultado: ${gestion.resultado}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (gestion.proximaAccion.isNotBlank()) {
                Text(
                    text = "Próxima acción: ${gestion.proximaAccion}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (fechaProximaAccion != null) {
                Text(
                    text = "Fecha de próxima acción: $fechaProximaAccion",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "Registrada por $usuario",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistorialSeccion(
    historial: List<HistorialCambioEntity>,
    usuarios: Map<Long, String>
) {
    val formatoFechaHora = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Historial de cambios",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        if (historial.isEmpty()) {
            Text(
                text = "Aún no hay cambios registrados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            historial.forEach { cambio ->
                TarjetaHistorial(
                    cambio = cambio,
                    usuario = usuarios[cambio.usuarioId] ?: "Usuario",
                    fechaHora = formatoFechaHora.format(Date(cambio.fecha))
                )
            }
        }
    }
}

@Composable
private fun TarjetaHistorial(
    cambio: HistorialCambioEntity,
    usuario: String,
    fechaHora: String
) {
    val tipo = TipoAccionHistorial.desdeNombre(cambio.tipoAccion)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tipo.etiqueta,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = fechaHora,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = cambio.descripcion,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Por $usuario",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
