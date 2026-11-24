package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.model.PostResponse
import cl.duocuc.evaluacion2.data.remote.AgenciaMaxApiService
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class PostRepositoryTest {

    @Test
    fun devuelveLasPublicacionesRecibidasDeLaApi() = runBlocking {
        val publicaciones = listOf(
            PostResponse(userId = 3, id = 12, title = "Título", body = "Contenido")
        )
        val repository = repositoryWith(Response.success(publicaciones))

        assertEquals(publicaciones, repository.obtenerPublicaciones())
    }

    @Test
    fun informaRespuestasHttpFallidas() {
        listOf(404, 503).forEach { code ->
            val response = Response.error<List<PostResponse>>(
                code,
                "{}".toResponseBody("application/json".toMediaType())
            )
            val repository = repositoryWith(response)
            val error = assertThrows(HttpException::class.java) {
                runBlocking { repository.obtenerPublicaciones() }
            }
            assertEquals(code, error.code())
        }
    }

    @Test
    fun respuestaExitosaSinCuerpoNoSeTrataComoListaVacia() {
        val repository = repositoryWith(Response.success<List<PostResponse>>(null))

        assertThrows(RespuestaApiVaciaException::class.java) {
            runBlocking { repository.obtenerPublicaciones() }
        }
    }

    @Test
    fun errorDeRedSePropagaAlViewModel() {
        val failure = IOException("sin conexión")
        val repository = PostRepository(
            api = object : AgenciaMaxApiService {
                override suspend fun obtenerPublicaciones(): Response<List<PostResponse>> {
                    throw failure
                }
            }
        )

        val error = assertThrows(IOException::class.java) {
            runBlocking { repository.obtenerPublicaciones() }
        }

        assertEquals(failure, error)
    }

    private fun repositoryWith(
        response: Response<List<PostResponse>>
    ) = PostRepository(
        api = object : AgenciaMaxApiService {
            override suspend fun obtenerPublicaciones() = response
        }
    )
}
