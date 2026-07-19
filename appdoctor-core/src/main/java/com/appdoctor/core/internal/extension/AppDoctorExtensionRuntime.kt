package com.appdoctor.core.internal.extension

import com.appdoctor.extension.Extension
import com.appdoctor.extension.ExtensionConfiguration
import com.appdoctor.extension.ExtensionContext
import com.appdoctor.extension.ExtensionException
import com.appdoctor.extension.ExtensionHealth
import com.appdoctor.extension.ExtensionInstaller
import com.appdoctor.extension.ExtensionLifecycle
import com.appdoctor.extension.ExtensionLogger
import com.appdoctor.extension.ExtensionMetadata
import com.appdoctor.extension.ExtensionRegistry
import com.appdoctor.extension.ExtensionValidator
import com.appdoctor.extension.ExtensionVersion

internal class AppDoctorExtensionRuntime(
    private val context: ExtensionContext,
    private val sdkVersion: ExtensionVersion,
    private val configuration: ExtensionConfiguration,
    private val validator: ExtensionValidator,
) : ExtensionInstaller, ExtensionRegistry {

    private data class Entry(
        val extension: Extension,
        val metadata: ExtensionMetadata,
        val capabilities: com.appdoctor.extension.ExtensionCapabilities,
        @Volatile var lifecycle: ExtensionLifecycle = ExtensionLifecycle.CREATED,
        @Volatile var health: ExtensionHealth = ExtensionHealth(ExtensionHealth.Status.HEALTHY),
    )

    private val entriesById: LinkedHashMap<String, Entry> = linkedMapOf()
    private val stateLock = Any()

    override val installed: List<ExtensionMetadata>
        get() = synchronized(stateLock) { entriesById.values.map { it.metadata } }

    override fun metadata(id: String): ExtensionMetadata? = synchronized(stateLock) { entriesById[id]?.metadata }

    override fun lifecycle(id: String): ExtensionLifecycle? = synchronized(stateLock) { entriesById[id]?.lifecycle }

    override fun health(id: String): ExtensionHealth? = synchronized(stateLock) { entriesById[id]?.health }

    override fun capabilities(id: String): com.appdoctor.extension.ExtensionCapabilities? =
        synchronized(stateLock) { entriesById[id]?.capabilities }

    override fun isEnabled(id: String): Boolean = lifecycle(id) == ExtensionLifecycle.ENABLED

    override fun install(extension: Extension): ExtensionMetadata {
        val issues = validator.validate(
            candidate = extension,
            installed = installed,
            sdkVersion = sdkVersion,
            configuration = configuration,
        )
        if (issues.isNotEmpty()) {
            throw ExtensionException(
                message = issues.joinToString(prefix = "Extension validation failed: ") { "[${it.code}] ${it.message}" },
            )
        }
        val metadata = extension.metadata
        val entry = Entry(
            extension = extension,
            metadata = metadata,
            capabilities = extension.capabilities,
        )

        synchronized(stateLock) {
            if (entriesById.containsKey(metadata.id)) {
                throw ExtensionException("Extension '${metadata.id}' is already installed.")
            }
            entriesById[metadata.id] = entry
        }

        try {
            extension.install(context)
            entry.lifecycle = ExtensionLifecycle.INSTALLED
            extension.initialize(context)
            entry.lifecycle = ExtensionLifecycle.INITIALIZED
            entry.health = ExtensionHealth(ExtensionHealth.Status.HEALTHY)
            context.logger.info("Extension installed: ${metadata.id}@${metadata.version.semanticVersion}")
            return metadata
        } catch (t: Throwable) {
            entry.lifecycle = ExtensionLifecycle.FAILED
            entry.health = ExtensionHealth(ExtensionHealth.Status.UNHEALTHY, t.message ?: "Unknown error")
            synchronized(stateLock) { entriesById.remove(metadata.id) }
            throw ExtensionException("Failed to install extension '${metadata.id}'.", t)
        }
    }

    override fun enable(id: String): Boolean = transition(id) { entry ->
        if (entry.lifecycle == ExtensionLifecycle.ENABLED) return@transition false
        if (entry.lifecycle != ExtensionLifecycle.INITIALIZED && entry.lifecycle != ExtensionLifecycle.DISABLED) {
            return@transition false
        }
        entry.extension.enable()
        entry.lifecycle = ExtensionLifecycle.ENABLED
        entry.health = ExtensionHealth(ExtensionHealth.Status.HEALTHY)
        true
    }

    override fun disable(id: String): Boolean = transition(id) { entry ->
        if (entry.lifecycle != ExtensionLifecycle.ENABLED) return@transition false
        entry.extension.disable()
        entry.lifecycle = ExtensionLifecycle.DISABLED
        entry.health = ExtensionHealth(ExtensionHealth.Status.DISABLED)
        true
    }

    override fun unload(id: String): Boolean = transition(id) { entry ->
        if (entry.lifecycle == ExtensionLifecycle.ENABLED) {
            entry.extension.disable()
            entry.lifecycle = ExtensionLifecycle.DISABLED
        }
        if (
            entry.lifecycle != ExtensionLifecycle.DISABLED &&
            entry.lifecycle != ExtensionLifecycle.INITIALIZED &&
            entry.lifecycle != ExtensionLifecycle.INSTALLED
        ) {
            return@transition false
        }
        entry.extension.unload()
        entry.lifecycle = ExtensionLifecycle.UNLOADED
        true
    }

    override fun destroy(id: String): Boolean = transition(id) { entry ->
        if (entry.lifecycle == ExtensionLifecycle.ENABLED) {
            entry.extension.disable()
            entry.lifecycle = ExtensionLifecycle.DISABLED
        }
        if (entry.lifecycle != ExtensionLifecycle.UNLOADED) {
            entry.extension.unload()
            entry.lifecycle = ExtensionLifecycle.UNLOADED
        }
        entry.extension.destroy()
        entry.lifecycle = ExtensionLifecycle.DESTROYED
        synchronized(stateLock) { entriesById.remove(id) }
        true
    }

    fun enableAll() {
        installed.forEach { metadata ->
            runTransition(metadata.id, "enable")
        }
    }

    fun disableAll() {
        installed.asReversed().forEach { metadata ->
            runTransition(metadata.id, "disable")
        }
    }

    fun destroyAll() {
        installed.asReversed().forEach { metadata ->
            runTransition(metadata.id, "destroy")
        }
    }

    private inline fun transition(id: String, block: (Entry) -> Boolean): Boolean {
        val entry = synchronized(stateLock) { entriesById[id] } ?: return false
        return try {
            block(entry)
        } catch (t: Throwable) {
            entry.lifecycle = ExtensionLifecycle.FAILED
            entry.health = ExtensionHealth(ExtensionHealth.Status.UNHEALTHY, t.message ?: "Unknown error")
            context.logger.error("Extension lifecycle transition failed for '$id'.", t)
            false
        }
    }

    private fun runTransition(id: String, phase: String) {
        when (phase) {
            "enable" -> enable(id)
            "disable" -> disable(id)
            "destroy" -> destroy(id)
        }
    }
}

internal class CoreExtensionLogger(
    private val debugFn: (String) -> Unit,
    private val infoFn: (String) -> Unit,
    private val warnFn: (String, Throwable?) -> Unit,
    private val errorFn: (String, Throwable?) -> Unit,
) : ExtensionLogger {
    override fun debug(message: String) {
        debugFn(message)
    }

    override fun info(message: String) {
        infoFn(message)
    }

    override fun warn(message: String, throwable: Throwable?) {
        warnFn(message, throwable)
    }

    override fun error(message: String, throwable: Throwable?) {
        errorFn(message, throwable)
    }
}

internal class CoreExtensionContext(
    override val applicationContext: Any,
    override val sdkVersion: ExtensionVersion,
    override val configuration: ExtensionConfiguration,
    override val logger: ExtensionLogger,
    private val services: Map<String, Any>,
) : ExtensionContext {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> service(key: String): T? = services[key] as? T
}

internal object ExtensionServiceKeys {
    const val APPLICATION = "application"
    const val METRICS_PROVIDER = "metricsProvider"
    const val COLLECTOR_REGISTRY = "collectorRegistry"
}
