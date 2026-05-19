package bank.userService

import bank.api.UserService
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main() {
    Database.connect("jdbc:h2:./my_shared_db;AUTO_SERVER=TRUE", driver = "org.h2.Driver")
    transaction { SchemaUtils.create(Users) }
    val repository = UserRepository()
    embeddedServer(Netty, port = 8000) {
        install(Krpc)
        routing {
            rpc("/api") {
                rpcConfig { serialization { json() } }
                registerService<UserService> { ctx -> UserServiceImpl(ctx, repository) }
            }
        }
    }.start(wait = true)
}
