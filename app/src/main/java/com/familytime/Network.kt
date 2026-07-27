package com.familytime

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class Contact(
    val name: String,
    val email: String
)

data class InviteRequest(
    val emails: List<String>
)

data class RoomResponse(
    val url: String?,
    val error: String?
)

interface GasApi {
    @GET("exec")
    suspend fun getContacts(): List<Contact>

    @POST("exec")
    suspend fun startCall(@Body request: InviteRequest): RoomResponse
}

object Network {
    // Note: Google Apps Script Web App URLs require redirects, but Retrofit (OkHttp) handles them automatically.
    // The base URL must end with a trailing slash. We'll use the domain as the base and pass the full path if needed, 
    // or just construct it cleanly.
    private const val BASE_URL = "https://script.google.com/macros/s/AKfycbwBrmH2S9PxuMv0gkvmO8j7yJsMNQoxNliT-LujRjOGm78XF3UuNyiyoS3zU1dvnSYI/"

    val api: GasApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GasApi::class.java)
    }
}
