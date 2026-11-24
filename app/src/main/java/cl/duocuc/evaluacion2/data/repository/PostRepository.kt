package cl.duocuc.evaluacion2.data.repository

import cl.duocuc.evaluacion2.data.model.PostResponse
import cl.duocuc.evaluacion2.data.remote.AgenciaMaxApiService
import cl.duocuc.evaluacion2.data.remote.RetrofitInstance
import retrofit2.HttpException

class PostRepository(
    private val api: AgenciaMaxApiService = RetrofitInstance.api
) {
    suspend fun obtenerPublicaciones(): List<PostResponse> {
        val response = api.obtenerPublicaciones()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        return response.body() ?: throw RespuestaApiVaciaException()
    }
}

class RespuestaApiVaciaException : Exception()
