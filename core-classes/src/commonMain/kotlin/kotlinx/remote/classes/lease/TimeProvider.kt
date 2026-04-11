package kotlinx.remote.classes.lease

import kotlinx.datetime.Clock

internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
