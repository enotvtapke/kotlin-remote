package cms.remote

import kotlinx.remote.RemoteClient
import kotlinx.remote.RemoteConfig

interface GuestConfig : RemoteConfig
interface AuthorConfig : GuestConfig
interface ModeratorConfig : AuthorConfig
interface AdminConfig : ModeratorConfig

object GuestSession : GuestConfig {
    override val client: RemoteClient = guestClient()
}

class AuthorSession(login: String, password: String) : AuthorConfig {
    override val client: RemoteClient = authedClient("/call/author", login, password)
}

class ModeratorSession(login: String, password: String) : ModeratorConfig {
    override val client: RemoteClient = authedClient("/call/moderator", login, password)
}

class AdminSession(login: String, password: String) : AdminConfig {
    override val client: RemoteClient = authedClient("/call/admin", login, password)
}
