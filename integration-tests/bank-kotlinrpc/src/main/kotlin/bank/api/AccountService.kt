package bank.api

import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc

@Rpc
interface AccountService : RemoteService {
    suspend fun createAccount(dto: CreateAccountDto): Account
    suspend fun accountById(id: Long): Account
    suspend fun userAccounts(userId: Long): List<Account>
    suspend fun transfer(dto: MakePaymentDto)
}
