package cl.duocuc.evaluacion2.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.duocuc.evaluacion2.qr.CameraPermissionHelper
import cl.duocuc.evaluacion2.qr.QrScanner
import cl.duocuc.evaluacion2.ui.viewmodels.OportunidadViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.QrViewModel

/**
 * Lee un QR y busca la oportunidad relacionada.
 * Solo acepta códigos que comienzan con «MAX:»; un código inválido no cambia datos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscanearQrScreen(
    qrViewModel: QrViewModel,
    oportunidadViewModel: OportunidadViewModel,
    onVolver: () -> Unit,
    onOportunidadEncontrada: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by qrViewModel.uiState.collectAsState()
    val oportunidades by oportunidadViewModel.oportunidades.collectAsState()

    var permisoConcedido by remember {
        mutableStateOf(CameraPermissionHelper.tienePermisoCamara(context))
    }

    val solicitudPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        permisoConcedido = concedido
        if (concedido) {
            qrViewModel.reiniciarEscaneo()
        } else {
            qrViewModel.onPermisoDenegado()
        }
    }

    // Limpia el resultado anterior al abrir la pantalla.
    LaunchedEffect(Unit) {
        qrViewModel.reiniciarEscaneo()
    }

    // Busca la oportunidad solo si el código es válido.
    LaunchedEffect(uiState.contenido, uiState.esValido, oportunidades) {
        // Usa el resultado más reciente para no abrir la oportunidad del escaneo anterior.
        val estado = qrViewModel.uiState.value
        val contenido = estado.contenido
        if (contenido != null && estado.esValido && estado.mensaje == null &&
            oportunidades.isNotEmpty()
        ) {
            val encontrada = oportunidades.firstOrNull { it.codigoQr == contenido }
            if (encontrada != null) {
                onOportunidadEncontrada(encontrada.id)
            } else {
                qrViewModel.onSinCoincidencia()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Escanear código QR", fontWeight = FontWeight.SemiBold) },
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Anima los cambios entre estados que no muestran la cámara.
            val estadoQr = when {
                !permisoConcedido -> EstadoQr.Permiso
                uiState.escaneando -> EstadoQr.Escaneando
                uiState.contenido != null && uiState.esValido && uiState.mensaje == null ->
                    EstadoQr.Buscando
                else -> EstadoQr.Resultado
            }

            // La cámara queda fuera de la animación para que conserve su tamaño.
            if (estadoQr == EstadoQr.Escaneando) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Escanea un código QR",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "Enfoca el código QR dentro del marco central.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // La cámara ocupa el espacio libre sin cambiar de tamaño durante la animación.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        QrScanner(
                            onQrCodeScanned = { contenido ->
                                qrViewModel.onQrDetectado(contenido)
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Ayuda a centrar el código QR.
                        Surface(
                            modifier = Modifier
                                .size(250.dp)
                                .align(Alignment.Center),
                            color = Color.Transparent,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        ) {}
                    }
                }
                return@Column
            }

            Crossfade(
                targetState = estadoQr,
                animationSpec = tween(300),
                label = "estadoQr"
            ) { estado ->
                when (estado) {
                    EstadoQr.Permiso -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NoPhotography,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Permiso de cámara requerido",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Para escanear códigos QR de oportunidades necesitamos acceso a la cámara del dispositivo.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            uiState.mensaje?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Button(onClick = { solicitudPermiso.launch(Manifest.permission.CAMERA) }) {
                                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Conceder permiso de cámara")
                            }
                        }
                    }
                    // Este estado ya se mostró antes de la animación.
                    EstadoQr.Escaneando -> {}
                    EstadoQr.Buscando -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Código válido, buscando oportunidad…",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    EstadoQr.Resultado -> {
                        // Muestra si el código no sirve, no coincide o falta permiso.
                        val esCodigoInvalido = uiState.contenido != null && !uiState.esValido
                        val esSinCoincidencia = uiState.contenido != null && uiState.esValido

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    esCodigoInvalido -> Icons.Outlined.ErrorOutline
                                    esSinCoincidencia -> Icons.Outlined.QrCodeScanner
                                    else -> Icons.Outlined.NoPhotography
                                },
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = if (esCodigoInvalido) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                            Text(
                                text = when {
                                    esCodigoInvalido -> "Código QR no válido"
                                    esSinCoincidencia -> "Oportunidad no encontrada"
                                    else -> "No se pudo escanear"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = if (esCodigoInvalido) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )

                            if (esCodigoInvalido) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                ) {
                                    Text(
                                        text = uiState.contenido.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }

                            uiState.mensaje?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (esCodigoInvalido) {
                                Text(
                                    text = "No se creó ni modificó ninguna oportunidad.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (permisoConcedido) {
                                Button(
                                    onClick = { qrViewModel.reiniciarEscaneo() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                                    Spacer(Modifier.size(8.dp))
                                    Text("Escanear otro código")
                                }
                            } else {
                                Button(
                                    onClick = { solicitudPermiso.launch(Manifest.permission.CAMERA) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Conceder permiso de cámara")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Estados que puede mostrar la pantalla del escáner. */
private enum class EstadoQr {
    Permiso,
    Escaneando,
    Buscando,
    Resultado
}
