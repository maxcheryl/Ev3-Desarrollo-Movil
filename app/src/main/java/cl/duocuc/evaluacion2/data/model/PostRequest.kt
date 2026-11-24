package cl.duocuc.evaluacion2.data.model

data class PostRequest(
    val userId: Int,
    val title: String,
    val body: String,
    val eventId: Int? = null,
    val eventName: String? = null,
    val date: String? = null,
    val location: String? = null,
    val organization: String? = null
)
