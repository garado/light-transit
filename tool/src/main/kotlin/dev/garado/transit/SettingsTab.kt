package dev.garado.transit

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextField
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.lightClickable

@Composable
fun SettingsTabContent(
    options: List<SettingsOption>,
    displayName: String,
    onToggle: (String) -> Unit,
    onAboutClick: () -> Unit,
    onSavedLocationsClick: () -> Unit,
    onEditName: () -> Unit,
) {
    LazyColumn {
        item {
            LightTextField(
                label = "Display Name",
                value = displayName,
                placeholder = "Enter your name",
                onClick = onEditName,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        items(options) { option ->
            SettingsToggleRow(
                option = option,
                onClick = { onToggle(option.label) },
            )
        }

        item {
            SettingsNavigationRow(
                label = "Saved Locations",
                onClick = onSavedLocationsClick,
            )
        }

        item {
            SettingsNavigationRow(
                label = "About",
                onClick = onAboutClick,
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(option: SettingsOption, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        LightIcon(
            icon = if (option.enabled) LightIcons.TOGGLE_STATE_ON else LightIcons.TOGGLE_STATE_OFF,
            modifier = Modifier.padding(end = 16.dp),
        )
        LightText(text = option.label, variant = LightTextVariant.Copy)
    }
}

@Composable
private fun SettingsNavigationRow(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        LightText(text = label, variant = LightTextVariant.Copy, modifier = Modifier.weight(1f))
        LightIcon(icon = LightIcons.ARROW_RIGHT)
    }
}
