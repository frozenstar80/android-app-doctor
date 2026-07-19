package com.appdoctor.core.plugin

import android.app.Application
import com.appdoctor.core.MetricsProvider
import com.appdoctor.core.metric.CollectorRegistry
import kotlinx.coroutines.CoroutineScope

/**
 * Everything a plugin is handed when it is installed.
 *
 * Kept intentionally small and stable so future modules (Network Inspector, Room
 * Inspector, Compose Inspector, …) can be built without changing core.
 */
public interface PluginContext {

    /** The host application. */
    public val application: Application

    /** Live metrics a plugin may want to read or surface. */
    public val metrics: MetricsProvider

    /** Read-only registry of all available metric collectors. */
    public val collectors: CollectorRegistry

    /**
     * A long-lived scope tied to AppDoctor's lifetime. Coroutines launched here are
     * cancelled automatically when AppDoctor is torn down.
     */
    public val scope: CoroutineScope
}
