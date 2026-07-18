package com.appdoctor.database

import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.appdoctor.core.AppDoctor
import com.appdoctor.database.internal.sqlite.AppDoctorSQLiteOpenHelperFactory
import com.appdoctor.database.recorder.DatabaseQueryRecorder

/**
 * Entry point for wiring database instrumentation into AppDoctor.
 *
 * Instrumentation writes to a process-wide sink resolved here, so the Room builder
 * extension is call-order independent and a true no-op when AppDoctor is not installed
 * (release builds / capture disabled): the wrappers see a `null`/idle recorder and fall
 * straight through to the real database.
 */
public object AppDoctorDatabase {

    @Volatile
    internal var activeRecorder: DatabaseQueryRecorder? = null
        private set

    internal fun attach(recorder: DatabaseQueryRecorder) {
        activeRecorder = recorder
    }

    internal fun detach(recorder: DatabaseQueryRecorder) {
        if (activeRecorder === recorder) activeRecorder = null
    }

    /** The installed database plugin instance, if present. */
    @JvmStatic
    public fun installed(): AppDoctorDatabasePlugin? =
        AppDoctor.plugin(AppDoctorDatabasePlugin.DATABASE_PLUGIN_ID) as? AppDoctorDatabasePlugin

    /**
     * Wraps an existing [SupportSQLiteOpenHelper.Factory] with AppDoctor instrumentation.
     * Use this for raw SupportSQLite / SQLDelight setups; Room users should prefer
     * [enableAppDoctor].
     */
    @JvmStatic
    public fun wrapOpenHelperFactory(
        delegate: SupportSQLiteOpenHelper.Factory,
    ): SupportSQLiteOpenHelper.Factory =
        AppDoctorSQLiteOpenHelperFactory(delegate) { activeRecorder }
}
