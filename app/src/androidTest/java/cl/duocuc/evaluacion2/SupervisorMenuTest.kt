package cl.duocuc.evaluacion2

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.duocuc.evaluacion2.ui.screens.MenuScreen
import cl.duocuc.evaluacion2.ui.theme.MaxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SupervisorMenuTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun supervisorSoloVeElAccesoAOportunidades() {
        composeRule.setContent {
            MaxTheme {
                MenuScreen(
                    nombre = "Supervisora",
                    perfil = "Supervisor comercial",
                    esSupervisor = true,
                    onOportunidades = {},
                    onCatalogos = {},
                    onCatalogoEventosExternos = {},
                    onEscanearQr = {},
                    onUbicacion = {},
                    onCerrarSesion = {}
                )
            }
        }

        composeRule.onNodeWithText("Oportunidades").assertIsDisplayed()
        composeRule.onNodeWithText("Cerrar sesión").assertIsDisplayed()
        composeRule.onAllNodesWithText("Organizaciones y contactos").assertCountEquals(0)
        composeRule.onAllNodesWithText("Catálogo de eventos externos").assertCountEquals(0)
        composeRule.onAllNodesWithText("Escanear código QR").assertCountEquals(0)
        composeRule.onAllNodesWithText("Mi ubicación").assertCountEquals(0)
    }
}
