package com.appdoctor.core.metric

/**
 * Optional capability an [com.appdoctor.core.plugin.AppDoctorPlugin] implements to
 * contribute [MetricCollector]s. When such a plugin is registered, AppDoctor adds its
 * collectors to the [CollectorRegistry] automatically.
 *
 * Optional by design (Interface Segregation): plugins that only render a tab need not
 * implement it, and no existing plugin API changes.
 */
public interface MetricCollectorProvider {

    /** Collectors this provider contributes. Read after the plugin's `onInstall`. */
    public val collectors: List<MetricCollector<Metric>>
}
