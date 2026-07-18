package com.appdoctor.core.monitor

import kotlinx.coroutines.flow.StateFlow

/**
 * A live source of a single metric of type [T].
 *
 * Implementations expose their latest value through a [StateFlow]. To keep CPU cost
 * near zero, monitors are expected to only do work **while their [data] flow is being
 * collected** (typically achieved with `stateIn(scope, WhileSubscribed(), ...)`).
 * When nothing observes the dashboard, monitors go idle.
 */
public interface Monitor<out T> {

    /** The most recent value, hot while subscribed and replayed to new collectors. */
    public val data: StateFlow<T>
}
