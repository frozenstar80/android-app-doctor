package com.appdoctor.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appdoctor.ui.theme.AppDoctorTokens

/**
 * A labelled progress bar for a `0..100` metric (memory, CPU, …). The bar colour shifts
 * from green to amber to red as the value climbs, giving an at-a-glance health read.
 *
 * @param fraction progress in `0f..1f`.
 * @param valueLabel pre-formatted value shown on the right (e.g. `"42.1%"`).
 */
@Composable
internal fun MetricBar(
    label: String,
    fraction: Float,
    valueLabel: String,
    modifier: Modifier = Modifier,
) {
    val safeFraction = fraction.coerceIn(0f, 1f)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        LinearProgressIndicator(
            progress = { safeFraction },
            color = healthColor(safeFraction),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        )
    }
}

private fun healthColor(fraction: Float): Color = when {
    fraction < 0.60f -> AppDoctorTokens.Good
    fraction < 0.85f -> AppDoctorTokens.Warn
    else -> AppDoctorTokens.Bad
}
