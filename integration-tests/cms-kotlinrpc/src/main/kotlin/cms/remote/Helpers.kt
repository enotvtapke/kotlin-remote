package cms.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.http.URLProtocol
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json

const val CMS_PORT = 8080

fun rpcHttpClient(login: String? = null, password: String? = null): HttpClient = HttpClient(CIO) {
    installKrpc()
    if (login != null && password != null) {
        install(Auth) {
            basic {
                credentials { BasicAuthCredentials(login, password) }
                sendWithoutRequest { it.url.host == "localhost" }
            }
        }
    }
}

suspend fun rpcConn(httpClient: HttpClient, path: String) =
    httpClient.rpc {
        url {
            protocol = URLProtocol.WS
            host = "localhost"
            port = CMS_PORT
            pathSegments = path.split("/").filter { it.isNotEmpty() }
        }
        rpcConfig { serialization { json() } }
    }
