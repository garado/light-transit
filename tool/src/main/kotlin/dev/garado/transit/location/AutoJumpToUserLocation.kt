package dev.garado.transit.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.map.LatLon
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** If live location dev setting is on, jumps [onLocationFound] to the user's location once it's available */
@Composable
fun AutoJumpToUserLocation(lightContext: SealedLightContext, onLocationFound: (LatLon) -> Unit) {
    val settings = remember { UserLocationSettings(lightContext.dataStore) }
    val liveLocationEnabled by settings.liveLocationEnabled.collectAsState(initial = false)

    LaunchedEffect(liveLocationEnabled) {
        if (!liveLocationEnabled) return@LaunchedEffect
        val provider = UserLocationLogsProvider()
        val readerJob = launch { provider.start() }
        val location = provider.location.filterNotNull().first()
        onLocationFound(LatLon(lat = location.lat, lon = location.lon))
        readerJob.cancel()
    }
}
