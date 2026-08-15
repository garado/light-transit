package dev.garado.transit.view.map

import dev.garado.transit.map.RasterTileSource
import dev.garado.transit.map.TileCacheDatabase
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun MapTabContent(isDarkTheme: Boolean, database: TileCacheDatabase) {
    val tileSource = remember(database) { RasterTileSource(database) }
    DisposableEffect(tileSource) {
        onDispose { tileSource.close() }
    }

    TransitMapView(isDarkTheme = isDarkTheme, tileSource = tileSource, modifier = Modifier.fillMaxSize())
}
