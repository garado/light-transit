package dev.garado.transit.gtfs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.lightClickable

/** Sub-regions within one country (only shown when that country has more than one region code). */
class GtfsRegionListScreen(
    sealedActivity: SealedLightActivity,
    private val countryCode: String,
    private val regions: Map<String, List<GtfsDataset>>,
) : SimpleLightScreen<List<GtfsDataset>>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val displayNames = remember { GtfsDisplayNames.get(lightContext) }
        val allVisible = regions.values.flatten()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text(displayNames.countryName(countryCode)),
                )

                LightScrollView(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    regions.entries.sortedBy { (regionCode, _) -> displayNames.regionName(regionCode) }.forEach { (regionCode, datasets) ->
                        RegionRow(
                            regionName = displayNames.regionName(regionCode),
                            count = datasets.size,
                            onClick = {
                                navigateTo({ activity ->
                                    GtfsDatasetListScreen(activity, regionCode, datasets)
                                }) { picked -> goBack(picked) }
                            },
                        )
                    }
                }

                LightText(
                    text = "ADD SHOWN",
                    variant = LightTextVariant.Button,
                    align = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                        .lightClickable(onClick = {
                            navigateTo({ activity ->
                                GtfsBulkAddConfirmScreen(activity, allVisible)
                            }) { datasets -> goBack(datasets) }
                        }),
                )
            }
        }
    }
}

@Composable
private fun RegionRow(regionName: String, count: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            LightText(text = regionName, variant = LightTextVariant.Copy)
            LightText(
                text = "$count source${if (count == 1) "" else "s"}",
                variant = LightTextVariant.Detail,
                lighten = true,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        LightIcon(icon = LightIcons.ARROW_RIGHT)
    }
}
