package com.appdoctor.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory

/**
 * Enables AppDoctor runtime instrumentation for this Room database.
 *
 * ```kotlin
 * Room.databaseBuilder(context, AppDb::class.java, "app.db")
 *     .enableAppDoctor()
 *     .build()
 * ```
 *
 * Wraps [delegateFactory] (the framework SQLite factory by default) so every query,
 * statement and transaction is timed and recorded. Pass your own factory if you already
 * use a custom one. Safe to leave in place in all builds — it does nothing unless the
 * database inspector is installed and enabled.
 */
public fun <T : RoomDatabase> RoomDatabase.Builder<T>.enableAppDoctor(
    delegateFactory: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory(),
): RoomDatabase.Builder<T> = openHelperFactory(AppDoctorDatabase.wrapOpenHelperFactory(delegateFactory))
