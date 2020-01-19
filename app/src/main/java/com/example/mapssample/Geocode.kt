package com.example.mapssample

import com.google.gson.annotations.SerializedName

data class Geocode(
    @SerializedName("results")
    val results: List<Result>
)

data class Result(
    @SerializedName("place_id")
    val placeId: String
)