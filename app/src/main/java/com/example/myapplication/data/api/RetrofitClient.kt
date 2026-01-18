package com.example.myapplication.data.api

import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Production backend
    private const val BASE_URL = "https://hamarjodi-backend-trwgz.ondigitalocean.app/"

//     Local backend
//    private const val BASE_URL = "http://10.0.2.2:8080/"

//     development backend
//    private const val BASE_URL = "https://shaadi-backend-development.onrender.com/"
    val gson = Gson()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    val webSocketManager: WebSocketManager by lazy {
        // Convert HTTPS to WSS for WebSocket connection
        val wsUrl = BASE_URL.replace("https://", "wss://").removeSuffix("/")
        WebSocketManager(wsUrl, gson)
    }
}
