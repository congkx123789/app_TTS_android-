package com.example.texttosound.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

data class SynthesisRequest(
    val text: String,
    val language: String = "vn",
    val speed: Float = 1.0f
)

interface TtsApiService {
    @Streaming
    @POST("api/v1/synthesize")
    suspend fun synthesizeText(@Body request: SynthesisRequest): Response<ResponseBody>
}
