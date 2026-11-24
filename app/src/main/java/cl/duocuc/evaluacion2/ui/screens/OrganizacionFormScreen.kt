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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duocuc.evaluacion2.data.model.TipoOrganizacion
import cl.duocuc.evaluacion2.ui.components.CampoDropdownFormulario
import cl.duocuc.evaluacion2.ui.components.CampoTextoFormulario
import cl.duocuc.evaluacion2.ui.components.OpcionDropdown
import cl.duocuc.evaluacion2.ui.viewmodels.OrganizacionViewModel

/** Crea una organización o permite actualizar sus datos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizacionFormScreen(
    viewModel: OrganizacionViewModel,
    organizacionId: Long?,
    onVolver: () -> Unit,
    onGuardado: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editor by viewModel.editor.collectAsState()

    // Carga la organización elegida o prepara el formulario para una nueva.
    LaunchedEffect(organizacionId) {
        if (organizacionId == null || organizacionId == 0L) {
            viewModel.beginNueva()
        } else {
            viewModel.beginEditar(organizacionId)
        }
    }

    val esEdicion = editor.form.id != 0L
    // Al editar, conserva el tipo anterior aunque ya no se ofrezca para nuevas organizaciones.
    val opcionesTipo = TipoOrganizacion.entries
        .filter { it != TipoOrganizacion.INSTITUCION || editor.form.tipo == it.name }
        .map { OpcionDropdown(it.name, it.etiqueta) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (esEdicion) "Editar organización" else "Nueva organización",
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
            if (!esEdicion) {
                Text(
                    text = "Completa los datos de la organización.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            CampoTextoFormulario(
                label = "Nombre *",
                value = editor.form.nombre,
                error = editor.errores.nombre,
                onValueChange = viewModel::onNombreChange
            )

            CampoDropdownFormulario(
                label = "Tipo *",
                selected = editor.form.tipo,
                options = opcionesTipo,
                placeholder = "Selecciona un tipo",
                error = editor.errores.tipo,
                onSelected = viewModel::onTipoChange
            )

            CampoTextoFormulario(
                label = "Comuna *",
                value = editor.form.comuna,
                error = editor.errores.comuna,
                onValueChange = viewModel::onComunaChange
            )

            CampoTextoFormulario(
                label = "Sitio web",
                value = editor.form.sitioWeb,
                error = editor.errores.sitioWeb,
                onValueChange = viewModel::onSitioWebChange,
                keyboardType = KeyboardType.Uri
            )

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
                    enabled = !editor.guardando
                ) {
                    if (editor.guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text(if (esEdicion) "Guardar cambios" else "Crear organización")
                    }
                }
            }
        }
    }
}
