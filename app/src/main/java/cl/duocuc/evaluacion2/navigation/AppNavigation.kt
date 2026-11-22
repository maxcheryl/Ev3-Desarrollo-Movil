package cl.duocuc.evaluacion2.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duocuc.evaluacion2.ui.screens.EscanearQrScreen
import cl.duocuc.evaluacion2.ui.screens.FormularioOportunidadScreen
import cl.duocuc.evaluacion2.ui.screens.LoginScreen
import cl.duocuc.evaluacion2.ui.screens.MenuScreen
import cl.duocuc.evaluacion2.ui.screens.OportunidadDetailScreen
import cl.duocuc.evaluacion2.ui.screens.OportunidadListScreen
import cl.duocuc.evaluacion2.ui.screens.PantallaInicialScreen
import cl.duocuc.evaluacion2.ui.screens.UbicacionScreen
import cl.duocuc.evaluacion2.ui.viewmodels.LoginViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.OportunidadViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.QrViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.UbicacionViewModel

/** Rutas de la aplicación (fuente única de verdad). */
sealed class Rutas(val route: String) {
    object PantallaInicial : Rutas("pantalla_inicial")
    object Login : Rutas("login")
    object MenuPrincipal : Rutas("menu_principal")
    object Oportunidades : Rutas("oportunidades")
    object EscanearQr : Rutas("escanear_qr")
    object Ubicacion : Rutas("ubicacion")
    object DetalleOportunidad : Rutas("detalle_oportunidad/{id}") {
        fun conId(id: Long) = "detalle_oportunidad/$id"
    }
    object FormularioOportunidad : Rutas("formulario_oportunidad?id={id}") {
        fun conId(id: Long) = "formulario_oportunidad?id=$id"
    }
}

/**
 * Host de navegación: organiza el flujo de pantallas de la aplicación.
 */
@Composable
fun AppNavigation(
    loginViewModel: LoginViewModel,
    oportunidadViewModel: OportunidadViewModel,
    qrViewModel: QrViewModel,
    ubicacionViewModel: UbicacionViewModel
) {
    val navController = rememberNavController()
    val usuario by loginViewModel.usuario.collectAsState()

    // Si la sesión en memoria se perdió (por ejemplo, tras recrear el proceso)
    // y la navegación restauró una pantalla protegida, se vuelve al login.
    LaunchedEffect(Unit) {
        val rutaActual = navController.currentDestination?.route
        if (usuario == null &&
            rutaActual != null &&
            rutaActual != Rutas.PantallaInicial.route &&
            rutaActual != Rutas.Login.route
        ) {
            navController.navigate(Rutas.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Rutas.PantallaInicial.route,
        // Transiciones suaves que acompañan la navegación sin retrasarla
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                slideInHorizontally(animationSpec = tween(300)) { ancho -> ancho / 4 }
        },
        exitTransition = { fadeOut(animationSpec = tween(250)) },
        popEnterTransition = { fadeIn(animationSpec = tween(250)) },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) +
                slideOutHorizontally(animationSpec = tween(300)) { ancho -> ancho / 4 }
        }
    ) {
        composable(Rutas.PantallaInicial.route) {
            PantallaInicialScreen(
                onIngresar = { navController.navigate(Rutas.Login.route) }
            )
        }

        composable(Rutas.Login.route) {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginExitoso = {
                    navController.navigate(Rutas.MenuPrincipal.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Rutas.MenuPrincipal.route) {
            MenuScreen(
                nombre = usuario?.nombre ?: "Usuario",
                perfil = usuario?.perfil?.etiqueta ?: "",
                onOportunidades = { navController.navigate(Rutas.Oportunidades.route) },
                onEscanearQr = { navController.navigate(Rutas.EscanearQr.route) },
                onUbicacion = { navController.navigate(Rutas.Ubicacion.route) },
                onCerrarSesion = {
                    loginViewModel.cerrarSesion()
                    // popUpTo(0) limpia toda la pila para que el botón atrás
                    // no pueda volver al menú sin sesión iniciada
                    navController.navigate(Rutas.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Rutas.Oportunidades.route) {
            OportunidadListScreen(
                viewModel = oportunidadViewModel,
                onVolver = { navController.popBackStack() },
                onNueva = {
                    navController.navigate(Rutas.FormularioOportunidad.conId(0L))
                },
                onEscanear = {
                    navController.navigate(Rutas.EscanearQr.route)
                },
                onSeleccionar = { id ->
                    navController.navigate(Rutas.DetalleOportunidad.conId(id))
                }
            )
        }

        composable(Rutas.EscanearQr.route) {
            EscanearQrScreen(
                qrViewModel = qrViewModel,
                oportunidadViewModel = oportunidadViewModel,
                onVolver = { navController.popBackStack() },
                onOportunidadEncontrada = { id ->
                    navController.navigate(Rutas.DetalleOportunidad.conId(id)) {
                        popUpTo(Rutas.EscanearQr.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Rutas.Ubicacion.route) {
            UbicacionScreen(
                viewModel = ubicacionViewModel,
                onVolver = { navController.popBackStack() }
            )
        }

        composable(
            route = Rutas.DetalleOportunidad.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            OportunidadDetailScreen(
                id = id,
                viewModel = oportunidadViewModel,
                onVolver = { navController.popBackStack() },
                onEditar = {
                    navController.navigate(Rutas.FormularioOportunidad.conId(id))
                },
                onEliminada = { navController.popBackStack() }
            )
        }

        composable(
            route = Rutas.FormularioOportunidad.route,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            FormularioOportunidadScreen(
                id = id,
                viewModel = oportunidadViewModel,
                onVolver = { navController.popBackStack() },
                onGuardado = { idGuardado ->
                    navController.navigate(Rutas.DetalleOportunidad.conId(idGuardado)) {
                        popUpTo(Rutas.Oportunidades.route)
                    }
                }
            )
        }
    }
}
