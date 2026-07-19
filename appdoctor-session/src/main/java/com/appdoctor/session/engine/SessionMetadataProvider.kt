package com.appdoctor.session.engine

import android.app.Application
import android.content.res.Configuration
import com.appdoctor.core.MetricsProvider
import com.appdoctor.session.model.SessionMetadata

public open class SessionMetadataProvider(
    private val application: Application,
    private val metrics: MetricsProvider,
) {
    public open fun build(
        sessionId: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
    ): SessionMetadata {
        val app = metrics.appInfo
        val device = metrics.deviceInfo
        val displayMetrics = application.resources.displayMetrics
        val orientation = when (application.resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> "PORTRAIT"
            Configuration.ORIENTATION_LANDSCAPE -> "LANDSCAPE"
            else -> null
        }
        return SessionMetadata(
            sessionId = sessionId,
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            durationMillis = (endTimeMillis - startTimeMillis).coerceAtLeast(0L),
            appVersion = app.versionName,
            versionCode = app.versionCode,
            buildVariant = app.buildType.name,
            packageName = app.packageName,
            deviceModel = device.model,
            androidVersion = device.androidVersion,
            apiLevel = device.apiLevel,
            manufacturer = device.manufacturer,
            screenSize = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}",
            orientation = orientation,
        )
    }
}
