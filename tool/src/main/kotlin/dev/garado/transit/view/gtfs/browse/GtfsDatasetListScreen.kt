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
import androidx.compose.runtime.rememberCoroutineScope
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
import dev.garado.transit.models.GtfsDataset
import dev.garado.transit.data.gtfs.GtfsDisplayNames
import dev.garado.transit.models.formatFileSize
import dev.garado.transit.data.gtfs.local.GtfsDatabaseHolder
import dev.garado.transit.data.gtfs.sources.GtfsImportProgressTracker
import dev.garado.transit.data.gtfs.sources.GtfsSource
import dev.garado.transit.data.gtfs.sources.GtfsSourceDownloadState
import dev.garado.transit.data.gtfs.sources.GtfsSourceStore
import dev.garado.transit.data.gtfs.sources.label
import kotlinx.coroutines.launch

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
        val scope = rememberCoroutineScope()
        val sources by store.all.collectAsState(initial = emptyList())
        val sourceByKey = remember(sources) {
            sources.associateBy { it.key to it.regionCode }
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
                        val existingSource = sourceByKey[dataset.key to dataset.regionCode]
                        DatasetRow(
                            dataset = dataset,
                            displayName = displayNames.agencyName(dataset.key, dataset.regionCode),
                            existingSource = existingSource,
                            onClick = { scope.launch { store.add(dataset) } },
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
                                GtfsBulkAddConfirmScreen(activity, datasets)
                            }) { picked -> goBack(picked) }
                        }),
                )
            }
        }
    }
}

@Composable
private fun DatasetRow(
    dataset: GtfsDataset,
    displayName: String,
    existingSource: GtfsSource?,
    onClick: () -> Unit,
) {
    val progress by GtfsImportProgressTracker.progress.collectAsState()
    val liveStage = existingSource?.let { progress[it.id] }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(top = 12.dp, bottom = 12.dp, start = 16.dp),
    ) {
        LightText(text = displayName, variant = LightTextVariant.Copy)
        val subLabel = when {
            liveStage != null -> liveStage.label
            existingSource?.downloadState == GtfsSourceDownloadState.DOWNLOADED -> "Downloaded"
            existingSource?.downloadState?.statusLabel != null -> existingSource.downloadState.statusLabel
            else -> dataset.sizeBytes?.let { formatFileSize(it) }
        }
        if (subLabel != null) {
            LightText(
                text = subLabel,
                variant = LightTextVariant.Detail,
                lighten = true,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
