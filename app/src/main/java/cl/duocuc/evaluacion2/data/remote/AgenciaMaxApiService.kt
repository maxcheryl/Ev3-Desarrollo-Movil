package cl.duocuc.evaluacion2.data.remote

import cl.duocuc.evaluacion2.data.model.PostRequest
import cl.duocuc.evaluacion2.data.model.PostResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AgenciaMaxApiService {
    @GET("/posts")
    suspend fun obtenerPublicaciones(): Response<List<PostResponse>>

    @POST("/posts")
    suspend fun crearPublicacion(
        @Body publicacion: PostRequest
    ): Response<PostResponse>

    @PUT("/posts/{id}")
    suspend fun actualizarPublicacion(
        @Path("id") id: Int,
        @Body publicacion: PostRequest
    ): Response<PostResponse>

    @DELETE("/posts/{id}")
    suspend fun eliminarPublicacion(
        @Path("id") id: Int
    ): Response<Unit>
}
