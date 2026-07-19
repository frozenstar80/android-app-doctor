package com.appdoctor.timeline

import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.core.plugin.AppDoctorPlugin
import com.appdoctor.core.plugin.AppDoctorPluginFactory

public class AppDoctorTimelinePluginFactory : AppDoctorPluginFactory {
    override val priority: Int = 300

    override fun create(config: AppDoctorConfig): AppDoctorPlugin? =
        if (config.enableTimeline) AppDoctorTimelinePlugin(config) else null
}
