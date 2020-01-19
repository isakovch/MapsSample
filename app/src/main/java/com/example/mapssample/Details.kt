package com.example.mapssample

import com.google.gson.annotations.SerializedName

data class Details(
    @SerializedName("result")
    val result: DetailsResult
)

data class DetailsResult(
    @SerializedName("reference")
    val reference: String,
    @SerializedName("place_id")
    val placeId: String
)