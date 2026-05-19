package bank.paymentService

import bank.api.AccountService
import bank.api.MakePaymentDto
import bank.api.Payment
import bank.api.PaymentService
import kotlin.coroutines.CoroutineContext

class PaymentServiceImpl(
    override val coroutineContext: CoroutineContext,
    private val repository: PaymentRepository,
    private val accountService: AccountService
) : PaymentService {
    override suspend fun makePayment(dto: MakePaymentDto): Payment {
        accountService.transfer(dto)
        return repository.createPayment(dto.amount, dto.payerAccountId, dto.payeeAccountId)
    }

    override suspend fun userPayments(userId: Long): List<Payment> = repository.userPayments(userId)
}
