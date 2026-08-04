package dev.garado.transit.search

import com.thelightphone.sdk.ui.LightIconConfiguration
import com.thelightphone.sdk.ui.LightIcons

data class SavedLocation(val label: String, val icon: LightIconConfiguration, val result: LocationResult)

val savedLocations = listOf(
    SavedLocation("Home", LightIcons.STAR, LocationResult("Home", lat = 37.8044, lon = -122.2712)),
    SavedLocation("Work", LightIcons.STAR, LocationResult("Work", lat = 37.7749, lon = -122.4194)),
)
