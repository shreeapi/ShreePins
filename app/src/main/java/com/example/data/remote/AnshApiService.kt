package com.example.data.remote

import com.example.data.remote.dto.ApiResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface AnshApiService {

    @GET("api/pintrest")
    suspend fun getPins(
        @Query("search") search: String,
        @Query("images") images: Int = 50,
        @Query("pages") pages: Int = 1
    ): ApiResponseDto
}
