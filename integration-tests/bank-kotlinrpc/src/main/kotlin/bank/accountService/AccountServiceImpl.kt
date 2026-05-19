package bank.accountService

import bank.api.Account
import bank.api.AccountService
import bank.api.CreateAccountDto
import bank.api.MakePaymentDto
import kotlin.coroutines.CoroutineContext

class AccountServiceImpl(
    override val coroutineContext: CoroutineContext,
    private val repository: AccountRepository
) : AccountService {
    override suspend fun createAccount(dto: CreateAccountDto): Account = repository.createAccount(dto)
    override suspend fun accountById(id: Long): Account = repository.accountById(id)
    override suspend fun userAccounts(userId: Long): List<Account> = repository.userAccounts(userId)
    override suspend fun transfer(dto: MakePaymentDto) =
        repository.transfer(dto.payerAccountId, dto.payeeAccountId, dto.amount)
}
