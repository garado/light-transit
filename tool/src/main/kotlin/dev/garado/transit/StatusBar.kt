package dev.garado.transit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.lightClickable
import dev.garado.transit.formatClockTime

/** Status bar with system information (battery, clock) + optional cancel button */
@Composable
fun StatusBar(modifier: Modifier = Modifier, onCancel: (() -> Unit)? = null) {
    val currentTimeSeconds by MinuteTimer.currentTimeSeconds.collectAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(LightThemeTokens.colors.background)
            .padding(horizontal = 16.dp),
    ) {
        LightIcon(
            icon = LightIcons.CLOSE,
            size = 1.2f,
            modifier = Modifier
                .alpha(if (onCancel != null) 1f else 0f)
                .lightClickable(enabled = onCancel != null, onClick = { onCancel?.invoke() }),
        )
        LightText(
            text = formatClockTime(currentTimeSeconds),
            variant = LightTextVariant.Superfine,
            align = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        // LightIcon(icon = LightIcons.BATTERY_FULL, size = 1f) // TODO: real battery level
    }
}
