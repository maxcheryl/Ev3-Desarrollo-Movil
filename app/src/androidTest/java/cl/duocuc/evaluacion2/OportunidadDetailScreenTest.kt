package cl.duocuc.evaluacion2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.duocuc.evaluacion2.data.local.AppDatabase
import cl.duocuc.evaluacion2.data.local.OpportunityEntity
import cl.duocuc.evaluacion2.data.model.EstadoGestion
import cl.duocuc.evaluacion2.data.model.PerfilUsuario
import cl.duocuc.evaluacion2.data.model.PotencialComercial
import cl.duocuc.evaluacion2.data.model.Prioridad
import cl.duocuc.evaluacion2.data.model.TipoOportunidad
import cl.duocuc.evaluacion2.data.repository.ContactoRepository
import cl.duocuc.evaluacion2.data.repository.EventoRepository
import cl.duocuc.evaluacion2.data.repository.GestionRepository
import cl.duocuc.evaluacion2.data.repository.HistorialRepository
import cl.duocuc.evaluacion2.data.repository.OportunidadRepository
import cl.duocuc.evaluacion2.data.repository.OrganizacionRepository
import cl.duocuc.evaluacion2.data.repository.UsuarioRepository
import cl.duocuc.evaluacion2.data.session.SessionManager
import cl.duocuc.evaluacion2.data.session.UsuarioSesion
import cl.duocuc.evaluacion2.ui.screens.OportunidadDetailScreen
import cl.duocuc.evaluacion2.ui.theme.MaxTheme
import cl.duocuc.evaluacion2.ui.viewmodels.ContactoViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.EventoViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.GestionViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.OrganizacionViewModel
import cl.duocuc.evaluacion2.ui.viewmodels.OportunidadViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OportunidadDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val viewModelStore = ViewModelStore()
    private var database: AppDatabase? = null

    @After
    fun cerrarBaseDeDatos() {
        composeRule.runOnIdle {
            viewModelStore.clear()
        }
        database?.close()
    }

    @Test
    fun muestraDetalleYPermiteIniciarAcciones() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testDatabase = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        database = testDatabase

        val nombreOportunidad = "Oportunidad de prueba UI"
        val oportunidadId = runBlocking {
            testDatabase.opportunityDao().insert(
                OpportunityEntity(
                    nombre = nombreOportunidad,
                    tipo = TipoOportunidad.FERIA_EMPRESARIAL.name,
                    descripcion = "Descripción visible en el detalle",
                    fuente = "Prueba instrumentada",
                    prioridad = Prioridad.ALTA.name,
                    potencialComercial = PotencialComercial.ALTO.name,
                    estado = EstadoGestion.CONTACTO_PENDIENTE.name,
                    responsableId = 1L
                )
            )
        }
        val sessionManager = SessionManager().apply {
            iniciar(
                UsuarioSesion(
                    id = 1L,
                    nombre = "Ejecutivo de prueba",
                    correo = "ejecutivo@example.test",
                    perfil = PerfilUsuario.EJECUTIVO_COMERCIAL
                )
            )
        }
        val oportunidadViewModel = OportunidadViewModel(
            OportunidadRepository(testDatabase),
            sessionManager
        )
        val gestionViewModel = GestionViewModel(
            GestionRepository(testDatabase),
            HistorialRepository(testDatabase),
            UsuarioRepository(testDatabase.usuarioDao()),
            sessionManager
        )
        val organizacionRepository = OrganizacionRepository(testDatabase.organizacionDao())
        val organizacionViewModel = OrganizacionViewModel(organizacionRepository)
        val contactoViewModel = ContactoViewModel(
            ContactoRepository(testDatabase.contactoDao()),
            organizacionRepository
        )
        val eventoViewModel = EventoViewModel(
            EventoRepository(testDatabase.eventoDao()),
            organizacionRepository
        )
        listOf<ViewModel>(
            oportunidadViewModel,
            gestionViewModel,
            organizacionViewModel,
            contactoViewModel,
            eventoViewModel
        ).forEach { viewModel ->
            viewModelStore.put(viewModel::class.java.name, viewModel)
        }

        var registroGestionSolicitado = false
        var activacionSolicitada = false
        var regresoSolicitado = false

        composeRule.setContent {
            MaxTheme {
                OportunidadDetailScreen(
                    id = oportunidadId,
                    viewModel = oportunidadViewModel,
                    gestionViewModel = gestionViewModel,
                    organizacionViewModel = organizacionViewModel,
                    contactoViewModel = contactoViewModel,
                    eventoViewModel = eventoViewModel,
                    onVolver = { regresoSolicitado = true },
                    onEditar = {},
                    onRegistrarGestion = { registroGestionSolicitado = true },
                    onGestionarActivacion = { activacionSolicitada = true },
                    onEliminada = {}
                )
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(nombreOportunidad)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText(nombreOportunidad).assertIsDisplayed()
        composeRule.onNodeWithText("Descripción visible en el detalle").assertIsDisplayed()

        composeRule.onNodeWithText("Registrar gestión")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Gestionar activación")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithContentDescription("Volver").performClick()

        composeRule.runOnIdle {
            assertTrue(registroGestionSolicitado)
            assertTrue(activacionSolicitada)
            assertTrue(regresoSolicitado)
        }
    }
}
