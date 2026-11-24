package cl.duocuc.evaluacion2.data.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow

import cl.duocuc.evaluacion2.data.model.PostRequest
import cl.duocuc.evaluacion2.data.model.PostResponse
import cl.duocuc.evaluacion2.data.remote.AgenciaMaxApiService
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

class PostRepositoryTest : StringSpec({
    "Devuelve las publicaciones recibidas de la api" { PostRepositoryTestCases().devuelveLasPublicacionesRecibidasDeLaApi() }
    "Informa respuestas http fallidas" { PostRepositoryTestCases().informaRespuestasHttpFallidas() }
    "Respuesta exitosa sin cuerpo no se trata como lista vacia" { PostRepositoryTestCases().respuestaExitosaSinCuerpoNoSeTrataComoListaVacia() }
    "Error de red se propaga al view model" { PostRepositoryTestCases().errorDeRedSePropagaAlViewModel() }
    "Crea publicaciones mediante POST" { PostRepositoryTestCases().creaPublicacionesMediantePost() }
    "Actualiza publicaciones mediante PUT" { PostRepositoryTestCases().actualizaPublicacionesMediantePut() }
    "Elimina publicaciones mediante DELETE" { PostRepositoryTestCases().eliminaPublicacionesMedianteDelete() }
})

private class PostRepositoryTestCases {

    fun devuelveLasPublicacionesRecibidasDeLaApi() = runBlocking {
        val publicaciones = listOf(
            PostResponse(userId = 3, id = 12, title = "Título", body = "Contenido")
        )
        val repository = repositoryWith(Response.success(publicaciones))

        (repository.obtenerPublicaciones()) shouldBe (publicaciones)
    }

    fun informaRespuestasHttpFallidas() {
        listOf(404, 503).forEach { code ->
            val response = Response.error<List<PostResponse>>(
                code,
                "{}".toResponseBody("application/json".toMediaType())
            )
            val repository = repositoryWith(response)
            val error = shouldThrow<HttpException> {
                runBlocking { repository.obtenerPublicaciones() }
            }
            (error.code()) shouldBe (code)
        }

        val errorCreacion = FakeAgenciaMaxApiService().apply {
            respuestaCreacion = Response.error(
                400,
                "{}".toResponseBody("application/json".toMediaType())
            )
        }
        val errorPost = shouldThrow<HttpException> {
            runBlocking {
                PostRepository(errorCreacion).crearPublicacion(PostRequest(1, "Título", "Texto"))
            }
        }
        (errorPost.code()) shouldBe (400)

        val errorActualizacion = FakeAgenciaMaxApiService().apply {
            respuestaActualizacion = Response.error(
                503,
                "{}".toResponseBody("application/json".toMediaType())
            )
        }
        val errorPut = shouldThrow<HttpException> {
            runBlocking {
                PostRepository(errorActualizacion)
                    .actualizarPublicacion(1, PostRequest(1, "Título", "Texto"))
            }
        }
        (errorPut.code()) shouldBe (503)
    }

    fun respuestaExitosaSinCuerpoNoSeTrataComoListaVacia() {
        val repository = repositoryWith(Response.success<List<PostResponse>>(null))

        shouldThrow<RespuestaApiVaciaException> {
            runBlocking { repository.obtenerPublicaciones() }
        }
    }

    fun errorDeRedSePropagaAlViewModel() {
        val failure = IOException("sin conexión")
        val api = FakeAgenciaMaxApiService().apply { errorAlObtener = failure }
        val repository = PostRepository(api)

        val error = shouldThrow<IOException> {
            runBlocking { repository.obtenerPublicaciones() }
        }

        (error) shouldBe (failure)
    }

    fun creaPublicacionesMediantePost() = runBlocking {
        val solicitud = PostRequest(userId = 1, title = " Nuevo ", body = " Contenido ")
        val creada = PostResponse(1, 101, "Nuevo", "Contenido")
        val api = FakeAgenciaMaxApiService().apply {
            respuestaCreacion = Response.success(creada)
        }

        (PostRepository(api).crearPublicacion(solicitud)) shouldBe (creada)
        (api.solicitudCreacion) shouldBe (solicitud)
    }

    fun actualizaPublicacionesMediantePut() = runBlocking {
        val solicitud = PostRequest(userId = 2, title = "Actualizada", body = "Nuevo contenido")
        val actualizada = PostResponse(2, 9, "Actualizada", "Nuevo contenido")
        val api = FakeAgenciaMaxApiService().apply {
            respuestaActualizacion = Response.success(actualizada)
        }

        (PostRepository(api).actualizarPublicacion(9, solicitud)) shouldBe (actualizada)
        (api.idActualizacion) shouldBe (9)
        (api.solicitudActualizacion) shouldBe (solicitud)
    }

    fun eliminaPublicacionesMedianteDelete() {
        val api = FakeAgenciaMaxApiService()
        runBlocking { PostRepository(api).eliminarPublicacion(9) }
        (api.idEliminacion) shouldBe (9)

        api.respuestaEliminacion = Response.error(
            503,
            "{}".toResponseBody("application/json".toMediaType())
        )
        val error = shouldThrow<HttpException> {
            runBlocking { PostRepository(api).eliminarPublicacion(9) }
        }
        (error.code()) shouldBe (503)
    }

    private fun repositoryWith(
        response: Response<List<PostResponse>>
    ) = PostRepository(FakeAgenciaMaxApiService().apply { respuestaPublicaciones = response })
}

private class FakeAgenciaMaxApiService : AgenciaMaxApiService {
    var respuestaPublicaciones: Response<List<PostResponse>> = Response.success(emptyList())
    var respuestaCreacion: Response<PostResponse> =
        Response.success(PostResponse(1, 101, "Creada", "Contenido"))
    var respuestaActualizacion: Response<PostResponse> =
        Response.success(PostResponse(1, 1, "Actualizada", "Contenido"))
    var respuestaEliminacion: Response<Unit> = Response.success(Unit)
    var solicitudCreacion: PostRequest? = null
    var solicitudActualizacion: PostRequest? = null
    var idActualizacion: Int? = null
    var idEliminacion: Int? = null
    var errorAlObtener: IOException? = null

    override suspend fun obtenerPublicaciones(): Response<List<PostResponse>> {
        errorAlObtener?.let { throw it }
        return respuestaPublicaciones
    }

    override suspend fun crearPublicacion(publicacion: PostRequest): Response<PostResponse> {
        solicitudCreacion = publicacion
        return respuestaCreacion
    }

    override suspend fun actualizarPublicacion(
        id: Int,
        publicacion: PostRequest
    ): Response<PostResponse> {
        idActualizacion = id
        solicitudActualizacion = publicacion
        return respuestaActualizacion
    }

    override suspend fun eliminarPublicacion(id: Int): Response<Unit> {
        idEliminacion = id
        return respuestaEliminacion
    }
}
