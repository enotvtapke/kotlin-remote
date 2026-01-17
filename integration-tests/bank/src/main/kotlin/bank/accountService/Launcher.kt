package bank.accountService

import bank.remote.remoteEmbeddedServer
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main() = runBlocking {
    val nodeUrl = "http://localhost:8002"
    Database.connect("jdbc:h2:./my_shared_db;AUTO_SERVER=TRUE", driver = "org.h2.Driver")
    transaction { SchemaUtils.create(Accounts) }
    remoteEmbeddedServer(nodeUrl).start(wait = true)
    Unit
}
