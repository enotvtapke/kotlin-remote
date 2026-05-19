package social.launchers

import org.koin.core.context.startKoin
import social.api.PostService
import social.di.dep
import social.di.postModule
import social.remote.POST_PORT
import social.remote.rpcServer
import social.repository.PostRepository
import social.server.PostServiceImpl

fun main(): Unit {
    startKoin { modules(postModule) }
    rpcServer(POST_PORT) {
        registerService<PostService> { PostServiceImpl(dep<PostRepository>()) }
    }.start(wait = true)
}
