package cl.duocuc.evaluacion2.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/** Resultado de pedir la ubicación una vez. */
sealed class ResultadoUbicacion {
    data class Disponible(val latitud: Double, val longitud: Double) : ResultadoUbicacion()
    data object PermisoDenegado : ResultadoUbicacion()
    data object NoDisponible : ResultadoUbicacion()
}

/**
 * Pide la ubicación al sistema una vez, sin mantener un seguimiento.
 */
object UbicacionHelper {

    private const val TIMEOUT_MILLIS = 10_000L

    fun tienePermiso(context: Context): Boolean {
        val conFina = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val conAproximada = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return conFina || conAproximada
    }

    /** Pide una ubicación y entrega el resultado al hilo que actualiza la pantalla. */
    @SuppressLint("MissingPermission", "DEPRECATION")
    fun obtenerUbicacionUnaVez(
        context: Context,
        onResultado: (ResultadoUbicacion) -> Unit
    ) {
        if (!tienePermiso(context)) {
            onResultado(ResultadoUbicacion.PermisoDenegado)
            return
        }

        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Usa GPS si está disponible; si no, intenta con la red.
        val proveedor = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (proveedor == null) {
            onResultado(ResultadoUbicacion.NoDisponible)
            return
        }

        // La respuesta y el tiempo límite pueden llegar juntos. Esta variable evita responder dos veces.
        var respondido = false
        val handler = Handler(Looper.getMainLooper())

        // Si no llega una ubicación en 10 segundos, termina la espera.
        val timeout = Runnable {
            if (!respondido) {
                respondido = true
                onResultado(ResultadoUbicacion.NoDisponible)
            }
        }

        val listener = android.location.LocationListener { location ->
            if (!respondido) {
                respondido = true
                handler.removeCallbacks(timeout)
                onResultado(
                    ResultadoUbicacion.Disponible(
                        latitud = location.latitude,
                        longitud = location.longitude
                    )
                )
            }
        }

        try {
            locationManager.requestSingleUpdate(
                proveedor,
                listener,
                Looper.getMainLooper()
            )
            handler.postDelayed(timeout, TIMEOUT_MILLIS)
        } catch (e: Exception) {
            handler.removeCallbacks(timeout)
            if (!respondido) {
                respondido = true
                onResultado(ResultadoUbicacion.NoDisponible)
            }
        }
    }
}
