package bank.client

import bank.accountService.AccountServiceDeps
import bank.model.dto.CreateAccountDto
import bank.model.dto.MakePaymentDto
import bank.model.dto.UserRegisterDto
import bank.paymentService.PaymentServiceDeps
import bank.remote.AccountServiceConfig
import bank.remote.PaymentServiceConfig
import bank.remote.UserServiceConfig
import bank.userService.UserServiceDeps
import kotlinx.coroutines.runBlocking
import kotlinx.remote.asContext

fun main() = runBlocking {
    val (user1, user2) = context(UserServiceConfig.asContext()) {
        val userService = UserServiceDeps.userService()
        val user1 = userService.register(UserRegisterDto("user1", "password", "John Doe"))
        println(user1)
        val user2 = userService.register(UserRegisterDto("user2", "password", "John Doe"))
        println(user2)
        user1 to user2
    }
    val (account1, account2) = context(AccountServiceConfig.asContext()) {
        AccountServiceDeps.accountService().createAccount(CreateAccountDto(user1.id)) to
                AccountServiceDeps.accountService().createAccount(CreateAccountDto(user2.id))
    }
    context(PaymentServiceConfig.asContext()) {
        val paymentService = PaymentServiceDeps.paymentService()
        val payment = paymentService.makePayment(MakePaymentDto(100.toBigDecimal(), account1.id, account2.id))
        println(payment)
        val user1Payments = paymentService.userPayments(user1.id)
        println(user1Payments)
        val user2Payments = paymentService.userPayments(user2.id)
        println(user2Payments)
    }
    Unit
}