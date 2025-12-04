package kotlinx.remote.classes.lease

import kotlin.js.Date

/**
 * JS implementation of SystemTimeProvider using Date.now().
 */
actual object SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = Date.now().toLong()
}

