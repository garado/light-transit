package dev.garado.transit.gtfs

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
import kotlinx.coroutines.launch

/** Sub-regions of saved sources within one country, mirrors GtfsRegionListScreen. */
class GtfsManagerRegionListScreen(
    sealedActivity: SealedLightActivity,
    private val countryCode: String,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val store = remember { GtfsSourceStore(GtfsSourceDatabaseHolder.get(lightContext), lightContext.filesDir) }
        val displayNames = remember { GtfsDisplayNames.get(lightContext) }
        val scope = rememberCoroutineScope()
        val sources by store.all.collectAsState(initial = emptyList())
        var isEditing by remember { mutableStateOf(false) }

        val byRegion = remember(sources) {
            sources
                .filter { it.regionCode.substringBefore("-") == countryCode }
                .groupBy { it.regionCode }
                .toList()
                .sortedBy { (regionCode, _) -> displayNames.regionName(regionCode) }
        }

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
                    byRegion.forEach { (regionCode, srcs) ->
                        RegionRow(
                            regionName = displayNames.regionName(regionCode),
                            count = srcs.size,
                            isEditing = isEditing,
                            onDeleteClick = { scope.launch { store.deleteAll(srcs) } },
                            onClick = {
                                navigateTo({ activity ->
                                    GtfsManagerSourceListScreen(activity, countryCode, regionCode)
                                })
                            },
                        )
                    }
                }

                if (byRegion.isNotEmpty()) {
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
private fun RegionRow(
    regionName: String,
    count: Int,
    isEditing: Boolean,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .lightClickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
    ) {
        if (isEditing) {
            Box(
                modifier = Modifier.width(DELETE_ICON_SIZE_UNITS.gridUnitsAsDp() + DELETE_ICON_GAP),
                contentAlignment = Alignment.CenterStart,
            ) {
                LightIcon(
                    icon = LightIcons.DELETE,
                    size = DELETE_ICON_SIZE_UNITS,
                    modifier = Modifier.lightClickable(onClick = onDeleteClick),
                )
            }
        }
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
