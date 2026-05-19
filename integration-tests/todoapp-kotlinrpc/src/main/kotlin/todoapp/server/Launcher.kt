package todoapp.server

import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import todoapp.api.TodoService

fun main() {
    Database.connect("jdbc:h2:./todoapp_db;AUTO_SERVER=TRUE", driver = "org.h2.Driver")
    transaction { SchemaUtils.create(Todos) }
    val repository = TodoRepository()
    embeddedServer(Netty, port = 8000) {
        install(Krpc)
        routing {
            rpc("/api") {
                rpcConfig {
                    serialization { json() }
                }
                registerService<TodoService> { TodoServiceImpl(repository) }
            }
        }
    }.start(wait = true)
}
