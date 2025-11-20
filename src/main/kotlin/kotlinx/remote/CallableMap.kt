package kotlinx.remote

object CallableMap {
    // This map is generated during compilation
    private val callableMap: MutableMap<String, RpcCallable> = mutableMapOf(
        // ...
    )

    operator fun get(name: String): RpcCallable = callableMap[name]
        ?: throw IllegalStateException(
            "Function $name is not registered in CallableMap. Registered functions: ${callableMap.keys.joinToString()}."
        )
    operator fun set(name: String, callable: RpcCallable) { callableMap[name] = callable }
}