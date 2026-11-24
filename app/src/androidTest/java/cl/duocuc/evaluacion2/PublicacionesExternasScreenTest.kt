package cl.duocuc.evaluacion2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.duocuc.evaluacion2.data.model.PostRequest
import cl.duocuc.evaluacion2.data.model.PostResponse
import cl.duocuc.evaluacion2.data.remote.AgenciaMaxApiService
import cl.duocuc.evaluacion2.data.repository.PostRepository
import cl.duocuc.evaluacion2.ui.screens.PublicacionesExternasScreen
import cl.duocuc.evaluacion2.ui.theme.MaxTheme
import cl.duocuc.evaluacion2.ui.viewmodels.PostViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class PublicacionesExternasScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun muestraPublicacionYPermiteVolver() {
        val viewModel = PostViewModel(
            PostRepository(
                api = FakeAgenciaMaxApiService(
                    listOf(
                        PostResponse(
                            userId = 2,
                            id = 7,
                            title = "Título de prueba",
                            body = "Contenido de prueba"
                        )
                    )
                )
            )
        )
        var regresoSolicitado = false

        composeRule.setContent {
            MaxTheme {
                PublicacionesExternasScreen(
                    viewModel = viewModel,
                    onVolver = { regresoSolicitado = true }
                )
            }
        }

        composeRule.onNodeWithText("Título de prueba").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Contenido de prueba").assertIsDisplayed()
        composeRule.onNodeWithText("Publicación #7 · Usuario #2").assertIsDisplayed()
        composeRule.onNodeWithText(
            "JSONPlaceholder simula estos datos",
            substring = true
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Volver").performClick()
        composeRule.runOnIdle {
            assertTrue(regresoSolicitado)
        }
    }

    @Test
    fun integraPostPutYDeleteDesdeLaInterfaz() {
        val api = FakeAgenciaMaxApiService(
            listOf(
                PostResponse(
                    userId = 2,
                    id = 7,
                    title = "Publicación existente",
                    body = "Contenido existente"
                )
            )
        )
        val viewModel = PostViewModel(PostRepository(api))

        composeRule.setContent {
            MaxTheme {
                PublicacionesExternasScreen(
                    viewModel = viewModel,
                    onVolver = {}
                )
            }
        }

        composeRule.onNodeWithText("Título").performTextInput("Nueva publicación")
        composeRule.onNodeWithText("Contenido").performTextInput("Contenido nuevo")
        composeRule.onNodeWithText("Enviar POST").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("POST exitoso", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onAllNodesWithText("Editar")[0].performScrollTo().performClick()
        composeRule.onNodeWithText("Editar título").performTextClearance()
        composeRule.onNodeWithText("Editar título").performTextInput("Publicación actualizada")
        composeRule.onNodeWithText("Editar contenido").performTextClearance()
        composeRule.onNodeWithText("Editar contenido").performTextInput("Contenido actualizado")
        composeRule.onNodeWithText("Guardar cambios (PUT)")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("PUT exitoso", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onAllNodesWithText("Eliminar")[0].performScrollTo().performClick()
        composeRule.onNodeWithText("Eliminar (DELETE)").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("DELETE exitoso", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.runOnIdle {
            assertTrue(api.solicitudesCreacion.single().title == "Nueva publicación")
            assertTrue(api.solicitudesActualizacion.single().title == "Publicación actualizada")
            assertTrue(api.idsEliminados.single() == 101)
        }
    }
}

private class FakeAgenciaMaxApiService(
    private val publicaciones: List<PostResponse>
) : AgenciaMaxApiService {

    val solicitudesCreacion = mutableListOf<PostRequest>()
    val solicitudesActualizacion = mutableListOf<PostRequest>()
    val idsEliminados = mutableListOf<Int>()

    override suspend fun obtenerPublicaciones(): Response<List<PostResponse>> =
        Response.success(publicaciones)

    override suspend fun crearPublicacion(publicacion: PostRequest): Response<PostResponse> {
        solicitudesCreacion += publicacion
        return Response.success(
            PostResponse(
                userId = publicacion.userId,
                id = 101,
                title = publicacion.title,
                body = publicacion.body
            )
        )
    }

    override suspend fun actualizarPublicacion(
        id: Int,
        publicacion: PostRequest
    ): Response<PostResponse> {
        solicitudesActualizacion += publicacion
        return Response.success(
            PostResponse(
                userId = publicacion.userId,
                id = id,
                title = publicacion.title,
                body = publicacion.body
            )
        )
    }

    override suspend fun eliminarPublicacion(id: Int): Response<Unit> {
        idsEliminados += id
        return Response.success(Unit)
    }
}
