package dev.garado.transit.gtfs.browse

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
import dev.garado.transit.StatusBar
import dev.garado.transit.gtfs.GtfsDataset
import dev.garado.transit.gtfs.GtfsDisplayNames
import dev.garado.transit.gtfs.formatFileSize
import dev.garado.transit.gtfs.local.GtfsDatabaseHolder
import dev.garado.transit.gtfs.sources.GtfsSourceDownloadState
import dev.garado.transit.gtfs.sources.GtfsSourceStore

class GtfsDatasetListScreen(
    sealedActivity: SealedLightActivity,
    private val regionCode: String,
    private val datasets: List<GtfsDataset>,
) : SimpleLightScreen<List<GtfsDataset>>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val displayNames = remember { GtfsDisplayNames.get(lightContext) }
        val store = remember {
            GtfsSourceStore(
                GtfsDatabaseHolder.get(lightContext),
                lightContext.filesDir,
            )
        }
        val sources by store.all.collectAsState(initial = emptyList())
        val downloadedKeys = remember(sources) {
            sources.filter { it.downloadState == GtfsSourceDownloadState.DOWNLOADED }
                .map { it.key to it.regionCode }
                .toSet()
        }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                StatusBar()
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text(displayNames.regionName(regionCode)),
                )

                LightScrollView(modifier = Modifier.weight(1f)) {
                    datasets.forEach { dataset ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .lightClickable(onClick = { goBack(listOf(dataset)) })
                                .padding(top = 12.dp, bottom = 12.dp, start = 16.dp),
                        ) {
                            LightText(
                                text = displayNames.agencyName(dataset.key, dataset.regionCode),
                                variant = LightTextVariant.Copy,
                            )
                            if (dataset.key to dataset.regionCode in downloadedKeys) {
                                LightText(
                                    text = "Downloaded",
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            } else if (dataset.sizeBytes != null) {
                                LightText(
                                    text = formatFileSize(dataset.sizeBytes),
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
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
                                GtfsBulkAddConfirmScreen(activity, datasets)
                            }) { picked -> goBack(picked) }
                        }),
                )
            }
        }
    }
}
