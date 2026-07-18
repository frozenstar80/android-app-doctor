package com.appdoctor.core.internal.util

import android.util.Log

/**
 * Thin internal logging shim so the library has one place to gate/redirect logs.
 * Not part of the public API.
 */
internal object Logger {

    const val TAG: String = "AppDoctor"

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(TAG, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
