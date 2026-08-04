package dev.garado.transit.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.lightClickable
import dev.garado.transit.api.nominatim.NominatimClient
import dev.garado.transit.api.nominatim.toDisplayLine

// TODO: dummy origin bias until the "From" location carries real coordinates
private const val DUMMY_ORIGIN_LAT = 37.8044
private const val DUMMY_ORIGIN_LON = -122.2712

class LocationResultsScreen(
    sealedActivity: SealedLightActivity,
    private val query: String,
) : SimpleLightScreen<LocationResult>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val results by produceState<List<LocationResult>?>(initialValue = null, query) {
            value = NominatimClient.search(
                query = query,
                originLat = DUMMY_ORIGIN_LAT,
                originLon = DUMMY_ORIGIN_LON,
            ).mapNotNull { result ->
                val lat = result.lat.toDoubleOrNull() ?: return@mapNotNull null
                val lon = result.lon.toDoubleOrNull() ?: return@mapNotNull null
                val title = result.displayName.substringBefore(",")
                val address = result.address?.toDisplayLine() ?: result.displayName.substringAfter(", ")
                LocationResult(title = title, address = address, lat = lat, lon = lon)
            }
        }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(
                        icon = LightIcons.BACK,
                        onClick = { goBack() },
                    ),
                    center = LightTopBarCenter.Text("Results"),
                )

                val currentResults = results
                when {
                    currentResults == null -> StatusMessage("Searching...")
                    currentResults.isEmpty() -> StatusMessage("No results found")
                    else -> LightScrollView(modifier = Modifier.padding(horizontal = 32.dp)) {
                        currentResults.forEach { result ->
                            LocationResultRow(result = result, onClick = { goBack(result) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        LightText(text = text, variant = LightTextVariant.Copy)
    }
}

@Composable
private fun LocationResultRow(result: LocationResult, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        LightText(text = result.title, variant = LightTextVariant.Copy)
        if (result.address.isNotBlank()) {
            LightText(
                text = result.address,
                variant = LightTextVariant.Detail,
                lighten = true,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
