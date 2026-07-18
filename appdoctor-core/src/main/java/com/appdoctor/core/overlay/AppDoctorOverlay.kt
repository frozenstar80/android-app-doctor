package com.appdoctor.core.overlay

import android.app.Activity

/**
 * Port (in the Clean-Architecture sense) representing the on-screen trigger that the
 * user taps to open diagnostics — typically a draggable floating button.
 *
 * The core module owns the *when* (lifecycle + enabled state) and calls [attach] /
 * [detach] as activities come and go; an implementation (see the `appdoctor-ui` module)
 * owns the *how* (adding a view to the activity's window, launching the dashboard).
 *
 * Implementations must be main-thread affine and must not retain activities beyond a
 * matching [detach]/[release] to avoid leaks.
 */
public interface AppDoctorOverlay {

    /** Show the trigger over [activity]'s window. Called when an activity resumes. */
    public fun attach(activity: Activity)

    /** Remove the trigger from [activity]. Called when the activity pauses/stops. */
    public fun detach(activity: Activity)

    /** Release any long-lived resources. Called when AppDoctor is torn down. */
    public fun release()
}
