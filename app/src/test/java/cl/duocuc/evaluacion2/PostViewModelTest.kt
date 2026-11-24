package cl.duocuc.evaluacion2

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

import cl.duocuc.evaluacion2.data.model.PostResponse
import cl.duocuc.evaluacion2.data.model.PostRequest
import cl.duocuc.evaluacion2.data.repository.PostRepository
import cl.duocuc.evaluacion2.ui.viewmodels.PostViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import retrofit2.HttpException
import retrofit2.Response
import com.google.gson.JsonParseException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModelTest : StringSpec({
    val dispatcher = StandardTestDispatcher()

    beforeSpec { Dispatchers.setMain(dispatcher) }
    afterSpec { Dispatchers.resetMain() }

    "Carga resultados y representa lista vacia" { PostViewModelTestCases().cargaResultadosYRepresentaListaVacia() }
    "Mapea error de red timeout http y json y permite reintentar" { PostViewModelTestCases().mapeaErrorDeRedTimeoutHttpYJsonYPermiteReintentar() }
    "Integra POST PUT y DELETE con el estado de pantalla" { PostViewModelTestCases().integraPostPutYDeleteConElEstadoDePantalla() }
    "Informa campos obligatorios al crear publicaciones" { PostViewModelTestCases().informaCamposObligatoriosAlCrearPublicaciones() }
    "Crea evento con sus campos y valida su identificador" { PostViewModelTestCases().creaEventoConSusCamposYValidaSuIdentificador() }
    "Informa errores HTTP al ejecutar mutaciones" { PostViewModelTestCases().informaErroresHttpAlEjecutarMutaciones() }
})

@OptIn(ExperimentalCoroutinesApi::class)
private class PostViewModelTestCases {


    fun cargaResultadosYRepresentaListaVacia() = runTest {
        val repository = mockk<PostRepository>()
        val posts = listOf(PostResponse(2, 9, "Título", "Texto"))
        coEvery { repository.obtenerPublicaciones() } returns posts
        val viewModel = PostViewModel(repository)
        advanceUntilIdle()

        (viewModel.uiState.value.cargando) shouldBe false
        (viewModel.uiState.value.publicaciones) shouldBe (posts)
        (viewModel.uiState.value.mensajeError) shouldBe (null)

        coEvery { repository.obtenerPublicaciones() } returns emptyList()
        viewModel.cargarPublicaciones()
        advanceUntilIdle()
        (viewModel.uiState.value.publicaciones.isEmpty()) shouldBe true
        (viewModel.uiState.value.mensajeError) shouldBe (null)
    }

    fun mapeaErrorDeRedTimeoutHttpYJsonYPermiteReintentar() = runTest {
        val repository = mockk<PostRepository>()
        coEvery { repository.obtenerPublicaciones() } throws UnknownHostException()
        val viewModel = PostViewModel(repository)
        advanceUntilIdle()
        (viewModel.uiState.value.mensajeError.orEmpty().contains("conectar")) shouldBe true

        coEvery { repository.obtenerPublicaciones() } throws SocketTimeoutException()
        viewModel.cargarPublicaciones()
        advanceUntilIdle()
        (viewModel.uiState.value.mensajeError.orEmpty().contains("tiempo de espera")) shouldBe true

        val httpError = HttpException(
            Response.error<List<PostResponse>>(
                502,
                "{}".toResponseBody("application/json".toMediaType())
            )
        )
        coEvery { repository.obtenerPublicaciones() } throws httpError
        viewModel.cargarPublicaciones()
        advanceUntilIdle()
        (viewModel.uiState.value.mensajeError.orEmpty().contains("HTTP 502")) shouldBe true

        coEvery { repository.obtenerPublicaciones() } throws JsonParseException("mal JSON")
        viewModel.cargarPublicaciones()
        advanceUntilIdle()
        (viewModel.uiState.value.mensajeError.orEmpty().contains("JSON inválida")) shouldBe true

        coEvery { repository.obtenerPublicaciones() } returns listOf(PostResponse(2, 9, "Título", "Texto"))
        viewModel.cargarPublicaciones()
        advanceUntilIdle()
        (viewModel.uiState.value.publicaciones.size) shouldBe (1)
        (viewModel.uiState.value.mensajeError) shouldBe (null)
    }

    fun integraPostPutYDeleteConElEstadoDePantalla() = runTest {
        val repository = mockk<PostRepository>()
        val original = PostResponse(1, 7, "Original", "Contenido original")
        val creada = PostResponse(1, 101, "Nueva", "Contenido nuevo")
        val actualizada = PostResponse(1, 7, "Actualizada", "Contenido editado")
        coEvery { repository.obtenerPublicaciones() } returns listOf(original)
        coEvery { repository.crearPublicacion(any()) } returns creada
        coEvery { repository.actualizarPublicacion(any(), any()) } returns actualizada
        coEvery { repository.eliminarPublicacion(any()) } returns Unit

        val viewModel = PostViewModel(repository)
        advanceUntilIdle()

        viewModel.crearPublicacion(" Nueva ", " Contenido nuevo ")
        advanceUntilIdle()
        (viewModel.uiState.value.publicaciones) shouldBe (listOf(creada, original))
        (viewModel.uiState.value.mensajeMutacion.orEmpty().startsWith("POST exitoso")) shouldBe true
        coVerify {
            repository.crearPublicacion(PostRequest(1, "Nueva", "Contenido nuevo"))
        }

        viewModel.actualizarPublicacion(actualizada, "Actualizada", "Contenido editado")
        advanceUntilIdle()
        (viewModel.uiState.value.publicaciones) shouldBe (listOf(creada, actualizada))
        (viewModel.uiState.value.mensajeMutacion.orEmpty().startsWith("PUT exitoso")) shouldBe true
        coVerify {
            repository.actualizarPublicacion(
                7,
                PostRequest(1, "Actualizada", "Contenido editado")
            )
        }

        viewModel.eliminarPublicacion(7)
        advanceUntilIdle()
        (viewModel.uiState.value.publicaciones) shouldBe (listOf(creada))
        (viewModel.uiState.value.mensajeMutacion.orEmpty().startsWith("DELETE exitoso")) shouldBe true
        coVerify { repository.eliminarPublicacion(7) }
    }

    fun informaCamposObligatoriosAlCrearPublicaciones() = runTest {
        val repository = mockk<PostRepository>()
        coEvery { repository.obtenerPublicaciones() } returns emptyList()

        val viewModel = PostViewModel(repository)
        advanceUntilIdle()

        viewModel.crearPublicacion(" ", "Contenido")
        (viewModel.uiState.value.mutacionFallida) shouldBe true
        (viewModel.uiState.value.mensajeMutacion.orEmpty().contains("obligatorios")) shouldBe true
        (viewModel.uiState.value.erroresPublicacion.titulo) shouldBe
            ("El título es obligatorio.")
        coVerify(exactly = 0) { repository.crearPublicacion(any()) }
    }

    fun creaEventoConSusCamposYValidaSuIdentificador() = runTest {
        val repository = mockk<PostRepository>()
        coEvery { repository.obtenerPublicaciones() } returns emptyList()
        coEvery { repository.crearPublicacion(any()) } returns
            PostResponse(1, 101, "Feria", "Fecha: 2025-11-18 · Ubicación: Providencia · Organización: Agencia")

        val viewModel = PostViewModel(repository)
        advanceUntilIdle()

        viewModel.crearEvento(
            idEvento = "42",
            nombreEvento = " Feria ",
            fecha = " 2025-11-18 ",
            ubicacion = " Providencia ",
            organizacion = " Agencia "
        )
        advanceUntilIdle()

        coVerify {
            repository.crearPublicacion(
                PostRequest(
                    userId = 1,
                    title = "Feria",
                    body = "Fecha: 2025-11-18 · Ubicación: Providencia · Organización: Agencia",
                    eventId = 42,
                    eventName = "Feria",
                    date = "2025-11-18",
                    location = "Providencia",
                    organization = "Agencia"
                )
            )
        }
        (viewModel.uiState.value.mutacionFallida) shouldBe false

        viewModel.crearEvento("abc", "Feria", "2025-11-18", "Providencia", "Agencia")
        advanceUntilIdle()
        (viewModel.uiState.value.mutacionFallida) shouldBe true
        coVerify(exactly = 1) { repository.crearPublicacion(any()) }

        viewModel.crearEvento("42", "Feria", "2025-02-30", "Providencia", "Agencia")
        (viewModel.uiState.value.erroresEvento.fecha) shouldBe
            ("Usa una fecha real con formato AAAA-MM-DD.")
        coVerify(exactly = 1) { repository.crearPublicacion(any()) }
    }

    fun informaErroresHttpAlEjecutarMutaciones() = runTest {
        val repository = mockk<PostRepository>()
        coEvery { repository.obtenerPublicaciones() } returns emptyList()
        coEvery { repository.crearPublicacion(any()) } throws HttpException(
            Response.error<PostResponse>(
                503,
                "{}".toResponseBody("application/json".toMediaType())
            )
        )

        val viewModel = PostViewModel(repository)
        advanceUntilIdle()

        viewModel.crearPublicacion("Título", "Contenido")
        advanceUntilIdle()

        (viewModel.uiState.value.mutacionFallida) shouldBe true
        (viewModel.uiState.value.mutacionEnCurso) shouldBe false
        (viewModel.uiState.value.mensajeMutacion.orEmpty().contains("HTTP 503")) shouldBe true
    }
}
