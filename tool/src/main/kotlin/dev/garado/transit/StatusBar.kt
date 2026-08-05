package dev.garado.transit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.lightClickable
import dev.garado.transit.formatClockTime
import kotlinx.coroutines.delay

private const val CLOCK_TICK_MS = 30_000L

/** Status bar with system information (battery, clock) + cancel button */
@Composable
fun StatusBar(onCancel: () -> Unit, modifier: Modifier = Modifier) {
    var currentTimeSeconds by remember { mutableStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeSeconds = System.currentTimeMillis() / 1000
            delay(CLOCK_TICK_MS)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(LightThemeTokens.colors.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        LightIcon(icon = LightIcons.CLOSE, size = 1.2f, modifier = Modifier.lightClickable(onClick = onCancel))
        LightText(
            text = formatClockTime(currentTimeSeconds),
            variant = LightTextVariant.Detail,
            align = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        LightIcon(icon = LightIcons.BATTERY_FULL, size = 1f) // TODO: real battery level
    }
}
