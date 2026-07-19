package com.appdoctor.diagnostics

import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.plugin.AppDoctorPlugin
import com.appdoctor.core.plugin.AppDoctorPluginFactory

/**
 * ServiceLoader factory for [AppDoctorDiagnosticsPlugin].
 */
public class AppDoctorDiagnosticsPluginFactory : AppDoctorPluginFactory {
    override val priority: Int = 200

    override fun create(config: AppDoctorConfig): AppDoctorPlugin? =
        if (config.enableDiagnostics) AppDoctorDiagnosticsPlugin(config) else null
}
