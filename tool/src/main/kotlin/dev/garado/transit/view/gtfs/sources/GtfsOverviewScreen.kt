package dev.garado.transit.view.gtfs.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
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
import dev.garado.transit.data.gtfs.sources.GtfsSourceDownloadState
import dev.garado.transit.models.formatFileSize
import dev.garado.transit.view.home.StatusBar

class GtfsOverviewScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, GtfsOverviewViewModel>(sealedActivity) {

    override val viewModelClass = GtfsOverviewViewModel::class.java
    override fun createViewModel() = GtfsOverviewViewModel(lightContext)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val sources by viewModel.sources.collectAsState()
        val spaceUsedBytes by viewModel.spaceUsedBytes.collectAsState()

        val downloading = sources.count { it.downloadState == GtfsSourceDownloadState.DOWNLOADING }
        val failed = sources.count { it.downloadState == GtfsSourceDownloadState.FAILED }
        val statusParts = buildList {
            if (downloading > 0) add("$downloading downloading")
            if (failed > 0) add("$failed failed")
            if (failed > 0) add("Tap to retry all")
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
                    center = LightTopBarCenter.Text("Route data"),
                )

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    StatRow(
                        value = "${sources.size} saved source${if (sources.size == 1) "" else "s"}",
                        subLabel = statusParts.joinToString(" / ").takeIf { it.isNotEmpty() },
                        onClick = if (failed > 0) viewModel::retryAllFailed else null,
                    )
                    StatRow(
                        value = spaceUsedBytes?.let { formatFileSize(it) } ?: "…",
                        subLabel = "Space used",
                    )
                    LightText(
                        text = "Edit sources",
                        variant = LightTextVariant.Copy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .lightClickable(onClick = { navigateTo(::GtfsManagerScreen) })
                            .padding(vertical = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(value: String, subLabel: String?, onClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .let { if (onClick != null) it.lightClickable(onClick = onClick) else it },
    ) {
        LightText(text = value, variant = LightTextVariant.Copy)
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
