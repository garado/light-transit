package dev.garado.transit

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightThemeTokens

@Composable
fun HomeBottomBar(onSelectTab: (HomeTab) -> Unit) {
    LightBottomBar(
        modifier = Modifier.background(LightThemeTokens.colors.background),
        items = listOf(
            LightBarButton.LightIcon(
                icon = LightIcons.SEARCH,
                contentDescription = "Search",
                onClick = { onSelectTab(HomeTab.SEARCH) },
            ),
            LightBarButton.LightIcon(
                icon = LightIcons.MAP,
                contentDescription = "Map",
                onClick = { onSelectTab(HomeTab.MAP) },
            ),
            LightBarButton.LightIcon(
                icon = LightIcons.SETTINGS,
                contentDescription = "Settings",
                onClick = { onSelectTab(HomeTab.SETTINGS) },
            ),
        ),
    )
}
