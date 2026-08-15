package dev.garado.transit.view.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextField
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.lightClickable
import dev.garado.transit.view.home.StatusBar
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun NameEditor(
    displayName: String,
    editSessionId: Int,
    onSubmit: (CharSequence) -> Unit,
    onBack: () -> Unit,
) {
    val nameFieldState = rememberTextFieldState(displayName)
    val keyboardOptionsFlow = remember {
        MutableStateFlow(
            KeyboardOptions(
                emptyList(),
                true,
                false,
                true,
                swipeEnabled = false,
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StatusBar()
        LightTextInputEditor(
            title = "Display Name",
            state = nameFieldState,
            onSubmit = onSubmit,
            onBack = onBack,
            keyboardOptionsFlow = keyboardOptionsFlow,
            singleLine = true,
            editorKey = editSessionId,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun SettingsTabContent(
    options: List<SettingsOption>,
    displayName: String,
    onToggle: (String) -> Unit,
    onSavedLocationsClick: () -> Unit,
    onApiSettingsClick: () -> Unit,
    onGtfsManagerClick: () -> Unit,
    onAttributionClick: () -> Unit,
    onEditName: () -> Unit,
) {
    LazyColumn {
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
                label = "API Settings",
                onClick = onApiSettingsClick,
            )
        }

        item {
            SettingsNavigationRow(
                label = "Manage Downloaded Data",
                onClick = onGtfsManagerClick,
            )
        }

        item {
            SettingsNavigationRow(
                label = "Attribution",
                onClick = onAttributionClick,
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
    LightText(
        text = label,
        variant = LightTextVariant.Copy,
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}
