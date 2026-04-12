package proposalExamples

import ServerConfig
import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.runWith

@Remote
context(_: RemoteContext<RemoteConfig>)
suspend fun div(x: Int, y: Int) = x / y

suspend fun main() {
    try {
        ServerConfig.runWith { div(10, 0) }
    } catch (e: ArithmeticException) {
        e.printStackTrace()
    }
}
