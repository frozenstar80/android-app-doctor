package com.appdoctor.core.info

/**
 * Immutable snapshot of static device characteristics.
 *
 * These values never change during a process lifetime, so they are captured once
 * (see [DeviceInfoProvider]) rather than exposed as an observable stream.
 */
public data class DeviceInfo(
    /** Human readable Android version, e.g. `"14"`. */
    public val androidVersion: String,
    /** SDK / API level, e.g. `34`. */
    public val apiLevel: Int,
    /** Device manufacturer, e.g. `"Google"`. */
    public val manufacturer: String,
    /** Device model, e.g. `"Pixel 8"`. */
    public val model: String,
    /** Product brand, e.g. `"google"`. */
    public val brand: String,
    /** Industrial design / device codename, e.g. `"shiba"`. */
    public val device: String,
    /** Supported ABIs, most-preferred first, e.g. `["arm64-v8a", "armeabi-v7a"]`. */
    public val supportedAbis: List<String>,
) {
    public companion object {
        /** Placeholder used before a real capture is available. */
        public val Unknown: DeviceInfo = DeviceInfo(
            androidVersion = "Unknown",
            apiLevel = 0,
            manufacturer = "Unknown",
            model = "Unknown",
            brand = "Unknown",
            device = "Unknown",
            supportedAbis = emptyList(),
        )
    }
}
