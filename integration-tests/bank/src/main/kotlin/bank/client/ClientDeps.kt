package bank.client

import bank.accountService.AccountService
import bank.accountService.AccountServiceDeps
import bank.paymentService.PaymentService
import bank.paymentService.PaymentServiceDeps
import bank.remote.AccountServiceConfig
import bank.remote.PaymentServiceConfig
import bank.remote.UserServiceConfig
import bank.userService.UserService
import bank.userService.UserServiceDeps
import kotlinx.remote.asContext

object ClientDeps {
    lateinit var userService: UserService
    lateinit var paymentService: PaymentService
    lateinit var accountService: AccountService

    suspend fun init() {
        userService = context(UserServiceConfig.asContext()) {
            UserServiceDeps.userService()
        }
        paymentService = context(PaymentServiceConfig.asContext()) {
            PaymentServiceDeps.paymentService()
        }
        accountService = context(AccountServiceConfig.asContext()) {
            AccountServiceDeps.accountService()
        }
    }
}