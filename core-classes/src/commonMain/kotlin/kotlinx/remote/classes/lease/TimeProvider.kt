package kotlinx.remote.classes.lease

import kotlinx.datetime.Clock

fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
