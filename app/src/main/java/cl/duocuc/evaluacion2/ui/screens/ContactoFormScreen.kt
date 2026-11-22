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
import cl.duocuc.evaluacion2.ui.components.CampoDropdownFormulario
import cl.duocuc.evaluacion2.ui.components.CampoTextoFormulario
import cl.duocuc.evaluacion2.ui.components.OpcionDropdown
import cl.duocuc.evaluacion2.ui.viewmodels.ContactoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactoFormScreen(
    viewModel: ContactoViewModel,
    contactoId: Long?,
    onVolver: () -> Unit,
    onGuardado: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editor by viewModel.editor.collectAsState()
    val organizaciones by viewModel.organizaciones.collectAsState()

    LaunchedEffect(contactoId) {
        if (contactoId == null || contactoId == 0L) {
            viewModel.beginNueva()
        } else {
            viewModel.beginEditar(contactoId)
        }
    }

    val esEdicion = editor.form.id != 0L
    val opcionesOrganizacion = organizaciones.map {
        OpcionDropdown(valor = it.id.toString(), etiqueta = it.nombre)
    }
    val errorOrganizacion = editor.errores.organizacionId
        ?: if (organizaciones.isEmpty()) "Crea primero una organización." else null

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (esEdicion) "Editar contacto" else "Nuevo contacto",
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
                    text = "No hay organizaciones registradas. Crea una en Catálogos → Organizaciones antes de agregar contactos.",
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

            CampoDropdownFormulario(
                label = "Organización *",
                selected = editor.form.organizacionId?.toString().orEmpty(),
                options = opcionesOrganizacion,
                placeholder = "Selecciona una organización",
                error = errorOrganizacion,
                onSelected = { valor -> viewModel.onOrganizacionChange(valor.toLongOrNull()) }
            )

            CampoTextoFormulario(
                label = "Cargo *",
                value = editor.form.cargo,
                error = editor.errores.cargo,
                onValueChange = viewModel::onCargoChange
            )

            CampoTextoFormulario(
                label = "Área",
                value = editor.form.area,
                error = null,
                onValueChange = viewModel::onAreaChange
            )

            CampoTextoFormulario(
                label = "Correo *",
                value = editor.form.correo,
                error = editor.errores.correo,
                onValueChange = viewModel::onCorreoChange,
                keyboardType = KeyboardType.Email
            )

            CampoTextoFormulario(
                label = "Teléfono *",
                value = editor.form.telefono,
                error = editor.errores.telefono,
                onValueChange = viewModel::onTelefonoChange,
                keyboardType = KeyboardType.Phone
            )

            CampoTextoFormulario(
                label = "Observaciones",
                value = editor.form.observaciones,
                error = null,
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
                    enabled = !editor.guardando && organizaciones.isNotEmpty()
                ) {
                    if (editor.guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text(if (esEdicion) "Guardar cambios" else "Crear contacto")
                    }
                }
            }
        }
    }
}
