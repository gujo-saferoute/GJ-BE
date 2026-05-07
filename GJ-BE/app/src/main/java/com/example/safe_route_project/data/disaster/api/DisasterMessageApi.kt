package com.example.safe_route_project.data.disaster.api

import com.example.safe_route_project.data.disaster.model.DisasterMessageResponse
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface DisasterMessageApi {

    @GET
    suspend fun getDisasterMessages(
        @Url url: String,
        @QueryMap(encoded = true) query: Map<String, String>,
        @HeaderMap headers: Map<String, String>
    ): DisasterMessageResponse
}