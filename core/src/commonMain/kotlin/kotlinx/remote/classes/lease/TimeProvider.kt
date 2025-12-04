package kotlinx.remote.classes.lease

/**
 * Provider for current time in milliseconds.
 * Abstracted to allow for testing and platform-specific implementations.
 */
fun interface TimeProvider {
    /**
     * Returns the current time in milliseconds since epoch.
     */
    fun currentTimeMillis(): Long
}

/**
 * Default time provider that uses the system clock.
 * Uses kotlin.system.getTimeMillis() on JVM and equivalent on other platforms.
 */
expect object SystemTimeProvider : TimeProvider

