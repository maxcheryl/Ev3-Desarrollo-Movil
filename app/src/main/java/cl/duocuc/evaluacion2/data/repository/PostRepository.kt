package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.model.PostRequest
import cl.duocuc.evaluacion2.data.model.PostResponse
import cl.duocuc.evaluacion2.data.remote.AgenciaMaxApiService
import cl.duocuc.evaluacion2.data.remote.RetrofitInstance
import retrofit2.HttpException
import retrofit2.Response

class PostRepository(
    private val api: AgenciaMaxApiService = RetrofitInstance.api
) {
    suspend fun obtenerPublicaciones(): List<PostResponse> {
        return api.obtenerPublicaciones().requireBody()
    }

    suspend fun crearPublicacion(publicacion: PostRequest): PostResponse =
        api.crearPublicacion(publicacion).requireBody()

    suspend fun actualizarPublicacion(id: Int, publicacion: PostRequest): PostResponse =
        api.actualizarPublicacion(id, publicacion).requireBody()

    suspend fun eliminarPublicacion(id: Int) {
        val response = api.eliminarPublicacion(id)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }
}

class RespuestaApiVaciaException : Exception()

private fun <T : Any> Response<T>.requireBody(): T {
    if (!isSuccessful) {
        throw HttpException(this)
    }
    return body() ?: throw RespuestaApiVaciaException()
}
