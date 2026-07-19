package com.appdoctor.timeline.engine

import com.appdoctor.timeline.model.RuntimeTimelineEvent

public class TimelineFormatter {
    public fun titleFor(collectorId: String): String = when (collectorId) {
        "memory" -> "Memory Sample"
        "cpu" -> "CPU Sample"
        "fps" -> "FPS Sample"
        "network" -> "Network Activity"
        "database" -> "Database Activity"
        "compose" -> "Compose Activity"
        "diagnostics" -> "Diagnostic Event"
        else -> "$collectorId Activity"
    }

    public fun summary(metadata: Map<String, Any>): String =
        metadata.entries.joinToString(", ") { (key, value) -> "$key=$value" }

    public fun formatLine(event: RuntimeTimelineEvent): String =
        "[${event.timestamp}] ${event.category.name} ${event.title} — ${event.summary}"
}
