package cl.duocuc.evaluacion2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import cl.duocuc.evaluacion2.data.session.SessionManager
import cl.duocuc.evaluacion2.navigation.AppNavigation
import cl.duocuc.evaluacion2.ui.theme.MaxTheme
import cl.duocuc.evaluacion2.ui.viewmodels.ContactoViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.EventoViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.GestionViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.LoginViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.OportunidadViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.OrganizacionViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.QrViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.UbicacionViewModel

/**
 * Única Activity de la app (patrón single-activity con Compose).
 * Los ViewModels se crean aquí y se comparten con toda la navegación,
 * de modo que sus estados sobreviven al cambio de pantalla.
 */
class MainActivity : ComponentActivity() {

    private val sessionManager = SessionManager()

    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModel.factory(application, sessionManager)
    }

    private val oportunidadViewModel: OportunidadViewModel by viewModels {
        OportunidadViewModel.factory(application, sessionManager)
    }

    private val gestionViewModel: GestionViewModel by viewModels {
        GestionViewModel.factory(application, sessionManager)
    }

    private val qrViewModel: QrViewModel by viewModels()

    private val organizacionViewModel: OrganizacionViewModel by viewModels {
        OrganizacionViewModel.factory(application)
    }

    private val contactoViewModel: ContactoViewModel by viewModels {
        ContactoViewModel.factory(application)
    }

    private val eventoViewModel: EventoViewModel by viewModels {
        EventoViewModel.factory(application)
    }

    private val ubicacionViewModel: UbicacionViewModel by viewModels {
        UbicacionViewModel.factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaxTheme {
                AppNavigation(
                    loginViewModel = loginViewModel,
                    oportunidadViewModel = oportunidadViewModel,
                    gestionViewModel = gestionViewModel,
                    organizacionViewModel = organizacionViewModel,
                    contactoViewModel = contactoViewModel,
                    eventoViewModel = eventoViewModel,
                    qrViewModel = qrViewModel,
                    ubicacionViewModel = ubicacionViewModel
                )
            }
        }
    }
}
