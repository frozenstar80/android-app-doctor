package com.appdoctor.session

import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.plugin.AppDoctorPlugin
import com.appdoctor.core.plugin.AppDoctorPluginFactory

public class AppDoctorSessionPluginFactory : AppDoctorPluginFactory {
    override val priority: Int = 400

    override fun create(config: AppDoctorConfig): AppDoctorPlugin? =
        if (config.enableSessionReports) AppDoctorSessionPlugin(config) else null
}
