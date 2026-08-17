package dev.garado.transit.view.home

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

@Composable
fun OnboardingContent(onDone: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        StatusBar()
        Column(modifier = Modifier.weight(1f).padding(16.dp)) {
            LightText(text = "Welcome to Transit", variant = LightTextVariant.Heading)
            LightText(
                text = "Plan trips, browse nearby stops and routes, and download offline transit data for the places you travel.",
                variant = LightTextVariant.Paragraph,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        LightText(
            text = "GET STARTED",
            variant = LightTextVariant.Button,
            align = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .lightClickable(onClick = onDone),
        )
    }
}
