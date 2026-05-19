package todoapp.server

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Todos : LongIdTable("todos") {
    val title = varchar("title", length = 255)
    val done = bool("done").default(false)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}
