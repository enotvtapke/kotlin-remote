import io.ktor.client.HttpClient
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.network.RemoteClient
import kotlinx.remote.network.configureRemote
import kotlinx.remote.network.remoteClient

data object ServerConfig : RemoteConfig {
    override val context = ServerContext
    override val client: RemoteClient = HttpClient { configureRemote("http://localhost:8080") }.remoteClient("/call")
}

data object ServerContext : RemoteContext
data object ClientContext : RemoteContext
