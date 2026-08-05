package dev.garado.transit.search

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextField
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

private val SWAP_ICON_GAP = 2.dp

@Composable
fun SearchTabContent(
    fromLocation: String,
    toLocation: String,
    onFromClick: () -> Unit,
    onToClick: () -> Unit,
    onSwapLocations: () -> Unit,
    onStartClick: () -> Unit,
) {
    val swapIconSize = 1.5f.gridUnitsAsDp()

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        LightTextField(
            label = "From:",
            value = fromLocation,
            placeholder = "Search Origin",
            onClick = onFromClick,
            modifier = Modifier.weight(1f),
        )
        LightIcon(
            icon = LightIcons.REVERSE_ORDER,
            contentDescription = "Swap origin and destination",
            modifier = Modifier
                .padding(start = SWAP_ICON_GAP)
                .lightClickable(onClick = onSwapLocations),
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
    ) {
        LightTextField(
            label = "To:",
            value = toLocation,
            placeholder = "Search Destination",
            onClick = onToClick,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.padding(start = SWAP_ICON_GAP).size(swapIconSize))
    }

    if (fromLocation.isNotBlank() && toLocation.isNotBlank()) {
        LightText(
            text = "START",
            variant = LightTextVariant.Button,
            align = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .lightClickable(onClick = onStartClick),
        )
    }
}
