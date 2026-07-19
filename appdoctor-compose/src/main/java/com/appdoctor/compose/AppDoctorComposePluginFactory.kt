package com.appdoctor.compose

import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.plugin.AppDoctorPlugin
import com.appdoctor.core.plugin.AppDoctorPluginFactory

/**
 * ServiceLoader factory for [AppDoctorComposePlugin], discovered by `appdoctor-core` via
 * `META-INF/services`. Returns `null` when [AppDoctorConfig.captureCompose] is `false`.
 */
public class AppDoctorComposePluginFactory : AppDoctorPluginFactory {
    override val priority: Int = 100

    override fun create(config: AppDoctorConfig): AppDoctorPlugin? =
        if (config.captureCompose) AppDoctorComposePlugin(config) else null
}
