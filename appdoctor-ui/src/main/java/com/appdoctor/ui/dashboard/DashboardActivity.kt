package com.appdoctor.ui.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appdoctor.core.AppDoctor
import com.appdoctor.ui.theme.AppDoctorTheme

/**
 * Host [ComponentActivity] for the Compose diagnostics dashboard.
 *
 * Launched by the floating trigger. Being a real Activity (rather than a window overlay)
 * means the dashboard gets a proper lifecycle, back-handling and `ViewModel` scope for
 * free, and the live metric flows are automatically stopped when it is not in the
 * foreground.
 *
 * Declared (not exported) in the `appdoctor-ui` manifest, so consumers need no manifest
 * changes.
 */
public class DashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val metrics = AppDoctor.metrics
        if (metrics == null) {
            // AppDoctor isn't active (e.g. process died and relaunched cold). Nothing to show.
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            AppDoctorTheme {
                val vm: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.factory(metrics),
                )
                DashboardScreen(viewModel = vm, onClose = ::finish)
            }
        }
    }
}
