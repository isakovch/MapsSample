package com.example.mapssample

import com.google.gson.annotations.SerializedName

data class Direction(
    val routes: List<Route>
)

data class Route(
    @SerializedName("legs")
    val legs: List<Leg>,
    @SerializedName("overview_polyline")
    val polyline: Point
)

data class Leg(
    @SerializedName("distance")
    val distance: Distance
)

data class Distance(
    @SerializedName("text")
    val text: String
)

data class Point(
    @SerializedName("points")
    val points: String
)