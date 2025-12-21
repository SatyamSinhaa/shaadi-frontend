package com.example.myapplication.data.api

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.MediaType.Companion.toMediaTypeOrNull

object SupabaseConfig {
    const val PROJECT_ID = "nuvzcgkstqbouakpdosn"
    const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im51dnpjZ2tzdHFib3Vha3Bkb3NuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjYyOTA3ODcsImV4cCI6MjA4MTg2Njc4N30.SkuQnuAfko3bfwh_EtCbtCJCXtcHsO7oXw7jw3y24gw"
    const val BUCKET_NAME = "photo_gallery"
    
    val supabase = createSupabaseClient(
        supabaseUrl = "https://$PROJECT_ID.supabase.co",
        supabaseKey = ANON_KEY
    ) {
        install(Storage)
    }

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun uploadFile(bytes: ByteArray, fileName: String): String {
        val url = "https://$PROJECT_ID.supabase.co/storage/v1/object/$BUCKET_NAME/$fileName"
        
        val requestBody = okhttp3.RequestBody.create("image/jpeg".toMediaTypeOrNull(), bytes)
        
        val request = okhttp3.Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $ANON_KEY")
            .addHeader("apikey", ANON_KEY)
            .addHeader("Content-Type", "image/jpeg")
            .post(requestBody)
            .build()
            
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw java.io.IOException("Upload failed: ${response.code} ${response.message} - ${response.body?.string()}")
        }
        
        return "https://$PROJECT_ID.supabase.co/storage/v1/object/public/$BUCKET_NAME/$fileName"
    }

    fun deleteFile(path: String) {
        // Extract filename/path from full URL if needed, but the storage API expects the path relative to bucket or absolute object key
        // Assuming path is the full URL, we need to extract the object key after /public/BUCKET_NAME/
        // URL frame: https://PROJECT.supabase.co/storage/v1/object/public/BUCKET/folder/file.jpg
        // API DELETE target: https://PROJECT.supabase.co/storage/v1/object/BUCKET/folder/file.jpg
        
        val objectKey = path.substringAfter("/public/$BUCKET_NAME/")
        val url = "https://$PROJECT_ID.supabase.co/storage/v1/object/$BUCKET_NAME/$objectKey"

        val request = okhttp3.Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $ANON_KEY")
            .addHeader("apikey", ANON_KEY)
            .delete()
            .build()

        val response = client.newCall(request).execute()
        // We log but don't crash if delete fails, as it's cleanup
        if (!response.isSuccessful) {
            // Log warning or throw
        }
    }
}
