package experiments

import ServerConfig
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteConfig
import kotlinx.remote.RemoteContext
import kotlinx.remote.asContext

@Remote
context(_: RemoteContext<RemoteConfig>)
suspend fun fibonacciRecursive(n: Int): Long {
    if (n < 0) {
        throw IllegalArgumentException("n must be non-negative")
    }
    if (n <= 1) {
        return n.toLong()
    }
    return fibonacciRecursive(n - 1) + fibonacciRecursive(n - 2)
}

@Remote
context(_: RemoteContext<RemoteConfig>)
suspend fun isEven(n: Int): Boolean {
    if (n == 0) return true
    return isOdd(n - 1)
}

@Remote
context(_: RemoteContext<RemoteConfig>)
suspend fun isOdd(n: Int): Boolean {
    if (n == 0) return false
    return isEven(n - 1)
}

fun main() = runBlocking {
    context(ServerConfig.asContext()) {
        val n = 20
        val result = fibonacciRecursive(n)
        println("The $n-th Fibonacci number is: $result")
        if (isEven(n)) {
            println("$n is Even")
        } else {
            println("$n is Odd")
        }
    }
}