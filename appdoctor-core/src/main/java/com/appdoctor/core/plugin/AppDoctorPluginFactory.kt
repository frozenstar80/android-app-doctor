package com.appdoctor.core.plugin

import com.appdoctor.core.AppDoctorConfig

/**
 * ServiceLoader entry point for a self-registering built-in plugin module.
 *
 * A collector/inspector module declares its factory in
 * `META-INF/services/com.appdoctor.core.plugin.AppDoctorPluginFactory`; AppDoctor
 * discovers it at install time — no core edits are needed to add a new module. Two rules
 * apply to implementations used for discovery:
 *
 *  - they **must** expose a public no-argument constructor, and
 *  - [create] should be cheap and side-effect free beyond building the plugin.
 */
public interface AppDoctorPluginFactory {

    /**
     * Creates the plugin, or returns `null` to opt out for this [config] (for example the
     * network module returns `null` when [AppDoctorConfig.captureNetwork] is `false`).
     */
    public fun create(config: AppDoctorConfig): AppDoctorPlugin?
}
