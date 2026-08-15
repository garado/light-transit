package dev.garado.transit.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.lightClickable

private const val MAX_DIGITS = 4
private val KEYPAD_ROWS = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))

private enum class TimeField { HOUR, MINUTE }

@Composable
fun TimePicker(
    modifier: Modifier = Modifier,
    onTimeChanged: (hour24: Int, minute: Int) -> Unit = { _, _ -> },
    onConfirm: () -> Unit = {},
) {
    var digits by remember { mutableStateOf("") }
    var isPm by remember { mutableStateOf(true) }

    val (hour12, minute) = digits.parseHourMinute()
    val hour24 = to24Hour(hour12, isPm)

    LaunchedEffect(digits, isPm) {
        onTimeChanged(hour24, minute)
    }

    fun onDigit(digit: Int) {
        val candidate = (digits + digit).takeLast(MAX_DIGITS)
        if (candidate.isValidTimeBuffer()) {
            digits = candidate
        }
    }

    fun onBackspace() {
        digits = digits.dropLast(1)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp),
        ) {
            LightText(
                text = "AM",
                variant = LightTextVariant.Copy,
                underline = !isPm,
                modifier = Modifier.lightClickable(onClick = { isPm = false }).padding(start = 16.dp),
            )

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row {
                    LightText( // Hour
                        text = digits.timeDisplay(TimeField.HOUR),
                        variant = LightTextVariant.Title,
                        maxLines = 1,
                    )
                    LightText(
                        text = ":",
                        variant = LightTextVariant.Title,
                        maxLines = 1,
                    )
                    LightText( // Minute
                        text = digits.timeDisplay(TimeField.MINUTE),
                        variant = LightTextVariant.Title,
                        maxLines = 1,
                    )
                }
            }

            LightText(
                text = "PM",
                variant = LightTextVariant.Copy,
                underline = isPm,
                modifier = Modifier.lightClickable(onClick = { isPm = true }).padding(end = 16.dp),
            )
        }

        // Number keys
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            KEYPAD_ROWS.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                    row.forEach { digit ->
                        KeypadDigit(text = digit.toString(), onClick = { onDigit(digit) })
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(64.dp),
            ) {
                val hasValidInput = digits.length >= 3
                LightIcon(
                    icon = LightIcons.ACCEPT,
                    modifier = Modifier
                        .alpha(if (hasValidInput) 1f else 0f)
                        .lightClickable(enabled = hasValidInput, onClick = onConfirm),
                )
                KeypadDigit(text = "0", onClick = { onDigit(0) })
                LightIcon(icon = LightIcons.BACK, modifier = Modifier.lightClickable(onClick = ::onBackspace))
            }
        }
    }
}

@Composable
private fun KeypadDigit(text: String, onClick: () -> Unit) {
    LightText(
        text = text,
        variant = LightTextVariant.Subtitle,
        align = TextAlign.Center,
        modifier = Modifier.lightClickable(onClick = onClick),
    )
}

/**
 * How many typed digits belong to the hour vs the minute
 *
 * Example input sequence (Input -> Displayed As -> Return):
 * 1 -> ":1" -> (0,1)
 * 12 -> "1:2" -> (1,1)
 * 123 -> "1:23" -> (1,2)
 * 1234 -> "12:34" -> (2,2)
 */
private fun String.hourMinuteLengths(): Pair<Int, Int> = when {
    isEmpty() -> 0 to 0
    length == 1 -> 0 to 1
    else -> {
        val hourLen = (length - 2).coerceIn(1, 2)
        hourLen to (length - hourLen)
    }
}

/** Input validation for incoming keypresses */
private fun String.isValidTimeBuffer(): Boolean {
    val (hour, minute) = parseHourMinute()
    return hour in 0..12 && minute in 0..59
}

/** Convert raw typed string to hour/minute pair */
private fun String.parseHourMinute(): Pair<Int, Int> {
    val (hourLen, minuteLen) = hourMinuteLengths()
    val hour = if (hourLen == 0) 0 else substring(0, hourLen).toInt()
    val minute = if (minuteLen == 0) 0 else substring(hourLen, hourLen + minuteLen).toInt()
    return hour to minute
}

/** Retrieve either the hour or the minute part from the current input */
private fun String.timeDisplay(field: TimeField): String {
    val (hourLen, minuteLen) = hourMinuteLengths()
    return when (field) {
        TimeField.HOUR -> if (hourLen == 0) "" else substring(0, hourLen)
        TimeField.MINUTE -> if (minuteLen == 0) "" else substring(hourLen, hourLen + minuteLen)
    }
}

private fun to24Hour(hour12: Int, isPm: Boolean): Int = when {
    hour12 == 12 -> if (isPm) 12 else 0
    isPm -> hour12 + 12
    else -> hour12
}
