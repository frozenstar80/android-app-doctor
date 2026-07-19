package com.example.appdoctor

import android.app.Application
import com.appdoctor.core.AppDoctor
import com.appdoctor.core.AppDoctorConfig

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
            this,
            AppDoctorConfig(enableSessionReports = true),
        )
    }
}
