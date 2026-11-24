package cl.duocuc.evaluacion2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duocuc.evaluacion2.ui.components.TarjetaAcceso

/**
 * Menú principal de la aplicación: punto de acceso a los módulos.
 */
@Composable
fun MenuScreen(
    nombre: String,
    perfil: String,
    esSupervisor: Boolean,
    onOportunidades: () -> Unit,
    onCatalogos: () -> Unit,
    onCatalogoEventosExternos: () -> Unit,
    onEscanearQr: () -> Unit,
    onUbicacion: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = "Hola, $nombre",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = perfil,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "¿Qué gestión realizaremos hoy?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        TarjetaAcceso(
            icono = Icons.Outlined.EventAvailable,
            titulo = "Oportunidades",
            descripcion = if (esSupervisor) {
                "Consulta, filtra y da seguimiento a las oportunidades comerciales."
            } else {
                "Consulta, crea y edita oportunidades de activación corporativa."
            },
            onClick = onOportunidades
        )

        if (!esSupervisor) {
            TarjetaAcceso(
                icono = Icons.Outlined.Category,
                titulo = "Organizaciones y contactos",
                descripcion = "Administra las organizaciones y sus contactos registrados.",
                onClick = onCatalogos
            )

            TarjetaAcceso(
                icono = Icons.Outlined.Event,
                titulo = "Catálogo de eventos externos",
                descripcion = "Consulta registros y crea eventos mediante la API de prueba.",
                onClick = onCatalogoEventosExternos
            )

            TarjetaAcceso(
                icono = Icons.Outlined.QrCodeScanner,
                titulo = "Escanear código QR",
                descripcion = "Escanea el código QR de una oportunidad para abrirla al instante.",
                onClick = onEscanearQr
            )

            TarjetaAcceso(
                icono = Icons.Outlined.LocationOn,
                titulo = "Mi ubicación",
                descripcion = "Obtén tu ubicación de referencia para contextualizar la gestión.",
                onClick = onUbicacion
            )
        }

        Spacer(Modifier.height(8.dp))

        FilledTonalButton(
            onClick = onCerrarSesion,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.Logout, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Cerrar sesión")
        }
    }
}
