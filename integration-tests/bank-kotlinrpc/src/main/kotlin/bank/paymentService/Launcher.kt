package bank.paymentService

import bank.api.AccountService
import bank.api.PaymentService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.krpc.ktor.client.Krpc as KrpcClient
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main() = runBlocking {
    Database.connect("jdbc:h2:./my_shared_db;AUTO_SERVER=TRUE", driver = "org.h2.Driver")
    transaction { SchemaUtils.create(Payments) }
    val repository = PaymentRepository()

    val httpClient = HttpClient(CIO) { install(KrpcClient) }
    val accountRpcClient = httpClient.rpc {
        url("ws://localhost:8002/api")
        rpcConfig { serialization { json() } }
    }
    val accountService = accountRpcClient.withService<AccountService>()

    embeddedServer(Netty, port = 8001) {
        install(Krpc)
        routing {
            rpc("/api") {
                rpcConfig { serialization { json() } }
                registerService<PaymentService> { ctx -> PaymentServiceImpl(ctx, repository, accountService) }
            }
        }
    }.start(wait = true)
}
