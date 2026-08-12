package dev.garado.transit.gtfs.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.lightClickable
import dev.garado.transit.gtfs.GtfsDataset
import dev.garado.transit.gtfs.formatFileSize
import dev.garado.transit.gtfs.totalSizeBytes

class GtfsBulkAddConfirmScreen(
    sealedActivity: SealedLightActivity,
    private val datasets: List<GtfsDataset>,
) : SimpleLightScreen<List<GtfsDataset>>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text("Confirm"),
                )

                Column(modifier = Modifier.weight(1f).padding(horizontal = 32.dp, vertical = 16.dp)) {
                    LightText(
                        text = "Add ${datasets.size} source${if (datasets.size == 1) "" else "s"}?",
                        variant = LightTextVariant.Paragraph,
                    )
                    LightText(
                        text = "Total size: ${formatFileSize(datasets.totalSizeBytes())}",
                        variant = LightTextVariant.Detail,
                        lighten = true,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                LightText(
                    text = "ADD",
                    variant = LightTextVariant.Button,
                    align = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                        .lightClickable(onClick = { goBack(datasets) }),
                )
            }
        }
    }
}
