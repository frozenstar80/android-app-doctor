package com.appdoctor.network

import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.plugin.AppDoctorPlugin
import com.appdoctor.core.plugin.AppDoctorPluginFactory

/**
 * ServiceLoader factory for [AppDoctorNetworkPlugin].
 *
 * Discovered by `appdoctor-core` via `META-INF/services`. Preserves the existing
 * [AppDoctorConfig.captureNetwork] gate by owning it here (it was previously enforced in
 * core), so behaviour is identical: no plugin is created when capture is disabled.
 */
public class AppDoctorNetworkPluginFactory : AppDoctorPluginFactory {
    override val priority: Int = 100

    override fun create(config: AppDoctorConfig): AppDoctorPlugin? =
        if (config.captureNetwork) AppDoctorNetworkPlugin(config) else null
}
