package com.appdoctor.ui.dashboard.plugin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.appdoctor.core.plugin.AppDoctorPlugin

/**
 * Optional dashboard extension point for plugins that want to render a full tab.
 */
public interface DashboardTabPlugin : AppDoctorPlugin {
    /** Stable tab key used for selection state. */
    public val tabKey: String

    /** Human-readable tab title. */
    public val tabTitle: String

    /** Composable content rendered when this tab is selected. */
    @Composable
    public fun DashboardTabContent(modifier: Modifier = Modifier)
}
