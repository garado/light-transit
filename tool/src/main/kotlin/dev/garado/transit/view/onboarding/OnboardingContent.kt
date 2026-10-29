package dev.garado.transit.view.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
        description = "Choose a default location to center maps and search results.",
        buttonLabel = "SET LOCATION",
        onButtonClick = onSetLocation,
    )
}

@Composable
fun OnboardingSaveLocationContent(onAddLocation: () -> Unit, onSkip: () -> Unit) {
    OnboardingStepContent(
        title = "Save a location",
        description = "Save frequently-visited places like home or work for quick access.",
        buttonLabel = "ADD",
        onButtonClick = onAddLocation,
        secondaryButtonLabel = "SKIP",
        onSecondaryButtonClick = onSkip,
    )
}

@Composable
fun OnboardingCompleteContent(onComplete: () -> Unit) {
    OnboardingStepContent(
        title = "You're all set!",
        description = "Change these options at any time from Settings."
        buttonLabel = "COMPLETE",
        onButtonClick = onComplete,
    )
}

@Composable
private fun OnboardingStepContent(
    title: String,
    description: String,
    buttonLabel: String,
    onButtonClick: () -> Unit,
    secondaryButtonLabel: String? = null,
    onSecondaryButtonClick: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        StatusBar()
        Column(modifier = Modifier.weight(1f).padding(16.dp)) {
            LightText(
                text = title,
                variant = LightTextVariant.Heading,
                align = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                LightText(
                    text = description,
                    variant = LightTextVariant.Paragraph,
                    align = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (secondaryButtonLabel != null && onSecondaryButtonClick != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
            ) {
                LightText(
                    text = buttonLabel,
                    variant = LightTextVariant.Button,
                    modifier = Modifier.lightClickable(onClick = onButtonClick),
                )
                Spacer(modifier = Modifier.weight(1f))
                LightText(
                    text = secondaryButtonLabel,
                    variant = LightTextVariant.Button,
                    lighten = true,
                    modifier = Modifier.lightClickable(onClick = onSecondaryButtonClick),
                )
            }
        } else {
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
}
