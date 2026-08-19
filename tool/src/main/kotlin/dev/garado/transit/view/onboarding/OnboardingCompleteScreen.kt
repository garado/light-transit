package dev.garado.transit.view.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import dev.garado.transit.data.settings.OnboardingStore
import kotlinx.coroutines.launch

class OnboardingCompleteScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()

        OnboardingCompleteContent(onComplete = {
            scope.launch {
                OnboardingStore(lightContext.dataStore).setComplete(false)
                goBack(Unit)
            }
        })
    }
}
