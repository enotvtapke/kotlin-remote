package kotlinx.remote

object CallableMap {
    private val callableMap: MutableMap<String, RemoteCallable> = mutableMapOf()

    operator fun get(name: String): RemoteCallable = callableMap[name]
        ?: throw IllegalStateException(
            "Function $name is not registered in CallableMap. Registered functions: ${callableMap.keys.joinToString()}."
        )
    operator fun set(name: String, callable: RemoteCallable) { callableMap[name] = callable }

    fun putAll(newMap: Map<String, RemoteCallable>) { callableMap.putAll(newMap) }
}

/**
 * This function call will be replace with generated CallableMap by compiler plugin
 */
fun genCallableMap(): MutableMap<String, RemoteCallable> = mutableMapOf()
