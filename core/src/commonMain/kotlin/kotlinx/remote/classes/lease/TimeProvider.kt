package kotlinx.remote.classes.lease

import kotlinx.datetime.Clock

fun interface TimeProvider {
    fun currentTimeMillis(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
