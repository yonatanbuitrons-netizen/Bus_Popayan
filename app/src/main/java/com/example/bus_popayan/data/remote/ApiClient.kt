package com.example.bus_popayan.data.remote

import com.example.bus_popayan.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // La dirección del servidor se configura en Config.SERVER_URL.
    private val BASE_URL = Config.SERVER_URL.trimEnd('/') + "/api/"

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
