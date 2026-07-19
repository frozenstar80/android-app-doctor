package com.appdoctor.session

import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.plugin.AppDoctorPlugin
import com.appdoctor.core.plugin.PluginContext
import com.appdoctor.session.engine.SessionBuilder
import com.appdoctor.session.engine.SessionExporter
import com.appdoctor.session.engine.SessionMetadataProvider
import com.appdoctor.session.engine.SessionRecorder
import com.appdoctor.session.engine.SessionRepository
import com.appdoctor.session.model.SessionReport
import java.io.File
import java.util.UUID

public class AppDoctorSessionPlugin(
    private val config: AppDoctorConfig = AppDoctorConfig(),
) : AppDoctorPlugin {
    override val id: String = SESSION_PLUGIN_ID
    override val title: String = "Session Reports"

    @Volatile
    private var recorder: SessionRecorder? = null

    @Volatile
    private var manager: SessionManager? = null

    override fun onInstall(context: PluginContext) {
        val sessionId = "session-${UUID.randomUUID()}"
        val metadataProvider = SessionMetadataProvider(context.application, context.metrics)
        val recorder = SessionRecorder(
            collectorRegistry = context.collectors,
            scope = context.scope,
        )
        val builder = SessionBuilder(
            config = config,
            metrics = context.metrics,
            recorder = recorder,
            metadataProvider = metadataProvider::build,
        )
        val repository = SessionRepository(config.maximumStoredReports)
        manager = SessionManager(
            sessionId = sessionId,
            startedAtMillis = System.currentTimeMillis(),
            builder = builder,
            exporter = SessionExporter(),
            repository = repository,
        )
        this.recorder = recorder
    }

    override fun onEnable() {
        recorder?.start()
    }

    override fun onDisable() {
        recorder?.stop()
    }

    public fun generate(): SessionReport? = manager?.generate()

    public fun save(): SessionReport? = manager?.save()

    public fun export(format: SessionExportFormat, outputDirectory: File): File? =
        manager?.export(format, outputDirectory)

    public fun export(format: String, outputDirectory: File): File? {
        val parsed = runCatching { SessionExportFormat.valueOf(format.trim().uppercase()) }.getOrNull()
            ?: return null
        return export(parsed, outputDirectory)
    }

    public fun share(format: SessionExportFormat, outputDirectory: File): SessionSharePayload? =
        manager?.share(format, outputDirectory)

    public fun storedReports(): List<SessionReport> = manager?.storedReports().orEmpty()

    public companion object {
        public const val SESSION_PLUGIN_ID: String = "session-reports"
    }
}
