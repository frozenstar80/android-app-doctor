package com.appdoctor.core.internal

import android.app.Activity
import com.appdoctor.core.internal.lifecycle.ActivityTracker
import com.appdoctor.core.overlay.AppDoctorOverlay
import java.lang.ref.WeakReference

/**
 * Decides *when* the [AppDoctorOverlay] should be visible, based on the enabled state and
 * activity lifecycle events. Holds only a [WeakReference] to the attached activity.
 *
 * AppDoctor's own UI activities (package `com.appdoctor.ui.*`, e.g. the dashboard) are
 * skipped so the floating button never appears on top of the dashboard itself.
 *
 * Not part of the public API. All methods are expected to be called on the main thread
 * (activity lifecycle callbacks already are).
 */
internal class OverlayCoordinator(
    private val overlay: AppDoctorOverlay?,
) : ActivityTracker.Listener {

    private var enabled: Boolean = false
    private var attachedActivityRef: WeakReference<Activity>? = null

    /**
     * Update the enabled state and reconcile the overlay against [currentActivity].
     */
    fun setEnabled(enabled: Boolean, currentActivity: Activity?) {
        this.enabled = enabled
        if (enabled) {
            currentActivity?.let(::attachTo)
        } else {
            detachAttached()
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (enabled) attachTo(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        overlay?.detach(activity)
        if (attachedActivityRef?.get() === activity) {
            attachedActivityRef = null
        }
    }

    /** Detach from everything and release the overlay's resources. */
    fun shutdown() {
        detachAttached()
        overlay?.release()
    }

    private fun attachTo(activity: Activity) {
        if (isAppDoctorOwnUi(activity)) return
        overlay?.attach(activity)
        attachedActivityRef = WeakReference(activity)
    }

    private fun detachAttached() {
        attachedActivityRef?.get()?.let { overlay?.detach(it) }
        attachedActivityRef = null
    }

    private fun isAppDoctorOwnUi(activity: Activity): Boolean =
        activity.javaClass.name.startsWith(OWN_UI_PACKAGE_PREFIX)

    private companion object {
        private const val OWN_UI_PACKAGE_PREFIX = "com.appdoctor.ui."
    }
}
