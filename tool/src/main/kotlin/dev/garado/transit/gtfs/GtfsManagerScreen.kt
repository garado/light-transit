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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

class GtfsManagerScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, GtfsManagerViewModel>(sealedActivity) {

    override val viewModelClass = GtfsManagerViewModel::class.java
    override fun createViewModel() = GtfsManagerViewModel(lightContext)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val sources by viewModel.sources.collectAsState()
        val hasLoaded by viewModel.hasLoaded.collectAsState()
        val displayNames = remember { GtfsDisplayNames.get(lightContext) }
        var isEditing by remember { mutableStateOf(false) }

        val byCountry = remember(sources) {
            sources
                .groupBy { it.regionCode.substringBefore("-") }
                .toList()
                .sortedBy { (countryCode, _) -> displayNames.countryName(countryCode) }
        }

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = { goBack() }),
                    center = LightTopBarCenter.Text("Route data"),
                    rightButton = LightBarButton.LightIcon(
                        icon = LightIcons.ADD,
                        onClick = {
                            navigateTo(::GtfsCountryListScreen) { datasets -> viewModel.addAll(datasets) }
                        },
                        sizeUnits = 1.5f,
                    ),
                )

                if (byCountry.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (hasLoaded) {
                            LightText(
                                text = "No sources added",
                                variant = LightTextVariant.Paragraph,
                                lighten = true,
                            )
                        }
                    }
                } else {
                    LightScrollView(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        byCountry.forEach { (countryCode, srcs) ->
                            val regionCodes = srcs.map { it.regionCode }.distinct()
                            CountryRow(
                                countryName = displayNames.countryName(countryCode),
                                count = srcs.size,
                                isEditing = isEditing,
                                onDeleteClick = { viewModel.deleteAll(srcs) },
                                onClick = {
                                    if (regionCodes.size == 1) {
                                        navigateTo({ activity ->
                                            GtfsManagerSourceListScreen(activity, countryCode, regionCodes.first())
                                        })
                                    } else {
                                        navigateTo({ activity ->
                                            GtfsManagerRegionListScreen(activity, countryCode)
                                        })
                                    }
                                },
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
private fun CountryRow(
    countryName: String,
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
            LightText(text = countryName, variant = LightTextVariant.Copy)
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
