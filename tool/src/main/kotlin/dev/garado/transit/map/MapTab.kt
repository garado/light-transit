package dev.garado.transit.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MapTabContent(isDarkTheme: Boolean, database: TileCacheDatabase) {
    TransitMapView(isDarkTheme = isDarkTheme, database = database, modifier = Modifier.fillMaxSize())
}
