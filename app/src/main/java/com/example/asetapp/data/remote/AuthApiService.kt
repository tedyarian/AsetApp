package com.example.asetapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val role: String? = null
)

data class LoginResponse(
    val success: Boolean,
    val message: String? = null,
    val token: String? = null,
    val user: UserDto? = null
)

interface AuthApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Header("Authorization") bearerToken: String): Response<Unit>
}