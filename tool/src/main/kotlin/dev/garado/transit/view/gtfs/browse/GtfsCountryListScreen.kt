package dev.garado.transit.view.gtfs.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
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
import dev.garado.transit.models.GtfsDataset
import dev.garado.transit.data.gtfs.GtfsDisplayNames
import dev.garado.transit.data.gtfs.local.GtfsDatabaseHolder
import dev.garado.transit.data.gtfs.sources.GtfsSourceStore

class GtfsCountryListScreen(sealedActivity: SealedLightActivity) :
    LightScreen<List<GtfsDataset>, GtfsBrowserViewModel>(sealedActivity) {

    override val viewModelClass = GtfsBrowserViewModel::class.java
    override fun createViewModel() = GtfsBrowserViewModel(lightContext)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val datasetsByRegion by viewModel.datasetsByRegion.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val isRefreshing by viewModel.isRefreshing.collectAsState()

        val displayNames = remember { GtfsDisplayNames.get(lightContext) }
        val store = remember { GtfsSourceStore(GtfsDatabaseHolder.get(lightContext), lightContext.filesDir) }
        val regionsByCountry = remember(datasetsByRegion) {
            datasetsByRegion.entries
                .groupBy({ it.key.substringBefore("-") }, { it.key to it.value })
                .mapValues { (_, pairs) -> pairs.toMap() }
                .toList()
                .sortedBy { (countryCode, _) -> displayNames.countryName(countryCode) }
        }
        val allVisible = datasetsByRegion.values.flatten()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                StatusBar()
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text(if (isRefreshing) "Refreshing..." else "Browse Sources"),
                    rightButton = LightBarButton.LightIcon(
                        icon = LightIcons.REFRESH,
                        onClick = { viewModel.refresh() },
                        sizeUnits = 1.25f,
                    ),
                )

                when {
                    isLoading || (isRefreshing && regionsByCountry.isEmpty()) ->
                        CenteredMessage(text = "Loading...", modifier = Modifier.weight(1f))
                    regionsByCountry.isEmpty() -> CenteredMessage(
                        text = "Failed to load sources",
                        modifier = Modifier.weight(1f),
                    )
                    else -> {
                        val scrollState = rememberScrollState(initial = viewModel.savedScrollOffset)
                        LaunchedEffect(scrollState) {
                            snapshotFlow { scrollState.value }.collect { viewModel.savedScrollOffset = it }
                        }
                        LightScrollView(modifier = Modifier.weight(1f), scrollState = scrollState) {
                            regionsByCountry.forEach { (countryCode, regions) ->
                                CountryRow(
                                    countryCode = countryCode,
                                    countryName = displayNames.countryName(countryCode),
                                    count = regions.values.sumOf { it.size },
                                    onClick = {
                                        if (regions.size == 1) {
                                            val datasets = regions.values.first()
                                            navigateTo({ activity ->
                                                GtfsDatasetListScreen(activity, countryCode, datasets)
                                            })
                                        } else {
                                            navigateTo({ activity ->
                                                GtfsRegionListScreen(activity, countryCode, regions)
                                            })
                                        }
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
                                    }) { picked -> store.addAllDetached(picked) }
                                }),
                        )
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
private fun CountryRow(countryCode: String, countryName: String, count: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(top = 12.dp, bottom = 12.dp, start = 16.dp),
    ) {
        LightText(text = countryName, variant = LightTextVariant.Copy)
        LightText(
            text = "$count source${if (count == 1) "" else "s"}",
            variant = LightTextVariant.Detail,
            lighten = true,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
