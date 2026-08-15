package dev.garado.transit.view.search

import dev.garado.transit.search.LocationResult
import dev.garado.transit.search.SearchCache
import dev.garado.transit.search.SearchCacheDatabase
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.buildDatabase
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
import dev.garado.transit.view.home.StatusBar
import dev.garado.transit.api.nominatim.NominatimClient
import dev.garado.transit.api.nominatim.NominatimResult
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
        val cacheDatabase = remember {
            lightContext.buildDatabase(SearchCacheDatabase::class.java, "search_cache.db")
        }
        DisposableEffect(cacheDatabase) {
            onDispose { cacheDatabase.close() }
        }
        val cache = remember(cacheDatabase) { SearchCache(cacheDatabase) }

        val results by produceState<List<LocationResult>?>(initialValue = null, query) {
            val cached = cache.get(query, DUMMY_ORIGIN_LAT, DUMMY_ORIGIN_LON)
            val nominatimResults = cached ?: NominatimClient.search(
                query = query,
                originLat = DUMMY_ORIGIN_LAT,
                originLon = DUMMY_ORIGIN_LON,
            ).also { cache.put(query, DUMMY_ORIGIN_LAT, DUMMY_ORIGIN_LON, it) }
            value = nominatimResults.mapNotNull(NominatimResult::toLocationResult)
        }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                StatusBar()
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
                    else -> LightScrollView {
                        currentResults.forEach { result ->
                            LocationResultRow(result = result, onClick = { goBack(result) })
                        }
                    }
                }
            }
        }
    }
}

private fun NominatimResult.toLocationResult(): LocationResult? {
    val resultLat = lat.toDoubleOrNull() ?: return null
    val resultLon = lon.toDoubleOrNull() ?: return null
    val title = displayName.substringBefore(",")
    val resultAddress = address?.toDisplayLine() ?: displayName.substringAfter(", ")
    return LocationResult(title = title, address = resultAddress, lat = resultLat, lon = resultLon)
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
            .padding(top = 12.dp, bottom = 12.dp, start = 16.dp),
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
