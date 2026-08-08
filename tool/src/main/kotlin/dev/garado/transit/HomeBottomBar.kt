package dev.garado.transit

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightThemeTokens

@Composable
fun HomeBottomBar(
    showStart: Boolean,
    onSettingsClick: () -> Unit,
    onStartClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    LightBottomBar(
        modifier = Modifier.background(LightThemeTokens.colors.background),
        items = listOf(
            LightBarButton.LightIcon(
                icon = LightIcons.SETTINGS,
                contentDescription = "Settings",
                onClick = onSettingsClick,
            ),
            if (showStart) LightBarButton.Text(text = "START", onClick = onStartClick) else null,
            LightBarButton.LightIcon(
                icon = LightIcons.ELLIPSES,
                contentDescription = "Map Menu",
                onClick = onMenuClick,
            ),
        ),
    )
}
