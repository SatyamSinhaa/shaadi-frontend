package com.example.myapplication.data.api

import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Changed to localhost as requested. Note: On standard Android Emulator, use 10.0.2.2 instead of localhost.
    private const val BASE_URL = "http://10.0.2.2:8080/"

    val gson = Gson()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
