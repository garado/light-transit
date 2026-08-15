package dev.garado.transit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIconConfiguration
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import dev.garado.transit.api.models.TripLeg

/** Colored route badge showing the name of the route */
@Composable
fun LegIcon(leg: TripLeg, modifier: Modifier = Modifier, minWidth: Dp = Dp.Unspecified) {
    Box(
        modifier = modifier.widthIn(min = minWidth),
        contentAlignment = Alignment.Center,
    ) {
        when (leg) {
            is TripLeg.Walk -> LightIcon(icon = LightIcons.DIRECTIONS_PEDESTRIAN, size = 1.25f)

            is TripLeg.Transit -> Box(
                modifier = Modifier
                    .background(
                        color = parseHexColor(leg.routeColor, fallback = LightThemeTokens.colors.content),
                        shape = RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                LightText(
                    text = leg.routeName,
                    variant = LightTextVariant.Detail,
                    color = parseHexColor(leg.routeTextColor, fallback = LightThemeTokens.colors.background),
                )
            }
        }
    }
}

/** Bus/train/boat/etc icon indicating the trip leg type */
@Composable
fun LegModeIcon(leg: TripLeg, modifier: Modifier = Modifier, width: Dp? = null) {
    Box(
        modifier = if (width != null) modifier.width(width) else modifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        when (leg) {
            is TripLeg.Walk -> LightIcon(
                icon = LightIcons.DIRECTIONS_PEDESTRIAN,
                size = 1.25f,
                modifier = iconOffset(LightIcons.DIRECTIONS_PEDESTRIAN),
            )
            is TripLeg.Transit -> {
                val icon = transitModeIcon(leg.modeName)
                LightIcon(icon = icon, size = 1.25f, modifier = iconOffset(icon))
            }
        }
    }
}

/** Some [LightIcons] glyphs sit slightly off-center within their bounds; nudge them individually. */
private fun iconOffset(icon: LightIconConfiguration): Modifier = when (icon) {
    LightIcons.DIRECTIONS_PEDESTRIAN -> Modifier.offset(x = (0).dp)
    LightIcons.DIRECTIONS_BUS -> Modifier.offset(x = (-2).dp)
    LightIcons.DIRECTIONS_TRAIN -> Modifier.offset(x = (-2).dp)
    LightIcons.DIRECTIONS_FERRY -> Modifier.offset(x = (0).dp)
    else -> Modifier
}

private fun transitModeIcon(modeName: String?) = when {
    modeName == null -> LightIcons.DIRECTIONS_BUS
    modeName.contains("train", ignoreCase = true) ||
        modeName.contains("rail", ignoreCase = true) ||
        modeName.contains("subway", ignoreCase = true) ||
        modeName.contains("metro", ignoreCase = true) ||
        modeName.contains("tram", ignoreCase = true) -> LightIcons.DIRECTIONS_TRAIN
    modeName.contains("ferry", ignoreCase = true) ||
        modeName.contains("boat", ignoreCase = true) -> LightIcons.DIRECTIONS_FERRY
    else -> LightIcons.DIRECTIONS_BUS
}
