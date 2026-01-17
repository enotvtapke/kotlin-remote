package bank.paymentService

import bank.accountService.AccountService
import bank.model.Payment
import bank.model.dto.MakePaymentDto
import bank.remote.AccountServiceConfig
import bank.remote.PaymentServiceConfig
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.asContext
import kotlinx.remote.classes.RemoteSerializable

interface PaymentService {
    context(_: RemoteContext<PaymentServiceConfig>)
    suspend fun makePayment(payment: MakePaymentDto): Payment
    context(_: RemoteContext<PaymentServiceConfig>)
    suspend fun userPayments(userId: Long): List<Payment>
}

@RemoteSerializable
class PaymentServiceImpl(
    private val accountService: AccountService,
    private val paymentRepository: PaymentRepository,
): PaymentService {
    @Remote
    context(_: RemoteContext<PaymentServiceConfig>)
    override suspend fun makePayment(payment: MakePaymentDto): Payment {
        context(AccountServiceConfig.asContext()) {
            accountService.transfer(payment)
        }
        return paymentRepository.createPayment(payment)
    }

    @Remote
    context(_: RemoteContext<PaymentServiceConfig>)
    override suspend fun userPayments(userId: Long): List<Payment> {
        return paymentRepository.userPayments(userId)
    }
}
