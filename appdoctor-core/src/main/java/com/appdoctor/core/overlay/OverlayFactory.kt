package com.appdoctor.core.overlay

import android.content.Context

/**
 * Factory that creates an [AppDoctorOverlay].
 *
 * This is the seam that keeps `appdoctor-core` free of any UI/Compose dependency
 * (Dependency Inversion). Two ways to supply one:
 *
 * 1. **Automatic** — if `appdoctor-ui` is on the classpath, core reflectively loads its
 *    `com.appdoctor.ui.ComposeOverlayFactory`. Zero configuration.
 * 2. **Explicit / DI** — provide your own via
 *    [com.appdoctor.core.AppDoctorConfig.overlayFactory] to swap the UI or inject a fake
 *    in tests.
 *
 * Implementations used for automatic discovery **must** expose a public no-argument
 * constructor.
 */
public interface OverlayFactory {

    /**
     * Creates the overlay.
     *
     * @param context application context (safe to retain).
     * @return the overlay, or throws if the environment is unsuitable.
     */
    public fun create(context: Context): AppDoctorOverlay
}
