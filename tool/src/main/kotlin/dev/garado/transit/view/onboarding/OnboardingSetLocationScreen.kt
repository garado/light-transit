package dev.garado.transit.view.onboarding

import androidx.compose.runtime.Composable
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import dev.garado.transit.data.settings.DefaultLocationStore
import dev.garado.transit.view.search.LocationSearchScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OnboardingSetLocationScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        OnboardingSetLocationContent(onSetLocation = {
            navigateTo(::LocationSearchScreen) { result ->
                // Not scoped to this composable - navigating away right after would cancel it mid-write
                CoroutineScope(Dispatchers.IO).launch { DefaultLocationStore(lightContext.dataStore).set(result) }
                navigateTo(::OnboardingCompleteScreen) { goBack(Unit) }
            }
        })
    }
}
