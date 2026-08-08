package dev.garado.transit.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.lightClickable
import dev.garado.transit.api.models.TripLeg
import dev.garado.transit.formatClockTime
import dev.garado.transit.parseHexColor

/**
 * Information on a single transit leg
 *
 * Shows:
 * - boarding stop + time
 * - an expandable "N stops before..." summary of the stops in between
 * - alighting stop + time
 */
@Composable
fun TransitLegDetail(leg: TripLeg.Transit) {
    var expanded by remember(leg) { mutableStateOf(false) }
    val boardingStop = leg.stops.firstOrNull()
    val alightingStop = leg.stops.lastOrNull()
    val intermediateStops = leg.stops.drop(1).dropLast(1)

    Column {
        RouteBadge(leg = leg, modifier = Modifier.padding(bottom = 8.dp))

        // first stop
        StopHeaderRow(name = boardingStop?.name ?: leg.routeName, time = formatClockTime(leg.startTime))

        if (intermediateStops.isNotEmpty()) {
            Row(modifier = Modifier.height(IntrinsicSize.Min).padding(start = 4.dp)) {
                // left "border" next to expandable section
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp) // line inset
                        .background(LightThemeTokens.colors.contentSecondary),
                )

                // expander button
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .lightClickable(onClick = { expanded = !expanded })
                            .padding(vertical = 8.dp)
                    ) {
                        LightIcon(
                            icon = if (expanded) LightIcons.UP else LightIcons.DOWN,
                            size = 1f,
                            modifier = Modifier.offset(x = (-4).dp, y = if (expanded) 1.5.dp else (-1.5).dp),
                        )
                        LightText(
                            text = "${intermediateStops.size} stops before...",
                            variant = LightTextVariant.Detail,
                            modifier = Modifier.padding(start = 2.dp),
                        )
                    }

                    // display all intermediate stops
                    if (expanded) {
                        intermediateStops.forEach { stop ->
                            LightText(
                                text = stop.name,
                                variant = LightTextVariant.Detail,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        // final stop
        if (alightingStop != null) {
            StopHeaderRow(name = alightingStop.name, time = formatClockTime(leg.endTime))
        }
    }
}

@Composable
private fun RouteBadge(leg: TripLeg.Transit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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

@Composable
private fun StopHeaderRow(name: String, time: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        LightText(
            text = name,
            variant = LightTextVariant.Paragraph,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        LightText(text = time, variant = LightTextVariant.Paragraph, maxLines = 1)
    }
}
