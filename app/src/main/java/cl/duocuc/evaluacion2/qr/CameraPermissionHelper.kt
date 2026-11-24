package cl.duocuc.evaluacion2.qr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** Revisa si la app tiene permiso para usar la cámara. */
object CameraPermissionHelper {

    fun tienePermisoCamara(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}
