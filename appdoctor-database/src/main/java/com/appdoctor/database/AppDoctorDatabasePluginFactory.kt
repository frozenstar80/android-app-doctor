package com.appdoctor.database

import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.plugin.AppDoctorPlugin
import com.appdoctor.core.plugin.AppDoctorPluginFactory

/**
 * ServiceLoader factory for [AppDoctorDatabasePlugin], discovered by `appdoctor-core` via
 * `META-INF/services`. Returns `null` when [AppDoctorConfig.captureDatabase] is `false`.
 */
public class AppDoctorDatabasePluginFactory : AppDoctorPluginFactory {

    override fun create(config: AppDoctorConfig): AppDoctorPlugin? =
        if (config.captureDatabase) AppDoctorDatabasePlugin(config) else null
}
