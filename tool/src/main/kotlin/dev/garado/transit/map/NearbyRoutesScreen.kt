package dev.garado.transit.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
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
import dev.garado.transit.StatusBar
import dev.garado.transit.api.models.TripRoute
import dev.garado.transit.parseHexColor

class NearbyRoutesScreen(sealedActivity: SealedLightActivity) : LightScreen<Unit, NearbyRoutesViewModel>(sealedActivity) {

    override val viewModelClass = NearbyRoutesViewModel::class.java
    override fun createViewModel() = NearbyRoutesViewModel(lightContext)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val routes by viewModel.routes.collectAsState()
        val hasSearched by viewModel.hasSearched.collectAsState()
        val isSearching by viewModel.isSearching.collectAsState()

        LaunchedEffect(Unit) { viewModel.search() }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                StatusBar()
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, sizeUnits = 1.5f, onClick = { goBack() }),
                    center = LightTopBarCenter.Text("Nearby Routes"),
                )

                when {
                    isSearching -> CenteredMessage("Searching...", modifier = Modifier.weight(1f))
                    !hasSearched -> CenteredMessage("Searching...", modifier = Modifier.weight(1f))
                    routes.isEmpty() -> CenteredMessage("No nearby routes found", modifier = Modifier.weight(1f))
                    else -> LightScrollView(modifier = Modifier.weight(1f)) {
                        routes.forEach { route -> RouteRow(route) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        LightText(text = text, variant = LightTextVariant.Paragraph)
    }
}

@Composable
private fun RouteRow(route: TripRoute) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp, start = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = parseHexColor(route.color, fallback = LightThemeTokens.colors.content),
                    shape = RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            LightText(
                text = route.name,
                variant = LightTextVariant.Detail,
                color = parseHexColor(route.textColor, fallback = LightThemeTokens.colors.background),
            )
        }
        if (route.longName != null) {
            LightText(
                text = route.longName,
                variant = LightTextVariant.Paragraph,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
