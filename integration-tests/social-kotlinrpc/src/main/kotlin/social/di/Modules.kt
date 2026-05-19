package social.di

import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import social.repository.CommentRepository
import social.repository.FollowRepository
import social.repository.LikeRepository
import social.repository.NotificationRepository
import social.repository.PostRepository
import social.repository.UserRepository

val userModule = module { single { UserRepository() } }
val postModule = module { single { PostRepository() } }
val commentModule = module { single { CommentRepository() } }
val likeModule = module { single { LikeRepository() } }
val followModule = module { single { FollowRepository() } }
val notificationModule = module { single { NotificationRepository() } }
val feedModule = module { }
val searchModule = module { }
val webBffModule = module { }
val mobileBffModule = module { }

inline fun <reified T : Any> dep(): T = GlobalContext.get().get()
