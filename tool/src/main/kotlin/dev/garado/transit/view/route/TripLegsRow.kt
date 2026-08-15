package dev.garado.transit.view.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import dev.garado.transit.view.home.LegIcon
import dev.garado.transit.api.models.TripLeg
import dev.garado.transit.formatDuration

@Composable
fun TripLegsRow(legs: List<TripLeg>, modifier: Modifier = Modifier) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        legs.forEachIndexed { index, leg ->
            if (index > 0) {
                LightIcon(
                  icon = LightIcons.ARROW_RIGHT,size = 1f, modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            LegChip(leg = leg, modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

@Composable
private fun LegChip(leg: TripLeg, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        LegIcon(leg = leg)
        LightText(
            text = formatDuration(leg.duration),
            variant = LightTextVariant.Detail,
            lighten = true,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
