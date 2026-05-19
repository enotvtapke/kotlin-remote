package growthbench

import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteConfig

object GrowthConfig : RemoteConfig {
    override val client: RemoteClient
        get() = error("benchmark stub")
}
