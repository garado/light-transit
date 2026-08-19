package dev.garado.transit.view.onboarding

import androidx.compose.runtime.Composable
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen

class OnboardingScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        OnboardingContent(onDone = {
            navigateTo(::OnboardingSetLocationScreen) { goBack(Unit) }
        })
    }
}
