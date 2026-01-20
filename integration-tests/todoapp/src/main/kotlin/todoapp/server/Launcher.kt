package todoapp.server

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import todoapp.remote.remoteEmbeddedServer

fun main() {
    Database.connect("jdbc:h2:./todoapp_db;AUTO_SERVER=TRUE", driver = "org.h2.Driver")
    transaction { SchemaUtils.create(Todos) }
    remoteEmbeddedServer("http://localhost:8000").start(wait = true)
}
