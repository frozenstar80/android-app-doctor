package com.appdoctor.ai

import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.plugin.AppDoctorPlugin
import com.appdoctor.core.plugin.AppDoctorPluginFactory

public class AppDoctorAiPluginFactory : AppDoctorPluginFactory {
    override fun create(config: AppDoctorConfig): AppDoctorPlugin? =
        if (config.enableAi) AppDoctorAiPlugin(config) else null
}
