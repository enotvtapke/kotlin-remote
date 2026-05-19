package bank.api

import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc

@Rpc
interface PaymentService : RemoteService {
    suspend fun makePayment(dto: MakePaymentDto): Payment
    suspend fun userPayments(userId: Long): List<Payment>
}
