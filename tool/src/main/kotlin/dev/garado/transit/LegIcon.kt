package dev.garado.transit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import dev.garado.transit.api.models.TripLeg

/**
 * Left-side icon/badge for a leg (pedestrian icon for walk, colored route badge for transit).
 * Pass [minWidth] to reserve the same width across every leg (used by the navigation screen's
 * list, where descriptions need to line up); left at its default, sizing is purely content-based
 * (used by [dev.garado.transit.route.TripLegsRow]'s chip strip).
 */
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
