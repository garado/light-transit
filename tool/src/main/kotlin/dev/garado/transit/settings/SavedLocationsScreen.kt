package dev.garado.transit.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import dev.garado.transit.StatusBar
import dev.garado.transit.search.LocationSearchScreen
import dev.garado.transit.search.SavedLocation
import kotlinx.coroutines.flow.MutableStateFlow

class SavedLocationsScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, SavedLocationsViewModel>(sealedActivity) {

    override val viewModelClass = SavedLocationsViewModel::class.java
    override fun createViewModel() = SavedLocationsViewModel(lightContext)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val savedLocations by viewModel.savedLocations.collectAsState()
        var isEditing by remember { mutableStateOf(false) }

        var isEnteringName by remember { mutableStateOf(false) }
        val nameFieldState = rememberTextFieldState("")
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

        fun startAddFlow(displayName: String) {
            navigateTo({ activity -> LocationSearchScreen(activity, startInSearch = true) }) { result ->
                viewModel.add(displayName, result)
            }
        }

        LightTheme(colors = themeColors) {
            if (isEnteringName) {
                LightTextInputEditor(
                    title = "Display Name",
                    state = nameFieldState,
                    onSubmit = {
                        isEnteringName = false
                        startAddFlow(it.toString())
                    },
                    onBack = { isEnteringName = false },
                    keyboardOptionsFlow = keyboardOptionsFlow,
                    singleLine = true,
                )
            } else {
                SavedLocationsList(
                    savedLocations = savedLocations,
                    isEditing = isEditing,
                    onBack = { goBack() },
                    onEditClick = { isEditing = !isEditing },
                    onAddClick = { isEnteringName = true },
                    onDeleteClick = { saved -> viewModel.delete(saved) },
                )
            }
        }
    }
}

@Composable
private fun SavedLocationsList(
    savedLocations: List<SavedLocation>,
    isEditing: Boolean,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: (SavedLocation) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightThemeTokens.colors.background),
    ) {
        StatusBar()
        Box {
            LightTopBar(
                leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = onBack),
                center = LightTopBarCenter.Text("Saved Locations"),
            )
            LightText(
                text = "+",
                variant = LightTextVariant.Heading,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 1f.gridUnitsAsDp())
                    .lightClickable(onClick = onAddClick),
            )
        }

        LightScrollView(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            savedLocations.forEach { saved ->
                SavedLocationRow(
                    saved = saved,
                    isEditing = isEditing,
                    onDeleteClick = { onDeleteClick(saved) },
                )
            }
        }

        LightText(
            text = if (isEditing) "DONE" else "EDIT",
            variant = LightTextVariant.Button,
            align = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .lightClickable(onClick = onEditClick),
        )
    }
}

private val DELETE_ICON_SIZE_UNITS = 1.25f
private val DELETE_ICON_GAP = 8.dp

@Composable
private fun SavedLocationRow(saved: SavedLocation, isEditing: Boolean, onDeleteClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier.width(DELETE_ICON_SIZE_UNITS.gridUnitsAsDp() + DELETE_ICON_GAP),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (isEditing) {
                LightIcon(
                    icon = LightIcons.DELETE,
                    size = DELETE_ICON_SIZE_UNITS,
                    modifier = Modifier.lightClickable(onClick = onDeleteClick),
                )
            }
        }
        Column {
            LightText(text = saved.displayName, variant = LightTextVariant.Copy)
            LightText(
                text = saved.result.address,
                variant = LightTextVariant.Detail,
                lighten = true,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
