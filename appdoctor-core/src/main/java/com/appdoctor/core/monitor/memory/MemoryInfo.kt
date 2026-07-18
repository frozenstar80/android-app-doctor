package com.appdoctor.core.monitor.memory

/**
 * Immutable snapshot of JVM heap and native memory usage, in bytes.
 *
 * @see MemoryMonitor
 */
public data class MemoryInfo(
    /** Bytes currently used on the JVM heap (`totalMemory - freeMemory`). */
    public val usedBytes: Long,
    /** Maximum bytes the JVM heap may grow to (`Runtime.maxMemory`). */
    public val maxBytes: Long,
    /** Bytes still available before hitting [maxBytes] (`maxBytes - usedBytes`). */
    public val freeBytes: Long,
    /** Bytes allocated on the native heap (`Debug.getNativeHeapAllocatedSize`). */
    public val nativeAllocatedBytes: Long,
) {
    /** Heap utilisation as a percentage in `0f..100f`. */
    public val usagePercent: Float
        get() = if (maxBytes > 0L) usedBytes.toFloat() / maxBytes.toFloat() * 100f else 0f

    public companion object {
        /** Zeroed placeholder used as the initial value before the first read. */
        public val Empty: MemoryInfo = MemoryInfo(
            usedBytes = 0L,
            maxBytes = 0L,
            freeBytes = 0L,
            nativeAllocatedBytes = 0L,
        )
    }
}
