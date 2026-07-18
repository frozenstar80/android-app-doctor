package com.appdoctor.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdoctor.core.info.AppInfo
import com.appdoctor.core.info.DeviceInfo
import com.appdoctor.core.monitor.cpu.CpuInfo
import com.appdoctor.core.monitor.fps.FpsInfo
import com.appdoctor.core.monitor.memory.MemoryInfo
import com.appdoctor.ui.dashboard.components.InfoRow
import com.appdoctor.ui.dashboard.components.MetricBar
import com.appdoctor.ui.dashboard.components.SectionCard
import com.appdoctor.ui.dashboard.plugin.DashboardTabPlugin
import com.appdoctor.ui.format.Formatters

/**
 * Root composable of the diagnostics dashboard. Reads static info directly and collects
 * the live monitors with [collectAsStateWithLifecycle] so recomposition only happens for
 * the section whose value actually changed.
 */
@Composable
internal fun DashboardScreen(
    viewModel: DashboardViewModel,
    tabPlugins: List<DashboardTabPlugin>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val allTabs = remember(tabPlugins) {
        buildList {
            add(TabSpec("overview", "Overview", null))
            tabPlugins.forEach { plugin ->
                add(TabSpec(plugin.tabKey, plugin.tabTitle, plugin))
            }
        }
    }
    if (selectedTabIndex > allTabs.lastIndex) selectedTabIndex = 0

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            DashboardHeader(onClose = onClose)
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 12.dp,
            ) {
                allTabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(tab.title) },
                    )
                }
            }

            val selected = allTabs[selectedTabIndex]
            if (selected.plugin == null) {
                OverviewTab(viewModel = viewModel)
            } else {
                selected.plugin.DashboardTabContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun OverviewTab(viewModel: DashboardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DeviceInfoSection(viewModel.deviceInfo)
        AppInfoSection(viewModel.appInfo)

        val memory by viewModel.memory.collectAsStateWithLifecycle()
        MemorySection(memory)

        val fps by viewModel.fps.collectAsStateWithLifecycle()
        FpsSection(fps)

        val cpu by viewModel.cpu.collectAsStateWithLifecycle()
        CpuSection(cpu)
    }
}

private data class TabSpec(
    val key: String,
    val title: String,
    val plugin: DashboardTabPlugin?,
)

@Composable
private fun DashboardHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "AppDoctor",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        TextButton(onClick = onClose) { Text("Done") }
    }
}

@Composable
private fun DeviceInfoSection(info: DeviceInfo) {
    SectionCard(title = "Device Info") {
        InfoRow("Android Version", info.androidVersion)
        InfoRow("API Level", info.apiLevel.toString())
        InfoRow("Manufacturer", info.manufacturer)
        InfoRow("Model", info.model)
        InfoRow("Brand", info.brand)
        InfoRow("ABI", info.supportedAbis.firstOrNull() ?: "Unknown")
    }
}

@Composable
private fun AppInfoSection(info: AppInfo) {
    SectionCard(title = "App Info") {
        InfoRow("Version Name", info.versionName)
        InfoRow("Version Code", info.versionCode.toString())
        InfoRow("Build Type", info.buildType.name)
        InfoRow("Package", info.packageName)
        InfoRow("Min / Target SDK", "${info.minSdk} / ${info.targetSdk}")
    }
}

@Composable
private fun MemorySection(info: MemoryInfo) {
    SectionCard(title = "Memory") {
        MetricBar(
            label = "Heap usage",
            fraction = info.usagePercent / 100f,
            valueLabel = Formatters.percent(info.usagePercent),
        )
        InfoRow("Used Memory", Formatters.bytes(info.usedBytes))
        InfoRow("Max Heap", Formatters.bytes(info.maxBytes))
        InfoRow("Free Memory", Formatters.bytes(info.freeBytes))
        InfoRow("Native Allocated", Formatters.bytes(info.nativeAllocatedBytes))
    }
}

@Composable
private fun FpsSection(info: FpsInfo) {
    SectionCard(title = "FPS Monitor") {
        InfoRow("Current FPS", Formatters.fps(info.current))
        InfoRow("Average FPS", Formatters.fps(info.average))
        InfoRow("Lowest FPS", Formatters.fps(info.lowest))
    }
}

@Composable
private fun CpuSection(info: CpuInfo) {
    SectionCard(title = "CPU Usage") {
        MetricBar(
            label = "Process CPU",
            fraction = info.processCpuPercent / 100f,
            valueLabel = Formatters.percent(info.processCpuPercent),
        )
        InfoRow("Cores", info.coreCount.toString())
    }
}
