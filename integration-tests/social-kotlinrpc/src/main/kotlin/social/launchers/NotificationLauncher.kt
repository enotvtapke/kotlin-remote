package social.launchers

import org.koin.core.context.startKoin
import social.api.NotificationService
import social.di.dep
import social.di.notificationModule
import social.remote.NOTIFICATION_PORT
import social.remote.rpcServer
import social.repository.NotificationRepository
import social.server.NotificationServiceImpl

fun main(): Unit {
    startKoin { modules(notificationModule) }
    rpcServer(NOTIFICATION_PORT) {
        registerService<NotificationService> { NotificationServiceImpl(dep<NotificationRepository>()) }
    }.start(wait = true)
}
