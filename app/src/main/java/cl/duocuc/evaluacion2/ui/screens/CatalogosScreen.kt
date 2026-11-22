package cl.duocuc.evaluacion2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duocuc.evaluacion2.ui.components.TarjetaAcceso

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogosScreen(
    onVolver: () -> Unit,
    onOrganizaciones: () -> Unit,
    onContactos: () -> Unit,
    onEventos: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Catálogos", fontWeight = FontWeight.SemiBold) },
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Datos de referencia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Administra las organizaciones, sus contactos y los eventos asociados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TarjetaAcceso(
                icono = Icons.Outlined.Business,
                titulo = "Organizaciones",
                descripcion = "Empresas, municipalidades e instituciones con las que se gestionan oportunidades.",
                onClick = onOrganizaciones
            )

            TarjetaAcceso(
                icono = Icons.Outlined.Contacts,
                titulo = "Contactos",
                descripcion = "Personas de contacto dentro de cada organización.",
                onClick = onContactos
            )

            TarjetaAcceso(
                icono = Icons.Outlined.Event,
                titulo = "Eventos",
                descripcion = "Instancias y fechas asociadas a una organización.",
                onClick = onEventos
            )
        }
    }
}
