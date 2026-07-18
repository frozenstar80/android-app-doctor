package com.appdoctor.core.internal.util

import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * Detects whether the host application is debuggable.
 *
 * AppDoctor uses this to guarantee it is a **complete no-op in release builds** without
 * requiring the host to wire up build-variant-specific dependencies. Relying on the host
 * app's `FLAG_DEBUGGABLE` works regardless of how the host structures its build.
 *
 * Not part of the public API.
 */
internal object BuildTypeDetector {

    /** `true` when the host app has `android:debuggable=true` (i.e. a debug build). */
    fun isDebuggable(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
