package kotlinx.remote.classes

import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic

object StubIdGenerator {
    private val remoteClassIdCounter: AtomicLong = atomic(0L)
    fun nextId(): Long = remoteClassIdCounter.incrementAndGet()
}