package cl.duocuc.evaluacion2

import cl.duocuc.evaluacion2.data.model.PostResponse
import cl.duocuc.evaluacion2.data.repository.PostRepository
import cl.duocuc.evaluacion2.ui.viewmodels.PostViewModel
import io.mockk.coEvery
import io.mockk.mockk
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import com.google.gson.JsonParseException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun cargaResultadosYRepresentaListaVacia() = runTest {
        val repository = mockk<PostRepository>()
        val posts = listOf(PostResponse(2, 9, "Título", "Texto"))
        coEvery { repository.obtenerPublicaciones() } returns posts
        val viewModel = PostViewModel(repository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.cargando)
        assertEquals(posts, viewModel.uiState.value.publicaciones)
        assertEquals(null, viewModel.uiState.value.mensajeError)

        coEvery { repository.obtenerPublicaciones() } returns emptyList()
        viewModel.cargarPublicaciones()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.publicaciones.isEmpty())
        assertEquals(null, viewModel.uiState.value.mensajeError)
    }

    @Test
    fun mapeaErrorDeRedTimeoutHttpYJsonYPermiteReintentar() = runTest {
        val repository = mockk<PostRepository>()
        coEvery { repository.obtenerPublicaciones() } throws UnknownHostException()
        val viewModel = PostViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.mensajeError.orEmpty().contains("conectar"))

        coEvery { repository.obtenerPublicaciones() } throws SocketTimeoutException()
        viewModel.cargarPublicaciones()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.mensajeError.orEmpty().contains("tiempo de espera"))

        val httpError = HttpException(
            Response.error<List<PostResponse>>(
                502,
                "{}".toResponseBody("application/json".toMediaType())
            )
        )
        coEvery { repository.obtenerPublicaciones() } throws httpError
        viewModel.cargarPublicaciones()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.mensajeError.orEmpty().contains("HTTP 502"))

        coEvery { repository.obtenerPublicaciones() } throws JsonParseException("mal JSON")
        viewModel.cargarPublicaciones()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.mensajeError.orEmpty().contains("JSON inválida"))

        coEvery { repository.obtenerPublicaciones() } returns listOf(PostResponse(2, 9, "Título", "Texto"))
        viewModel.cargarPublicaciones()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.publicaciones.size)
        assertEquals(null, viewModel.uiState.value.mensajeError)
    }
}
