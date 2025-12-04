package kotlinx.remote.classes.lease

/**
 * JVM implementation of SystemTimeProvider using System.currentTimeMillis().
 */
actual object SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

