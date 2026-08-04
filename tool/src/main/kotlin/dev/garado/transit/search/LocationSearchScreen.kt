package dev.garado.transit.search

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import kotlinx.coroutines.flow.MutableStateFlow

class LocationSearchScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<LocationResult>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
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

        LightTheme(colors = themeColors) {
            LightTextInputEditor(
                title = "Search Location",
                state = fieldState,
                onSubmit = { query ->
                    navigateTo({ activity -> LocationResultsScreen(activity, query.toString()) }) { result ->
                        goBack(result)
                    }
                },
                onBack = { goBack() },
                keyboardOptionsFlow = keyboardOptionsFlow,
                singleLine = true,
            )
        }
    }
}
