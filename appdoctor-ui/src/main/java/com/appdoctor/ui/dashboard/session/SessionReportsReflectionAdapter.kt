package com.appdoctor.ui.dashboard.session

import com.appdoctor.core.AppDoctor
import java.io.File

internal class SessionReportsReflectionAdapter {
    private val sessionPlugin: Any? get() = AppDoctor.plugin(SESSION_PLUGIN_ID)

    fun isAvailable(): Boolean = sessionPlugin != null

    fun export(format: String, outputDirectory: File): File? {
        val plugin = sessionPlugin ?: return null
        val method = plugin.javaClass.methods.firstOrNull {
            it.name == "export" &&
                it.parameterCount == 2 &&
                it.parameterTypes[0] == String::class.java &&
                it.parameterTypes[1] == File::class.java
        } ?: return null
        return runCatching { method.invoke(plugin, format, outputDirectory) as? File }.getOrNull()
    }

    companion object {
        private const val SESSION_PLUGIN_ID = "session-reports"
    }
}
