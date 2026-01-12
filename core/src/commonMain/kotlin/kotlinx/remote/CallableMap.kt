package kotlinx.remote

class CallableMap(val callableMap: Map<String, RemoteCallable> = mapOf()) {
    operator fun get(name: String): RemoteCallable = callableMap[name]
        ?: error(
            "Function $name is not registered in CallableMap. Registered functions: ${callableMap.keys.joinToString()}."
        )

    operator fun plus(other: CallableMap): CallableMap = CallableMap(callableMap + other.callableMap)
    override fun toString(): String {
        return callableMap.entries.joinToString("\n")
    }
}

internal val RemoteIntrinsic: Nothing
    get() = throw IllegalStateException("Intrinsic function was called directly")

/**
 * The compiler plugin will replace every call to this function with generated CallableMap
 */
fun genCallableMap(): CallableMap = RemoteIntrinsic
