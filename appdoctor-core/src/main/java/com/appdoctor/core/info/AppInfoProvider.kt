package com.appdoctor.core.info

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build

/**
 * Captures an [AppInfo] snapshot from the host application's [android.content.pm.PackageManager].
 *
 * @param context any [Context]; the application context is used internally to avoid leaks.
 */
public class AppInfoProvider(context: Context) {

    private val appContext: Context = context.applicationContext

    /** Reads the host application's package metadata into an immutable [AppInfo]. */
    public fun capture(): AppInfo {
        val packageName = appContext.packageName
        val pm = appContext.packageManager
        val packageInfo: PackageInfo = pm.getPackageInfo(packageName, 0)
        val appInfo: ApplicationInfo = appContext.applicationInfo

        return AppInfo(
            packageName = packageName,
            versionName = packageInfo.versionName ?: "Unknown",
            versionCode = extractVersionCode(packageInfo),
            buildType = if (isDebuggable(appInfo)) BuildType.DEBUG else BuildType.RELEASE,
            minSdk = appInfo.minSdkVersion,
            targetSdk = appInfo.targetSdkVersion,
        )
    }

    private fun extractVersionCode(packageInfo: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }

    private fun isDebuggable(appInfo: ApplicationInfo): Boolean =
        (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
