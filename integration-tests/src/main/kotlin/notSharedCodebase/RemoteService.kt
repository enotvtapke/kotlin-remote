package notSharedCodebase

import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext

interface RemoteService {
    context(_: RemoteContext<RemoteConfig>)
    suspend fun orderPizza(count: Int): Boolean
}