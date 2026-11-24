package cl.duocuc.evaluacion2.qr

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Muestra la cámara y busca códigos QR.
 * Al encontrar uno, deja de leer hasta que se reinicie la pantalla.
 */
@SuppressLint("UnsafeOptInUsageError")
@Composable
fun QrScanner(
    onQrCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Actualiza la función de respuesta sin reiniciar la cámara.
    val currentCallback = rememberUpdatedState(onQrCodeScanned)

    // Crea la vista de cámara una sola vez.
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER

            // Este modo funciona mejor en algunos emuladores.
            implementationMode =
                PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Muestra la cámara dentro de la pantalla.
    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize()
    )

    // Inicia y detiene la cámara según se abre o se cierra esta pantalla.
    DisposableEffect(context, lifecycleOwner, previewView) {

        val cameraExecutor = Executors.newSingleThreadExecutor()

        // Busca solo códigos QR.
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        val barcodeScanner = BarcodeScanning.getClient(options)

        val disposed = AtomicBoolean(false)
        val detected = AtomicBoolean(false)

        // Prepara la imagen que verá el usuario.
        val preview = Preview.Builder()
            .build()
            .apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

        // Prepara la lectura de las imágenes de la cámara.
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(
                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            )
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->

            if (disposed.get() || detected.get()) {
                imageProxy.close()
            } else {

                val mediaImage = imageProxy.image

                if (mediaImage == null) {
                    imageProxy.close()
                } else {

                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    try {
                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->

                                val qr = barcodes.firstOrNull {
                                    it.format == Barcode.FORMAT_QR_CODE &&
                                            it.rawValue != null
                                }

                                if (
                                    qr != null &&
                                    !disposed.get() &&
                                    detected.compareAndSet(false, true)
                                ) {
                                    imageAnalysis.clearAnalyzer()

                                    currentCallback.value(
                                        qr.rawValue!!
                                    )

                                    Log.d(
                                        "QrScanner",
                                        "Código QR detectado"
                                    )
                                }
                            }
                            .addOnFailureListener { error ->
                                Log.e(
                                    "QrScanner",
                                    "Error al analizar código QR",
                                    error
                                )
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }

                    } catch (error: Exception) {
                        Log.e(
                            "QrScanner",
                            "Error al procesar imagen",
                            error
                        )
                        imageProxy.close()
                    }
                }
            }
        }

        // Prepara la cámara para usarla en esta pantalla.
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        var boundProvider: ProcessCameraProvider? = null

        cameraProviderFuture.addListener({

            if (!disposed.get()) {

                try {
                    val cameraProvider =
                        cameraProviderFuture.get()

                    if (!disposed.get()) {

                        // Usa la cámara trasera.
                        val cameraSelector =
                            CameraSelector.DEFAULT_BACK_CAMERA

                        // Conecta la vista de cámara y la lectura QR a esta pantalla.
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )

                        boundProvider = cameraProvider

                        Log.d(
                            "QrScanner",
                            "CameraX vinculado correctamente"
                        )
                    }

                } catch (error: Exception) {
                    Log.e(
                        "QrScanner",
                        "Error al iniciar la cámara",
                        error
                    )
                }
            }

        }, ContextCompat.getMainExecutor(context))

        // Cierra la cámara y el lector al salir de la pantalla.
        onDispose {

            disposed.set(true)

            imageAnalysis.clearAnalyzer()

            boundProvider?.unbind(
                preview,
                imageAnalysis
            )

            barcodeScanner.close()

            cameraExecutor.shutdown()

            Log.d(
                "QrScanner",
                "Recursos de cámara liberados"
            )
        }
    }
}
