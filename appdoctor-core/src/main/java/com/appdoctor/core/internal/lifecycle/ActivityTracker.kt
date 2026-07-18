package com.appdoctor.core.internal.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Bridges [Application.ActivityLifecycleCallbacks] to a minimal [Listener] and tracks the
 * currently-resumed activity **weakly** (never a static/strong reference — no leaks).
 *
 * Not part of the public API.
 */
internal class ActivityTracker(
    private val listener: Listener,
) : Application.ActivityLifecycleCallbacks {

    /** Narrow callback surface the coordinator cares about. */
    interface Listener {
        fun onActivityResumed(activity: Activity)
        fun onActivityPaused(activity: Activity)
    }

    private var resumedActivityRef: WeakReference<Activity>? = null

    /** The currently resumed activity, or `null` if none / already collected. */
    val currentActivity: Activity?
        get() = resumedActivityRef?.get()

    override fun onActivityResumed(activity: Activity) {
        resumedActivityRef = WeakReference(activity)
        listener.onActivityResumed(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        listener.onActivityPaused(activity)
        if (resumedActivityRef?.get() === activity) {
            resumedActivityRef = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
