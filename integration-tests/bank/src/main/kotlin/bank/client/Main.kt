package bank.client

import bank.model.Account
import bank.model.User
import bank.model.dto.CreateAccountDto
import bank.model.dto.MakePaymentDto
import bank.model.dto.UserRegisterDto
import bank.remote.AccountServiceConfig
import bank.remote.PaymentServiceConfig
import bank.remote.UserServiceConfig
import kotlinx.coroutines.runBlocking
import kotlinx.remote.asContext
import java.math.BigDecimal

private var currentUser: User? = null

fun main() = runBlocking {
    ClientDeps.init()
    println(
        """
        ╔════════════════════════════════════════════╗
        ║         BANK CLI CLIENT v1.0               ║
        ║     Welcome to the Banking System          ║
        ╚════════════════════════════════════════════╝
        """.trimIndent()
    )

    while (true) {
        printMainMenu()
        when (readInput("Select option")) {
            "1" -> registerUser()
            "2" -> loginUser()
            "3" -> createAccount()
            "4" -> viewMyAccounts()
            "5" -> viewAccountById()
            "6" -> makePayment()
            "7" -> viewMyPayments()
            "8" -> logout()
            "0" -> {
                println("\n👋 Goodbye! Thank you for using Bank CLI.")
                break
            }
            else -> println("\n⚠️  Invalid option. Please try again.")
        }
    }
}

private fun printMainMenu() {
    val userStatus = currentUser?.let { "Logged in as: ${it.login} (ID: ${it.id})" } ?: "Not logged in"
    println(
        """
        
        ┌────────────────────────────────────────────┐
        │  $userStatus
        ├────────────────────────────────────────────┤
        │  USER OPERATIONS                           │
        │    1. Register new user                    │
        │    2. Login                                │
        │                                            │
        │  ACCOUNT OPERATIONS                        │
        │    3. Create account                       │
        │    4. View my accounts                     │
        │    5. View account by ID                   │
        │                                            │
        │  PAYMENT OPERATIONS                        │
        │    6. Make payment                         │
        │    7. View my payments                     │
        │                                            │
        │  OTHER                                     │
        │    8. Logout                               │
        │    0. Exit                                 │
        └────────────────────────────────────────────┘
        """.trimIndent()
    )
}

private fun readInput(prompt: String): String {
    print("$prompt: ")
    return readlnOrNull()?.trim() ?: ""
}

private fun readLongInput(prompt: String): Long? {
    val input = readInput(prompt)
    return input.toLongOrNull().also {
        if (it == null) println("⚠️  Invalid number format.")
    }
}

private fun readBigDecimalInput(prompt: String): BigDecimal? {
    val input = readInput(prompt)
    return try {
        BigDecimal(input)
    } catch (e: NumberFormatException) {
        println("⚠️  Invalid amount format.")
        null
    }
}

private suspend fun registerUser() {
    println("\n📝 USER REGISTRATION")
    println("─".repeat(40))

    val login = readInput("Login")
    if (login.isBlank()) {
        println("⚠️  Login cannot be empty.")
        return
    }

    val password = readInput("Password")
    if (password.isBlank()) {
        println("⚠️  Password cannot be empty.")
        return
    }

    val name = readInput("Full name")
    if (name.isBlank()) {
        println("⚠️  Name cannot be empty.")
        return
    }

    try {
        val user = context(UserServiceConfig.asContext()) {
            ClientDeps.userService.register(UserRegisterDto(login, password, name))
        }
        currentUser = user
        println("\n✅ Registration successful!")
        printUser(user)
    } catch (e: Exception) {
        println("\n❌ Registration failed: ${e.message}")
    }
}

private suspend fun loginUser() {
    println("\n🔐 USER LOGIN")
    println("─".repeat(40))

    val login = readInput("Login")
    if (login.isBlank()) {
        println("⚠️  Login cannot be empty.")
        return
    }

    val password = readInput("Password")
    if (password.isBlank()) {
        println("⚠️  Password cannot be empty.")
        return
    }

    try {
        val user = context(UserServiceConfig.asContext()) {
            ClientDeps.userService.login(login, password)
        }
        currentUser = user
        println("\n✅ Login successful!")
        printUser(user)
    } catch (e: Exception) {
        println("\n❌ Login failed: ${e.message}")
    }
}

private suspend fun createAccount() {
    val user = requireLogin() ?: return

    println("\n🏦 CREATE ACCOUNT")
    println("─".repeat(40))

    try {
        val account = context(AccountServiceConfig.asContext()) {
            ClientDeps.accountService.createAccount(CreateAccountDto(user.id))
        }
        println("\n✅ Account created successfully!")
        printAccount(account)
    } catch (e: Exception) {
        println("\n❌ Failed to create account: ${e.message}")
    }
}

private suspend fun viewMyAccounts() {
    val user = requireLogin() ?: return

    println("\n📋 MY ACCOUNTS")
    println("─".repeat(40))

    try {
        val accounts = context(AccountServiceConfig.asContext()) {
            ClientDeps.accountService.userAccounts(user.id)
        }
        if (accounts.isEmpty()) {
            println("No accounts found. Create one using option 3.")
        } else {
            println("Found ${accounts.size} account(s):\n")
            accounts.forEach { printAccount(it) }
        }
    } catch (e: Exception) {
        println("\n❌ Failed to retrieve accounts: ${e.message}")
    }
}

private suspend fun viewAccountById() {
    println("\n🔍 VIEW ACCOUNT BY ID")
    println("─".repeat(40))

    val accountId = readLongInput("Account ID") ?: return

    try {
        val account = context(AccountServiceConfig.asContext()) {
            ClientDeps.accountService.accountById(accountId)
        }
        println("\n✅ Account found!")
        printAccount(account)
    } catch (e: Exception) {
        println("\n❌ Failed to retrieve account: ${e.message}")
    }
}

private suspend fun makePayment() {
    requireLogin() ?: return

    println("\n💸 MAKE PAYMENT")
    println("─".repeat(40))

    val amount = readBigDecimalInput("Amount") ?: return
    if (amount <= BigDecimal.ZERO) {
        println("⚠️  Amount must be positive.")
        return
    }

    val payerAccountId = readLongInput("Your account ID (payer)") ?: return
    val payeeAccountId = readLongInput("Recipient account ID (payee)") ?: return

    if (payerAccountId == payeeAccountId) {
        println("⚠️  Cannot transfer to the same account.")
        return
    }

    try {
        val payment = context(PaymentServiceConfig.asContext()) {
            ClientDeps.paymentService
                .makePayment(MakePaymentDto(amount, payerAccountId, payeeAccountId))
        }
        println("\n✅ Payment successful!")
        printPayment(payment)
    } catch (e: Exception) {
        println("\n❌ Payment failed: ${e.message}")
    }
}

private suspend fun viewMyPayments() {
    val user = requireLogin() ?: return

    println("\n📜 MY PAYMENT HISTORY")
    println("─".repeat(40))

    try {
        val payments = context(PaymentServiceConfig.asContext()) {
            ClientDeps.paymentService.userPayments(user.id)
        }
        if (payments.isEmpty()) {
            println("No payments found.")
        } else {
            println("Found ${payments.size} payment(s):\n")
            payments.forEach { printPayment(it) }
        }
    } catch (e: Exception) {
        println("\n❌ Failed to retrieve payments: ${e.message}")
    }
}

private fun logout() {
    if (currentUser == null) {
        println("\n⚠️  You are not logged in.")
        return
    }
    println("\n👋 Logged out from ${currentUser?.login}")
    currentUser = null
}

private fun requireLogin(): User? {
    return currentUser ?: run {
        println("\n⚠️  Please login or register first (options 1 or 2).")
        null
    }
}

private fun printUser(user: User) {
    println(
        """
        ┌─ User ─────────────────────────────────────┐
        │  ID:    ${user.id.toString().padEnd(32)}│
        │  Login: ${user.login.padEnd(32)}│
        │  Name:  ${user.name.padEnd(32)}│
        └────────────────────────────────────────────┘
        """.trimIndent()
    )
}

private fun printAccount(account: Account) {
    println(
        """
        ┌─ Account ──────────────────────────────────┐
        │  ID:      ${account.id.toString().padEnd(30)}│
        │  Owner:   ${account.owner.name.padEnd(30)}│
        │  Balance: ${account.balance.toString().padEnd(30)}│
        └────────────────────────────────────────────┘
        """.trimIndent()
    )
}

private fun printPayment(payment: bank.model.Payment) {
    println(
        """
        ┌─ Payment ──────────────────────────────────┐
        │  ID:     ${payment.id.toString().padEnd(31)}│
        │  Amount: ${payment.amount.toString().padEnd(31)}│
        │  From:   Account #${payment.payerAccount.id} (${payment.payerAccount.owner.name})
        │  To:     Account #${payment.payeeAccount.id} (${payment.payeeAccount.owner.name})
        │  Time:   ${payment.time.toString().padEnd(31)}│
        └────────────────────────────────────────────┘
        """.trimIndent()
    )
}
