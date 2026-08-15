package dev.garado.transit.view.gtfs.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
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
import dev.garado.transit.view.home.StatusBar
import dev.garado.transit.gtfs.GtfsDataset
import dev.garado.transit.gtfs.GtfsDisplayNames

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
                StatusBar()
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text(displayNames.countryName(countryCode)),
                )

                LightScrollView(modifier = Modifier.weight(1f)) {
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(top = 12.dp, bottom = 12.dp, start = 16.dp),
    ) {
        LightText(text = regionName, variant = LightTextVariant.Copy)
        LightText(
            text = "$count source${if (count == 1) "" else "s"}",
            variant = LightTextVariant.Detail,
            lighten = true,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
