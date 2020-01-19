package com.example.mapssample

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface Api {

    @GET("directions/json")
    fun getPolyline(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("sensor") isSensorEnabled: Boolean,
        @Query("mode") mode: String,
        @Query("key") key: String
    ): Single<Direction>

    @GET("geocode/json")
    fun getGeocode(
        @Query("key") key: String,
        @Query("latlng") latlng: String,
        @Query("sensor") isSensorEnabled: Boolean = false
    ): Single<Geocode>
}