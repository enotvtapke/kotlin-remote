package bank.paymentService

import bank.accountService.AccountService
import bank.accountService.AccountServiceDeps
import bank.remote.AccountServiceConfig
import bank.remote.PaymentServiceConfig
import kotlinx.coroutines.runBlocking
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.asContext

object PaymentServiceDeps {
    val paymentRepository: PaymentRepository = PaymentRepositoryImpl()
    val paymentService: PaymentService by lazy {
        PaymentServiceImpl(
            accountService = accountService,
            paymentRepository = paymentRepository
        )
    }

    val accountService: AccountService by lazy {
        runBlocking {
            context(AccountServiceConfig.asContext()) {
                AccountServiceDeps.accountService()
            }
        }
    }

    @Remote
    context(_: RemoteContext<PaymentServiceConfig>)
    suspend fun paymentService(): PaymentService = paymentService
}