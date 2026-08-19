package dev.garado.transit.view.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.lightClickable
import dev.garado.transit.view.home.StatusBar

@Composable
fun OnboardingContent(onDone: () -> Unit) {
    OnboardingStepContent(
        title = "Welcome to Transit",
        description = "Plan trips, browse nearby stops and routes, and download offline transit data for the places you travel.",
        buttonLabel = "GET STARTED",
        onButtonClick = onDone,
    )
}

@Composable
fun OnboardingSetLocationContent(onSetLocation: () -> Unit) {
    OnboardingStepContent(
        title = "Set default location",
        description = "Choose a default location so we can center maps and searches near you, even without GPS.",
        buttonLabel = "SET LOCATION",
        onButtonClick = onSetLocation,
    )
}

@Composable
fun OnboardingCompleteContent(onComplete: () -> Unit) {
    OnboardingStepContent(
        title = "You're all set",
        description = "You can change your default location and other settings anytime from Settings.",
        buttonLabel = "COMPLETE",
        onButtonClick = onComplete,
    )
}

@Composable
private fun OnboardingStepContent(title: String, description: String, buttonLabel: String, onButtonClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        StatusBar()
        Column(modifier = Modifier.weight(1f).padding(16.dp)) {
            LightText(text = title, variant = LightTextVariant.Heading)
            LightText(
                text = description,
                variant = LightTextVariant.Paragraph,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        LightText(
            text = buttonLabel,
            variant = LightTextVariant.Button,
            align = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .lightClickable(onClick = onButtonClick),
        )
    }
}
