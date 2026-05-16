package social.remote

import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteConfig
import kotlinx.remote.asContext

object UserServiceConfig : RemoteConfig { override val client: RemoteClient = remoteClient("http://localhost:8101") }
object PostServiceConfig : RemoteConfig { override val client: RemoteClient = remoteClient("http://localhost:8102") }
object CommentServiceConfig : RemoteConfig { override val client: RemoteClient = remoteClient("http://localhost:8103") }
object LikeServiceConfig : RemoteConfig { override val client: RemoteClient = remoteClient("http://localhost:8104") }
object FollowServiceConfig : RemoteConfig { override val client: RemoteClient = remoteClient("http://localhost:8105") }
object FeedServiceConfig : RemoteConfig { override val client: RemoteClient = remoteClient("http://localhost:8106") }
object NotificationServiceConfig : RemoteConfig { override val client: RemoteClient = remoteClient("http://localhost:8107") }
object SearchServiceConfig : RemoteConfig { override val client: RemoteClient = remoteClient("http://localhost:8108") }
val SearchServiceContext = SearchServiceConfig.asContext()
val NotificationServiceContext = NotificationServiceConfig.asContext()
val FeedServiceContext = FeedServiceConfig.asContext()
val FollowServiceContext = FollowServiceConfig.asContext()
val LikeServiceContext = LikeServiceConfig.asContext()
val CommentServiceContext = CommentServiceConfig.asContext()
val PostServiceContext = PostServiceConfig.asContext()
val UserServiceContext = UserServiceConfig.asContext()
