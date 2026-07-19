package com.appdoctor.timeline.engine

import com.appdoctor.core.ids.CollectorIds
import com.appdoctor.timeline.model.RuntimeTimelineEvent

public class TimelineFormatter {
    public fun titleFor(collectorId: String): String = when (collectorId) {
        CollectorIds.MEMORY -> "Memory Sample"
        CollectorIds.CPU -> "CPU Sample"
        CollectorIds.FPS -> "FPS Sample"
        CollectorIds.NETWORK -> "Network Activity"
        CollectorIds.DATABASE -> "Database Activity"
        CollectorIds.COMPOSE -> "Compose Activity"
        CollectorIds.DIAGNOSTICS -> "Diagnostic Event"
        else -> "$collectorId Activity"
    }

    public fun summary(metadata: Map<String, Any>): String =
        metadata.entries.joinToString(", ") { (key, value) -> "$key=$value" }

    public fun formatLine(event: RuntimeTimelineEvent): String =
        "[${event.timestamp}] ${event.category.name} ${event.title} — ${event.summary}"
}
