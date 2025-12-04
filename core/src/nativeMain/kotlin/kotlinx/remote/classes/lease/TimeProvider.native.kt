package kotlinx.remote.classes.lease

import kotlin.time.TimeSource

/**
 * Native implementation of SystemTimeProvider using TimeSource.Monotonic.
 * 
 * Note: This provides monotonic time rather than wall-clock time.
 * For lease-based GC, monotonic time is sufficient since we only need
 * to track relative time differences for lease expiration.
 */
actual object SystemTimeProvider : TimeProvider {
    private val startMark = TimeSource.Monotonic.markNow()
    
    override fun currentTimeMillis(): Long {
        return startMark.elapsedNow().inWholeMilliseconds
    }
}

