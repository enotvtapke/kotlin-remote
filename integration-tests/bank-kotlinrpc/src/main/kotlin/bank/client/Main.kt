package bank.client

import bank.api.AccountService
import bank.api.CreateAccountDto
import bank.api.MakePaymentDto
import bank.api.PaymentService
import bank.api.User
import bank.api.UserRegisterDto
import bank.api.UserService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.krpc.ktor.client.Krpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService

private var currentUser: User? = null

fun main() = runBlocking {
    val httpClient = HttpClient(CIO) { install(Krpc) }
    suspend fun <T : kotlinx.rpc.RemoteService> connect(port: Int, fn: (kotlinx.rpc.RpcClient) -> T): T {
        val rpc = httpClient.rpc {
            url("ws://localhost:$port/api")
            rpcConfig { serialization { json() } }
        }
        return fn(rpc)
    }
    val userService = connect(8000) { it.withService<UserService>() }
    val accountService = connect(8002) { it.withService<AccountService>() }
    val paymentService = connect(8001) { it.withService<PaymentService>() }

    println("BANK CLI CLIENT v1.0")
    while (true) {
        printMainMenu()
        when (readInput("Select option")) {
            "1" -> registerUser(userService)
            "2" -> loginUser(userService)
            "3" -> createAccount(accountService)
            "4" -> viewMyAccounts(accountService)
            "5" -> viewAccountById(accountService)
            "6" -> makePayment(paymentService)
            "7" -> viewMyPayments(paymentService)
            "8" -> logout()
            "0" -> { println("Goodbye!"); break }
            else -> println("Invalid option.")
        }
    }
    httpClient.close()
}

private fun printMainMenu() {
    val s = currentUser?.let { "Logged in as: ${it.login} (ID: ${it.id})" } ?: "Not logged in"
    println(
        """
        |$s
        |  1. Register   2. Login
        |  3. Create account   4. View my accounts   5. View account by ID
        |  6. Make payment   7. View my payments
        |  8. Logout   0. Exit
        """.trimMargin()
    )
}

private fun readInput(prompt: String): String {
    print("$prompt: "); return readlnOrNull()?.trim() ?: ""
}

private fun readLong(prompt: String): Long? =
    readInput(prompt).toLongOrNull().also { if (it == null) println("Invalid number.") }

private fun readDecimal(prompt: String): BigDecimal? = try {
    BigDecimal(readInput(prompt))
} catch (_: NumberFormatException) { println("Invalid amount."); null }

private suspend fun registerUser(s: UserService) {
    val login = readInput("Login"); if (login.isBlank()) return
    val password = readInput("Password"); if (password.isBlank()) return
    val name = readInput("Full name"); if (name.isBlank()) return
    try {
        currentUser = s.register(UserRegisterDto(login, password, name))
        println("Registered: ${currentUser?.login}")
    } catch (e: Exception) { println("Error: ${e.message}") }
}

private suspend fun loginUser(s: UserService) {
    val login = readInput("Login"); if (login.isBlank()) return
    val password = readInput("Password"); if (password.isBlank()) return
    try {
        currentUser = s.login(login, password)
        println("Logged in: ${currentUser?.login}")
    } catch (e: Exception) { println("Error: ${e.message}") }
}

private suspend fun createAccount(s: AccountService) {
    val user = requireLogin() ?: return
    try {
        val a = s.createAccount(CreateAccountDto(user.id))
        println("Account #${a.id} created, balance ${a.balance}")
    } catch (e: Exception) { println("Error: ${e.message}") }
}

private suspend fun viewMyAccounts(s: AccountService) {
    val user = requireLogin() ?: return
    try {
        val list = s.userAccounts(user.id)
        if (list.isEmpty()) println("No accounts.")
        else list.forEach { println("Account #${it.id} balance=${it.balance}") }
    } catch (e: Exception) { println("Error: ${e.message}") }
}

private suspend fun viewAccountById(s: AccountService) {
    val id = readLong("Account ID") ?: return
    try {
        val a = s.accountById(id)
        println("Account #${a.id} owner=${a.owner.name} balance=${a.balance}")
    } catch (e: Exception) { println("Error: ${e.message}") }
}

private suspend fun makePayment(s: PaymentService) {
    requireLogin() ?: return
    val amount = readDecimal("Amount") ?: return
    if (amount <= BigDecimal.ZERO) { println("Amount must be positive."); return }
    val payer = readLong("Payer account ID") ?: return
    val payee = readLong("Payee account ID") ?: return
    if (payer == payee) { println("Same account."); return }
    try {
        val p = s.makePayment(MakePaymentDto(amount, payer, payee))
        println("Payment #${p.id} amount=${p.amount}")
    } catch (e: Exception) { println("Error: ${e.message}") }
}

private suspend fun viewMyPayments(s: PaymentService) {
    val user = requireLogin() ?: return
    try {
        val list = s.userPayments(user.id)
        if (list.isEmpty()) println("No payments.")
        else list.forEach { println("Payment #${it.id} amount=${it.amount} from=${it.payerAccount.id} to=${it.payeeAccount.id}") }
    } catch (e: Exception) { println("Error: ${e.message}") }
}

private fun logout() {
    if (currentUser == null) { println("Not logged in."); return }
    println("Logged out from ${currentUser?.login}")
    currentUser = null
}

private fun requireLogin(): User? = currentUser ?: run { println("Please login first."); null }
