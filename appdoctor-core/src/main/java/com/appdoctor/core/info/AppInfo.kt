package com.appdoctor.core.info

/** The build variant of the host application, as detected at runtime. */
public enum class BuildType {
    /** Application is marked debuggable (`FLAG_DEBUGGABLE`). */
    DEBUG,

    /** Application is not debuggable. */
    RELEASE,
}

/**
 * Immutable snapshot of the host application's identity and build metadata.
 *
 * Captured once via [AppInfoProvider]; these values are constant for a process.
 */
public data class AppInfo(
    /** Application package name / id, e.g. `"com.example.app"`. */
    public val packageName: String,
    /** Version name from the manifest, e.g. `"1.4.2"`. */
    public val versionName: String,
    /** Version code from the manifest (long form). */
    public val versionCode: Long,
    /** Detected build type of the host app. */
    public val buildType: BuildType,
    /** `minSdkVersion` declared by the host app (0 if unavailable). */
    public val minSdk: Int,
    /** `targetSdkVersion` declared by the host app. */
    public val targetSdk: Int,
) {
    public companion object {
        /** Placeholder used before a real capture is available. */
        public val Unknown: AppInfo = AppInfo(
            packageName = "Unknown",
            versionName = "Unknown",
            versionCode = 0L,
            buildType = BuildType.RELEASE,
            minSdk = 0,
            targetSdk = 0,
        )
    }
}
