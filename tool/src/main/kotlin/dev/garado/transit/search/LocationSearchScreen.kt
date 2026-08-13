package dev.garado.transit.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
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
import com.thelightphone.sdk.ui.lightClickable
import dev.garado.transit.StatusBar
import kotlinx.coroutines.flow.MutableStateFlow

class LocationSearchScreen(
    sealedActivity: SealedLightActivity,
    private val startInSearch: Boolean = false,
) : SimpleLightScreen<LocationResult>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val store = remember { SavedLocationStore(SavedLocationDatabaseHolder.get(lightContext)) }
        val savedLocations by store.all.collectAsState(initial = emptyList())

        var isEnteringQuery by remember { mutableStateOf(startInSearch) }
        val fieldState = rememberTextFieldState("")
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

        fun submitQuery(query: CharSequence) {
            navigateTo({ activity -> LocationResultsScreen(activity, query.toString()) }) { result ->
                goBack(result)
            }
        }

        LightTheme(colors = themeColors) {
            if (isEnteringQuery) {
                LightTextInputEditor(
                    title = "Search Location",
                    state = fieldState,
                    onSubmit = { submitQuery(it) },
                    onBack = { isEnteringQuery = false },
                    keyboardOptionsFlow = keyboardOptionsFlow,
                    singleLine = true,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    StatusBar()
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(
                            icon = LightIcons.BACK,
                            sizeUnits = 1.5f,
                            onClick = { goBack() },
                        ),
                        center = LightTopBarCenter.Text("Search Location"),
                        rightButton = LightBarButton.LightIcon(
                            icon = LightIcons.SEARCH,
                            sizeUnits = 1.5f,
                            onClick = { isEnteringQuery = true },
                        ),
                    )

                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        savedLocations.forEach { saved ->
                            SavedLocationRow(
                                saved = saved,
                                onClick = { goBack(saved.result.copy(displayName = saved.displayName)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedLocationRow(saved: SavedLocation, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        LightText(text = saved.displayName, variant = LightTextVariant.Copy)
        LightText(
            text = saved.result.address,
            variant = LightTextVariant.Detail,
            lighten = true,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
