package social.launchers

import org.koin.core.context.startKoin
import social.api.CommentService
import social.di.commentModule
import social.di.dep
import social.remote.COMMENT_PORT
import social.remote.rpcServer
import social.repository.CommentRepository
import social.server.CommentServiceImpl

fun main(): Unit {
    startKoin { modules(commentModule) }
    rpcServer(COMMENT_PORT) {
        registerService<CommentService> { CommentServiceImpl(dep<CommentRepository>()) }
    }.start(wait = true)
}
