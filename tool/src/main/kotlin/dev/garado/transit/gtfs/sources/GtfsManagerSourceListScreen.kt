package dev.garado.transit.gtfs.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import dev.garado.transit.StatusBar
import dev.garado.transit.gtfs.GtfsDisplayNames
import dev.garado.transit.gtfs.local.GtfsDatabaseHolder
import kotlinx.coroutines.launch

class GtfsManagerSourceListScreen(
    sealedActivity: SealedLightActivity,
    private val countryCode: String,
    private val regionCode: String,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val store = remember {
            GtfsSourceStore(
                GtfsDatabaseHolder.get(lightContext),
                lightContext.filesDir,
            )
        }
        val displayNames = remember { GtfsDisplayNames.get(lightContext) }
        val scope = rememberCoroutineScope()
        val allSources by store.all.collectAsState(initial = null)
        var isEditing by remember { mutableStateOf(false) }

        val sources = remember(allSources) {
            allSources?.filter { it.regionCode == regionCode }
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

                if (sources.isNullOrEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (sources != null) {
                            LightText(
                                text = "No sources added",
                                variant = LightTextVariant.Paragraph,
                                lighten = true,
                            )
                        }
                    }
                } else {
                    LightScrollView(modifier = Modifier.weight(1f)) {
                        sources.forEach { source ->
                            GtfsSourceRow(
                                source = source,
                                displayName = displayNames.agencyName(source.key, source.regionCode),
                                isEditing = isEditing,
                                onDeleteClick = { scope.launch { store.delete(source) } },
                                onRetryClick = { scope.launch { store.retryDownload(source) } },
                            )
                        }
                    }

                    LightText(
                        text = if (isEditing) "DONE" else "EDIT",
                        variant = LightTextVariant.Button,
                        align = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 16.dp)
                            .lightClickable(onClick = { isEditing = !isEditing }),
                    )
                }
            }
        }
    }
}

private val DELETE_ICON_SIZE_UNITS = 1.25f
private val DELETE_ICON_GAP = 8.dp

@Composable
private fun GtfsSourceRow(
    source: GtfsSource,
    displayName: String,
    isEditing: Boolean,
    onDeleteClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp, start = 16.dp),
    ) {
        Box(
            modifier = Modifier.width(DELETE_ICON_SIZE_UNITS.gridUnitsAsDp() + DELETE_ICON_GAP),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (isEditing) {
                LightIcon(
                    icon = LightIcons.DELETE,
                    size = DELETE_ICON_SIZE_UNITS,
                    modifier = Modifier.lightClickable(onClick = onDeleteClick),
                )
            }
        }
        Column {
            LightText(text = displayName, variant = LightTextVariant.Copy)
            DownloadStateLabel(source.id, source.downloadState, onRetryClick)
        }
    }
}

@Composable
private fun DownloadStateLabel(sourceId: Long, state: GtfsSourceDownloadState, onRetryClick: () -> Unit) {
    val progress by GtfsImportProgressTracker.progress.collectAsState()
    val liveStage = progress[sourceId]
    val text = liveStage?.label ?: state.statusLabel ?: return
    val baseModifier = Modifier.padding(top = 2.dp)
    LightText(
        text = text,
        variant = LightTextVariant.Detail,
        lighten = true,
        modifier = if (liveStage == null && state.isRetryable) baseModifier.lightClickable(onClick = onRetryClick) else baseModifier,
    )
}
