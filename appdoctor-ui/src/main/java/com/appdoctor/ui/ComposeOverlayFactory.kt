package com.appdoctor.ui

import android.content.Context
import com.appdoctor.core.overlay.AppDoctorOverlay
import com.appdoctor.core.overlay.OverlayFactory
import com.appdoctor.ui.overlay.FloatingButtonOverlay

/**
 * Default [OverlayFactory] provided by the `appdoctor-ui` module.
 *
 * `appdoctor-core` discovers this class **reflectively** (by its fully-qualified name
 * `com.appdoctor.ui.ComposeOverlayFactory`) when the UI module is on the classpath, which
 * is what makes `AppDoctor.install(app)` work with zero configuration. Because of that,
 * this class must keep its fully-qualified name and its public no-argument constructor.
 */
public class ComposeOverlayFactory : OverlayFactory {

    override fun create(context: Context): AppDoctorOverlay =
        FloatingButtonOverlay(context)
}
