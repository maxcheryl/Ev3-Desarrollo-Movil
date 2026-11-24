package cl.duocuc.evaluacion2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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
import cl.duocuc.evaluacion2.data.repository.OportunidadRepository
import cl.duocuc.evaluacion2.data.session.SessionManager
import cl.duocuc.evaluacion2.data.session.UsuarioSesion
import cl.duocuc.evaluacion2.ui.screens.OportunidadListScreen
import cl.duocuc.evaluacion2.ui.theme.MaxTheme
import cl.duocuc.evaluacion2.ui.viewmodels.OportunidadViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OportunidadListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val viewModelStore = ViewModelStore()
    private var database: AppDatabase? = null

    @After
    fun cerrarBaseDeDatos() {
        composeRule.runOnIdle { viewModelStore.clear() }
        database?.close()
    }

    @Test
    fun borrarFiltrosLimpiaBusquedaYRestableceResultados() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testDatabase = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        database = testDatabase
        val oportunidadNombre = "Oportunidad para probar filtros"
        runBlocking {
            testDatabase.opportunityDao().insert(
                OpportunityEntity(
                    nombre = oportunidadNombre,
                    tipo = TipoOportunidad.FERIA_EMPRESARIAL.name,
                    descripcion = "Actividad comercial de prueba.",
                    fuente = "Prueba",
                    prioridad = Prioridad.MEDIA.name,
                    potencialComercial = PotencialComercial.MEDIO.name,
                    estado = EstadoGestion.EVENTO_IDENTIFICADO.name,
                    responsableId = 1L
                )
            )
        }
        val viewModel = OportunidadViewModel(
            OportunidadRepository(testDatabase),
            SessionManager().apply {
                iniciar(
                    UsuarioSesion(
                        id = 2L,
                        nombre = "Supervisor",
                        correo = "supervisor@example.test",
                        perfil = PerfilUsuario.SUPERVISOR_COMERCIAL
                    )
                )
            }
        )
        viewModelStore.put(viewModel::class.java.name, viewModel)

        composeRule.setContent {
            MaxTheme {
                OportunidadListScreen(
                    viewModel = viewModel,
                    onVolver = {},
                    onNueva = {},
                    onEscanear = {},
                    onSeleccionar = {},
                    usuarios = mapOf(1L to "Ejecutivo")
                )
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(oportunidadNombre)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Borrar filtros")
            .performScrollTo()
            .assertIsNotEnabled()
        composeRule.onNodeWithText("Buscar oportunidades")
            .performScrollTo()
            .performTextInput("sin coincidencias")
        composeRule.onNodeWithText(
            "No hay oportunidades que coincidan con la búsqueda y los filtros."
        ).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Borrar filtros").performScrollTo().performClick()
        composeRule.onNodeWithText(oportunidadNombre).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Borrar filtros")
            .performScrollTo()
            .assertIsNotEnabled()

        composeRule.runOnIdle {
            assertTrue(viewModel.oportunidades.value.any { it.nombre == oportunidadNombre })
        }
    }
}
