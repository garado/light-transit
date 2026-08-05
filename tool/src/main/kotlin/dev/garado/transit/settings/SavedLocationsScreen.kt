package dev.garado.transit.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
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
                    onBack = { goBack() },
                    onAddClick = { isEnteringName = true },
                )
            }
        }
    }
}

@Composable
private fun SavedLocationsList(
    savedLocations: List<SavedLocation>,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightThemeTokens.colors.background),
    ) {
        LightTopBar(
            leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = onBack),
            center = LightTopBarCenter.Text("Saved Locations"),
            rightButton = LightBarButton.LightIcon(icon = LightIcons.ADD, onClick = onAddClick),
        )

        LightScrollView(modifier = Modifier.padding(horizontal = 32.dp)) {
            savedLocations.forEach { saved -> SavedLocationRow(saved = saved) }
        }
    }
}

@Composable
private fun SavedLocationRow(saved: SavedLocation) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        LightText(text = saved.displayName, variant = LightTextVariant.Copy)
        LightText(
            text = saved.result.title,
            variant = LightTextVariant.Detail,
            lighten = true,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
