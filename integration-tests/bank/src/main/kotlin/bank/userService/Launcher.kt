package bank.userService

import bank.remote.remoteEmbeddedServer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun main() {
    val nodeUrl = "http://localhost:8000"
    Database.connect("jdbc:h2:./my_shared_db;AUTO_SERVER=TRUE", driver = "org.h2.Driver")
    transaction { SchemaUtils.create(Users) }
    remoteEmbeddedServer(nodeUrl).start(wait = true)
}
