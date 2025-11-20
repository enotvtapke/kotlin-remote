package kotlinx.remote

object CallableMap {
    // This map is generated during compilation
    private val callableMap: MutableMap<String, RemoteCallable> = mutableMapOf(
        // ...
    )

    operator fun get(name: String): RemoteCallable = callableMap[name]
        ?: throw IllegalStateException(
            "Function $name is not registered in CallableMap. Registered functions: ${callableMap.keys.joinToString()}."
        )
    operator fun set(name: String, callable: RemoteCallable) { callableMap[name] = callable }
}