package com.example.appdoctor

import android.app.Application
import com.appdoctor.core.AppDoctor
import com.appdoctor.core.AppDoctorConfig
import com.appdoctor.extension.ExtensionConfiguration
import com.example.appdoctor.extensions.CoilExtension
import com.example.appdoctor.extensions.FirebasePerformanceExtension
import com.example.appdoctor.extensions.PagingExtension
import com.example.appdoctor.extensions.SQLDelightExtension
import com.example.appdoctor.extensions.WorkManagerExtension

/**
 * Sample [Application] showing the one-line AppDoctor integration.
 *
 * That's the entire setup. AppDoctor detects the build type automatically and does
 * nothing in release builds.
 */
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // The only line you need. A draggable floating button now appears on every
        // Activity in debug builds; tapping it opens the diagnostics dashboard.
        AppDoctor.install(
            application = this,
            config = AppDoctorConfig(enableSessionReports = true),
            extensionConfiguration = ExtensionConfiguration(
                enableExtensions = true,
                allowThirdPartyExtensions = true,
                strictCompatibilityChecking = true,
                extensionLoadingStrategy = ExtensionConfiguration.LoadingStrategy.DEPENDENCY_INJECTION,
                dependencyInjectedExtensions = listOf(
                    WorkManagerExtension(),
                    PagingExtension(),
                    CoilExtension(),
                    FirebasePerformanceExtension(),
                    SQLDelightExtension(),
                ),
            ),
        )
    }
}
