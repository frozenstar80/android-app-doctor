package com.example.appdoctor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appdoctor.core.AppDoctor
import com.example.appdoctor.ui.theme.SampleTheme

/**
 * Landing screen of the sample. Demonstrates:
 *  - the floating AppDoctor button (added automatically, no code here),
 *  - toggling AppDoctor at runtime via [AppDoctor.enable] / [AppDoctor.disable],
 *  - navigating to another Activity to show the button follows the foreground Activity,
 *  - a couple of "load generators" so the FPS / CPU / memory graphs actually move.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    SampleScreen(modifier = Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun SampleScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var appDoctorEnabled by remember { mutableStateOf(AppDoctor.isEnabled()) }
    var busyWork by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("AppDoctor Sample", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Tap the floating stethoscope button to open the diagnostics dashboard. " +
                "The button is added automatically to every Activity in debug builds.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "AppDoctor is ${if (appDoctorEnabled) "ENABLED" else "DISABLED"}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(onClick = {
                    if (AppDoctor.isEnabled()) AppDoctor.disable() else AppDoctor.enable()
                    appDoctorEnabled = AppDoctor.isEnabled()
                }) {
                    Text(if (appDoctorEnabled) "Disable overlay" else "Enable overlay")
                }
            }
        }

        Button(onClick = {
            context.startActivity(Intent(context, SecondActivity::class.java))
        }) {
            Text("Open second Activity")
        }

        Button(onClick = { busyWork = heavyComputation() }) {
            Text("Do heavy work (watch CPU / FPS)")
        }
        if (busyWork != 0) {
            Text("Computed checksum: $busyWork", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Deliberately wasteful work to make the CPU and FPS meters visibly react. */
private fun heavyComputation(): Int {
    var acc = 0
    repeat(6_000_000) { i ->
        acc = (acc + i) xor (acc * 31 + 7)
    }
    return acc
}
