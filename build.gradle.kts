// Top-level build file. Plugins are declared here (apply false) and applied per-module.
// Note: AGP 9 provides built-in Kotlin, so there is no kotlin-android plugin.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.17.0"
}

apiValidation {
    ignoredProjects += listOf("sample-app")
}