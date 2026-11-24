package cl.duocuc.evaluacion2.data.remote

import cl.duocuc.evaluacion2.data.model.PostResponse
import retrofit2.Response
import retrofit2.http.GET

interface AgenciaMaxApiService {
    @GET("posts")
    suspend fun obtenerPublicaciones(): Response<List<PostResponse>>
}
