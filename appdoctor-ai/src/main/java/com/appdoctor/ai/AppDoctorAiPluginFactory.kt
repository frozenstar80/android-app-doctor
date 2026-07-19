package com.appdoctor.ai

import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.plugin.AppDoctorPlugin
import com.appdoctor.core.plugin.AppDoctorPluginFactory

/**
 * ServiceLoader factory for conditionally creating [AppDoctorAiPlugin].
 *
 * The plugin is only instantiated when `enableAi` is enabled in [AppDoctorConfig].
 */
public class AppDoctorAiPluginFactory : AppDoctorPluginFactory {
    override val priority: Int = 500

    /**
     * Creates an AI plugin for the given [config].
     *
     * @return plugin instance when AI is enabled, otherwise `null`.
     */
    override fun create(config: AppDoctorConfig): AppDoctorPlugin? =
        if (config.enableAi) AppDoctorAiPlugin(config) else null
}
