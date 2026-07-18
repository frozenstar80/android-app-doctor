package com.appdoctor.core.info

import android.os.Build

/**
 * Captures a [DeviceInfo] snapshot from [android.os.Build].
 *
 * Stateless and side-effect free; safe to call from any thread.
 */
public object DeviceInfoProvider {

    /** Reads the current device characteristics into an immutable [DeviceInfo]. */
    public fun capture(): DeviceInfo = DeviceInfo(
        androidVersion = Build.VERSION.RELEASE ?: "Unknown",
        apiLevel = Build.VERSION.SDK_INT,
        manufacturer = Build.MANUFACTURER ?: "Unknown",
        model = Build.MODEL ?: "Unknown",
        brand = Build.BRAND ?: "Unknown",
        device = Build.DEVICE ?: "Unknown",
        supportedAbis = Build.SUPPORTED_ABIS?.toList().orEmpty(),
    )
}
