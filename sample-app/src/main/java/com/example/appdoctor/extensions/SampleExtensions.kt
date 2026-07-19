package com.example.appdoctor.extensions

import com.appdoctor.extension.Extension
import com.appdoctor.extension.ExtensionCapabilities
import com.appdoctor.extension.ExtensionCompatibility
import com.appdoctor.extension.ExtensionContext
import com.appdoctor.extension.ExtensionMetadata
import com.appdoctor.extension.ExtensionVersion

private const val APPDOCTOR_MIN_VERSION = "10.0.0"
private const val APPDOCTOR_MAX_VERSION = "10.99.99"

public abstract class BaseSampleExtension(
    final override val metadata: ExtensionMetadata,
    final override val capabilities: ExtensionCapabilities,
) : Extension {
    protected lateinit var context: ExtensionContext

    final override fun install(context: ExtensionContext) {
        this.context = context
        context.logger.info("Installing extension '${metadata.id}'.")
    }

    final override fun initialize(context: ExtensionContext) {
        context.logger.info("Initializing extension '${metadata.id}'.")
    }

    final override fun enable() {
        context.logger.info("Enabling extension '${metadata.id}'.")
    }

    final override fun disable() {
        context.logger.info("Disabling extension '${metadata.id}'.")
    }

    final override fun unload() {
        context.logger.info("Unloading extension '${metadata.id}'.")
    }

    final override fun destroy() {
        context.logger.info("Destroying extension '${metadata.id}'.")
    }
}

public class WorkManagerExtension : BaseSampleExtension(
    metadata = ExtensionMetadata(
        id = "sample.workmanager",
        name = "WorkManager Extension",
        description = "Demonstrates background-work timeline enrichment.",
        author = "AppDoctor",
        version = ExtensionVersion("1.0.0"),
        compatibility = ExtensionCompatibility(
            minimumSdkVersion = ExtensionVersion(APPDOCTOR_MIN_VERSION),
            maximumSdkVersion = ExtensionVersion(APPDOCTOR_MAX_VERSION),
            supportedCapabilities = setOf(
                ExtensionCapabilities.Capability.TIMELINE_EVENT_ENRICHERS,
                ExtensionCapabilities.Capability.RECOMMENDATIONS,
            ),
        ),
        website = "https://github.com/frozenstar80/android-app-doctor",
        isThirdParty = false,
    ),
    capabilities = ExtensionCapabilities(
        supported = setOf(
            ExtensionCapabilities.Capability.TIMELINE_EVENT_ENRICHERS,
            ExtensionCapabilities.Capability.RECOMMENDATIONS,
        ),
    ),
)

public class PagingExtension : BaseSampleExtension(
    metadata = ExtensionMetadata(
        id = "sample.paging",
        name = "Paging Extension",
        description = "Demonstrates diagnostics and report enrichment for Paging streams.",
        author = "AppDoctor",
        version = ExtensionVersion("1.0.0"),
        compatibility = ExtensionCompatibility(
            minimumSdkVersion = ExtensionVersion(APPDOCTOR_MIN_VERSION),
            maximumSdkVersion = ExtensionVersion(APPDOCTOR_MAX_VERSION),
            supportedCapabilities = setOf(
                ExtensionCapabilities.Capability.DIAGNOSTICS_RULES,
                ExtensionCapabilities.Capability.SESSION_REPORT_ENRICHERS,
            ),
        ),
        website = "https://github.com/frozenstar80/android-app-doctor",
        isThirdParty = false,
    ),
    capabilities = ExtensionCapabilities(
        supported = setOf(
            ExtensionCapabilities.Capability.DIAGNOSTICS_RULES,
            ExtensionCapabilities.Capability.SESSION_REPORT_ENRICHERS,
        ),
    ),
)

public class CoilExtension : BaseSampleExtension(
    metadata = ExtensionMetadata(
        id = "sample.coil",
        name = "Coil Extension",
        description = "Demonstrates collector and dashboard-tab capability declarations.",
        author = "AppDoctor",
        version = ExtensionVersion("1.0.0"),
        compatibility = ExtensionCompatibility(
            minimumSdkVersion = ExtensionVersion(APPDOCTOR_MIN_VERSION),
            maximumSdkVersion = ExtensionVersion(APPDOCTOR_MAX_VERSION),
            supportedCapabilities = setOf(
                ExtensionCapabilities.Capability.COLLECTORS,
                ExtensionCapabilities.Capability.DASHBOARD_TABS,
            ),
        ),
        website = "https://github.com/frozenstar80/android-app-doctor",
        isThirdParty = false,
    ),
    capabilities = ExtensionCapabilities(
        supported = setOf(
            ExtensionCapabilities.Capability.COLLECTORS,
            ExtensionCapabilities.Capability.DASHBOARD_TABS,
        ),
    ),
)

public class FirebasePerformanceExtension : BaseSampleExtension(
    metadata = ExtensionMetadata(
        id = "sample.firebase-performance",
        name = "Firebase Performance Extension",
        description = "Demonstrates exporter and formatter extension capabilities.",
        author = "AppDoctor",
        version = ExtensionVersion("1.0.0"),
        compatibility = ExtensionCompatibility(
            minimumSdkVersion = ExtensionVersion(APPDOCTOR_MIN_VERSION),
            maximumSdkVersion = ExtensionVersion(APPDOCTOR_MAX_VERSION),
            supportedCapabilities = setOf(
                ExtensionCapabilities.Capability.EXPORTERS,
                ExtensionCapabilities.Capability.FORMATTERS,
            ),
        ),
        website = "https://github.com/frozenstar80/android-app-doctor",
        isThirdParty = false,
    ),
    capabilities = ExtensionCapabilities(
        supported = setOf(
            ExtensionCapabilities.Capability.EXPORTERS,
            ExtensionCapabilities.Capability.FORMATTERS,
        ),
    ),
)

public class SQLDelightExtension : BaseSampleExtension(
    metadata = ExtensionMetadata(
        id = "sample.sqldelight",
        name = "SQLDelight Extension",
        description = "Demonstrates AI and remote-inspector enrichment declarations.",
        author = "AppDoctor",
        version = ExtensionVersion("1.0.0"),
        compatibility = ExtensionCompatibility(
            minimumSdkVersion = ExtensionVersion(APPDOCTOR_MIN_VERSION),
            maximumSdkVersion = ExtensionVersion(APPDOCTOR_MAX_VERSION),
            supportedCapabilities = setOf(
                ExtensionCapabilities.Capability.AI_PROMPT_ENRICHERS,
                ExtensionCapabilities.Capability.REMOTE_INSPECTOR_COMMANDS,
            ),
        ),
        website = "https://github.com/frozenstar80/android-app-doctor",
        isThirdParty = false,
    ),
    capabilities = ExtensionCapabilities(
        supported = setOf(
            ExtensionCapabilities.Capability.AI_PROMPT_ENRICHERS,
            ExtensionCapabilities.Capability.REMOTE_INSPECTOR_COMMANDS,
        ),
    ),
)
