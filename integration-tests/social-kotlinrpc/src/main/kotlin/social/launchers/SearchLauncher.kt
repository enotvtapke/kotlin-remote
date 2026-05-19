package social.launchers

import kotlinx.coroutines.runBlocking
import kotlinx.rpc.withService
import org.koin.core.context.startKoin
import social.api.PostService
import social.api.SearchService
import social.api.UserService
import social.di.searchModule
import social.remote.POST_PORT
import social.remote.SEARCH_PORT
import social.remote.USER_PORT
import social.remote.rpcConn
import social.remote.rpcHttpClient
import social.remote.rpcServer
import social.server.SearchServiceImpl

fun main(): Unit = runBlocking {
    startKoin { modules(searchModule) }
    val http = rpcHttpClient()
    val postS = rpcConn(http, POST_PORT).withService<PostService>()
    val userS = rpcConn(http, USER_PORT).withService<UserService>()
    rpcServer(SEARCH_PORT) {
        registerService<SearchService> { SearchServiceImpl(postS, userS) }
    }.start(wait = true)
    Unit
}
