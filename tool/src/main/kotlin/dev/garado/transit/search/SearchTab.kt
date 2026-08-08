package dev.garado.transit.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightTextField
import com.thelightphone.sdk.ui.lightClickable

private val SWAP_ICON_GAP = 2.dp
private val SWAP_ICON_Y_OFFSET = 12.dp

@Composable
fun SearchTabContent(
    fromLocation: String,
    toLocation: String,
    departureFieldLabel: String,
    departureFieldValue: String,
    onFromClick: () -> Unit,
    onToClick: () -> Unit,
    onSwapLocations: () -> Unit,
    onDepartureTimeClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            LightTextField(
                label = "From:",
                value = fromLocation,
                placeholder = "Search Origin",
                onClick = onFromClick,
                modifier = Modifier.fillMaxWidth(),
            )
            LightTextField(
                label = "To:",
                value = toLocation,
                placeholder = "Search Destination",
                onClick = onToClick,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
        }
        LightIcon(
            icon = LightIcons.REVERSE_ORDER,
            contentDescription = "Swap origin and destination",
            modifier = Modifier
                .padding(start = SWAP_ICON_GAP)
                .offset(y = SWAP_ICON_Y_OFFSET)
                .lightClickable(onClick = onSwapLocations),
        )
    }

    LightTextField(
        label = departureFieldLabel,
        value = departureFieldValue,
        placeholder = "Now",
        onClick = onDepartureTimeClick,
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
    )
}
