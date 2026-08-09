package dev.garado.transit.gtfs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
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

class GtfsRegionListScreen(sealedActivity: SealedLightActivity) :
    LightScreen<List<GtfsDataset>, GtfsBrowserViewModel>(sealedActivity) {

    override val viewModelClass = GtfsBrowserViewModel::class.java
    override fun createViewModel() = GtfsBrowserViewModel(lightContext)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val datasetsByRegion by viewModel.datasetsByRegion.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val isRefreshing by viewModel.isRefreshing.collectAsState()
        val allVisible = datasetsByRegion.values.flatten()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                Box {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                        center = LightTopBarCenter.Text(if (isRefreshing) "Refreshing..." else "Browse Sources"),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                    ) {
                        LightIcon(
                            icon = LightIcons.REFRESH,
                            size = 1.25f,
                            modifier = Modifier.lightClickable(onClick = { viewModel.refresh() }),
                        )
                        LightIcon(
                            icon = LightIcons.DOWNLOAD_ARROW,
                            size = 1.25f,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .lightClickable(onClick = {
                                    navigateTo({ activity ->
                                        GtfsBulkAddConfirmScreen(activity, allVisible)
                                    }) { datasets -> goBack(datasets) }
                                }),
                        )
                    }
                }

                when {
                    isLoading -> CenteredMessage(text = "Loading...", modifier = Modifier.weight(1f))
                    datasetsByRegion.isEmpty() -> CenteredMessage(
                        text = "Failed to load sources",
                        modifier = Modifier.weight(1f),
                    )
                    else -> LightScrollView(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        datasetsByRegion.forEach { (regionCode, datasets) ->
                            RegionRow(
                                regionCode = regionCode,
                                count = datasets.size,
                                onClick = {
                                    navigateTo({ activity ->
                                        GtfsDatasetListScreen(activity, regionCode, datasets)
                                    }) { picked -> goBack(picked) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        LightText(text = text, variant = LightTextVariant.Paragraph, lighten = true)
    }
}

@Composable
private fun RegionRow(regionCode: String, count: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            LightText(text = regionCode.uppercase(), variant = LightTextVariant.Copy)
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
