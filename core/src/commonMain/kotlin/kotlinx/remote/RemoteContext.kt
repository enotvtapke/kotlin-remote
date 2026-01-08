package kotlinx.remote

@Target(AnnotationTarget.FUNCTION)
annotation class Remote


interface RemoteContext {
    val client: RemoteClient
}

sealed interface RemoteWrapper <out T: RemoteContext>
object Local: RemoteWrapper<Nothing>
class WrappedRemote<T: RemoteContext>(val context: T): RemoteWrapper<T>

fun <T: RemoteContext> T.wrap() = WrappedRemote(this)

val <T: RemoteContext> T.wrapped
    get() = wrap()
