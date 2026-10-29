package dev.garado.transit.view.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.thelightphone.lp3Keyboard.ui.KeyboardOptions
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import dev.garado.transit.data.database.savedlocations.SavedLocationDatabaseHolder
import dev.garado.transit.data.database.savedlocations.SavedLocationStore
import dev.garado.transit.view.home.StatusBar
import dev.garado.transit.view.search.LocationSearchScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class OnboardingSaveLocationScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        var isEnteringName by remember { mutableStateOf(false) }
        val nameFieldState = rememberTextFieldState("")
        val keyboardOptionsFlow = remember {
            MutableStateFlow(KeyboardOptions(emptyList(), true, false, true, swipeEnabled = false))
        }

        fun goToComplete() {
            navigateTo(::OnboardingCompleteScreen) { goBack(Unit) }
        }

        fun startAddFlow(displayName: String) {
            navigateTo({ activity -> LocationSearchScreen(activity, startInSearch = true) }) { result ->
                // Not scoped to this composable - navigating away right after would cancel it mid-write
                CoroutineScope(Dispatchers.IO).launch {
                    SavedLocationStore(SavedLocationDatabaseHolder.get(lightContext)).add(displayName, result)
                }
                goToComplete()
            }
        }

        LightTheme(colors = themeColors) {
            if (isEnteringName) {
                Column(modifier = Modifier.fillMaxSize()) {
                    StatusBar()
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
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                OnboardingSaveLocationContent(
                    onAddLocation = { isEnteringName = true },
                    onSkip = ::goToComplete,
                )
            }
        }
    }
}
