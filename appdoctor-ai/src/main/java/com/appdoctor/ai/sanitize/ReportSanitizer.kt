package com.appdoctor.ai.sanitize

import com.appdoctor.session.model.DiagnosticsSection
import com.appdoctor.session.model.SessionMetadata
import com.appdoctor.session.model.SessionReport
import com.appdoctor.session.model.TimelineSection

public interface ReportSanitizer {
    public fun sanitize(report: SessionReport): SessionReport
}

public class CompositeReportSanitizer(
    private val sanitizers: List<ReportSanitizer>,
) : ReportSanitizer {
    override fun sanitize(report: SessionReport): SessionReport =
        sanitizers.fold(report) { current, sanitizer -> sanitizer.sanitize(current) }
}

public class SensitiveKeySanitizer : ReportSanitizer {
    private val blockedKeys = setOf(
        "authorization",
        "cookie",
        "set-cookie",
        "x-api-key",
        "apikey",
        "api_key",
        "token",
        "access_token",
        "refresh_token",
    )

    override fun sanitize(report: SessionReport): SessionReport = transform(report) { key, value ->
        if (blockedKeys.contains(key.lowercase())) REDACTED else maskPatterns(value)
    }
}

public class BodySanitizer : ReportSanitizer {
    private val bodyKeys = setOf("body", "requestbody", "responsebody", "payload", "content")
    override fun sanitize(report: SessionReport): SessionReport = transform(report) { key, value ->
        if (bodyKeys.contains(key.lowercase())) REDACTED else value
    }
}

public class DeviceIdentifierSanitizer : ReportSanitizer {
    override fun sanitize(report: SessionReport): SessionReport = report.copy(
        metadata = report.metadata.copy(
            deviceModel = REDACTED,
            manufacturer = REDACTED,
        ),
        deviceInformation = report.deviceInformation.mapValues { (key, value) ->
            if (key.equals("model", ignoreCase = true) || key.equals("manufacturer", ignoreCase = true)) {
                REDACTED
            } else {
                value
            }
        },
    )
}

public class ApplicationIdentifierSanitizer : ReportSanitizer {
    override fun sanitize(report: SessionReport): SessionReport = report.copy(
        metadata = report.metadata.copy(packageName = REDACTED),
        applicationInformation = report.applicationInformation.mapValues { (key, value) ->
            if (key.equals("packageName", ignoreCase = true) || key.equals("applicationId", ignoreCase = true)) {
                REDACTED
            } else {
                value
            }
        },
    )
}

public class UrlSanitizer : ReportSanitizer {
    override fun sanitize(report: SessionReport): SessionReport = transform(report) { _, value ->
        val idx = value.indexOf('?')
        if (idx > 0 && value.startsWith("http")) value.substring(0, idx) else value
    }
}

public object BuiltInReportSanitizers {
    public fun defaults(): List<ReportSanitizer> = listOf(
        SensitiveKeySanitizer(),
        BodySanitizer(),
        UrlSanitizer(),
        DeviceIdentifierSanitizer(),
    )
}

private const val REDACTED: String = "[REDACTED]"

private fun transform(
    report: SessionReport,
    transformValue: (key: String, value: String) -> String,
): SessionReport = report.copy(
    metadata = transformMetadata(report.metadata, transformValue),
    timeline = transformTimeline(report.timeline, transformValue),
    diagnostics = transformDiagnostics(report.diagnostics, transformValue),
    healthReport = report.healthReport,
    collectorSummaries = report.collectorSummaries.mapValues { (_, summary) ->
        summary.mapValues { (key, value) -> transformValue(key, value) }
    },
    analyticsSummaries = report.analyticsSummaries.mapValues { (_, summary) ->
        summary.mapValues { (key, value) -> transformValue(key, value) }
    },
    deviceInformation = report.deviceInformation.mapValues { (key, value) -> transformValue(key, value) },
    applicationInformation = report.applicationInformation.mapValues { (key, value) -> transformValue(key, value) },
    configuration = report.configuration.mapValues { (key, value) -> transformValue(key, value) },
    buildInformation = report.buildInformation.mapValues { (key, value) -> transformValue(key, value) },
)

private fun transformMetadata(
    metadata: SessionMetadata,
    transformValue: (key: String, value: String) -> String,
): SessionMetadata = metadata.copy(
    sessionId = transformValue("sessionId", metadata.sessionId),
    appVersion = transformValue("appVersion", metadata.appVersion),
    buildVariant = transformValue("buildVariant", metadata.buildVariant),
    packageName = transformValue("packageName", metadata.packageName),
    deviceModel = transformValue("deviceModel", metadata.deviceModel),
    androidVersion = transformValue("androidVersion", metadata.androidVersion),
    manufacturer = transformValue("manufacturer", metadata.manufacturer),
    screenSize = transformValue("screenSize", metadata.screenSize),
    orientation = metadata.orientation?.let { transformValue("orientation", it) },
)

private fun transformTimeline(
    timeline: TimelineSection?,
    transformValue: (key: String, value: String) -> String,
): TimelineSection? = timeline?.copy(
    events = timeline.events.map { event ->
        event.mapValues { (key, value) -> transformValue(key, value) }
    },
)

private fun transformDiagnostics(
    diagnostics: DiagnosticsSection?,
    transformValue: (key: String, value: String) -> String,
): DiagnosticsSection? = diagnostics?.copy(
    issues = diagnostics.issues.map { issue ->
        issue.mapValues { (key, value) -> transformValue(key, value) }
    },
)

private fun maskPatterns(value: String): String {
    val email = Regex("([A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+)")
    val phone = Regex("\\b\\+?[0-9][0-9\\- ]{7,}[0-9]\\b")
    return value.replace(email, REDACTED).replace(phone, REDACTED)
}
