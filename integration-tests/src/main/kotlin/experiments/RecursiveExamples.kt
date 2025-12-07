package experiments

import ClientContext
import ServerConfig
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext

@Remote(ServerConfig::class)
context(_: RemoteContext)
suspend fun fibonacciRecursive(n: Int): Long {
    if (n < 0) {
        throw IllegalArgumentException("n must be non-negative")
    }
    if (n <= 1) {
        return n.toLong()
    }
    return fibonacciRecursive(n - 1) + fibonacciRecursive(n - 2)
}

@Remote(ServerConfig::class)
context(_: RemoteContext)
suspend fun isEven(n: Int): Boolean {
    if (n == 0) return true
    return isOdd(n - 1)
}

@Remote(ServerConfig::class)
context(_: RemoteContext)
suspend fun isOdd(n: Int): Boolean {
    if (n == 0) return false
    return isEven(n - 1)
}

fun main() = runBlocking {
    context(ClientContext) {
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