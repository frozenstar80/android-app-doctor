package com.appdoctor.timeline

import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.ids.PluginIds
import com.appdoctor.core.plugin.AppDoctorPlugin
import com.appdoctor.core.plugin.PluginContext
import com.appdoctor.timeline.api.TimelineReadApi
import com.appdoctor.timeline.engine.TimelineEngine
import com.appdoctor.timeline.engine.TimelineEventFactory
import com.appdoctor.timeline.engine.TimelineExporter
import com.appdoctor.timeline.engine.TimelineRepository
import com.appdoctor.timeline.model.RuntimeTimelineEvent
import com.appdoctor.timeline.model.TimelineEvent
import com.appdoctor.timeline.model.TimelineFilter
import com.appdoctor.timeline.model.TimelineSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public class AppDoctorTimelinePlugin(
    private val config: AppDoctorConfig = AppDoctorConfig(),
) : AppDoctorPlugin, TimelineReadApi {

    override val id: String = TIMELINE_PLUGIN_ID
    override val title: String = "Timeline"

    private val emptyEvents = MutableStateFlow<List<RuntimeTimelineEvent>>(emptyList())

    @Volatile
    private var engine: TimelineEngine? = null

    public val events: StateFlow<List<RuntimeTimelineEvent>>
        get() = engine?.events ?: emptyEvents

    override fun events(): StateFlow<List<TimelineEvent>> = events

    override fun onInstall(context: PluginContext) {
        val repository = TimelineRepository(maximumEvents = config.maximumTimelineEvents)
        engine = TimelineEngine(
            collectorRegistry = context.collectors,
            repository = repository,
            factory = TimelineEventFactory(),
            exporter = TimelineExporter(),
            scope = context.scope,
            groupingWindowMillis = config.timelineGroupingWindowMillis,
        )
    }

    override fun onEnable() {
        engine?.start()
    }

    override fun onDisable() {
        engine?.stop()
    }

    public fun session(): TimelineSession? = engine?.session()

    public fun search(filter: TimelineFilter, query: String): List<RuntimeTimelineEvent> =
        engine?.search(filter, query).orEmpty()

    public fun filter(filter: TimelineFilter): List<RuntimeTimelineEvent> =
        engine?.filter(filter).orEmpty()

    public fun exportJson(filter: TimelineFilter = TimelineFilter()): String =
        engine?.exportJson(filter).orEmpty()

    public fun exportMarkdown(filter: TimelineFilter = TimelineFilter()): String =
        engine?.exportMarkdown(filter).orEmpty()

    public companion object {
        public const val TIMELINE_PLUGIN_ID: String = PluginIds.TIMELINE
    }
}
