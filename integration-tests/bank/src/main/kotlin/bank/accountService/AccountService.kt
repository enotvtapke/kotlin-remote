package bank.accountService

import bank.model.Account
import bank.model.dto.CreateAccountDto
import bank.model.dto.MakePaymentDto
import bank.remote.AccountServiceConfig
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.classes.RemoteSerializable

interface AccountService {
    context(_: RemoteContext<AccountServiceConfig>)
    suspend fun accountById(id: Long): Account

    context(_: RemoteContext<AccountServiceConfig>)
    suspend fun userAccounts(userId: Long): List<Account>

    context(_: RemoteContext<AccountServiceConfig>)
    suspend fun transfer(dto: MakePaymentDto)

    context(_: RemoteContext<AccountServiceConfig>)
    suspend fun createAccount(dto: CreateAccountDto): Account
}

@RemoteSerializable
class AccountServiceImpl(private val accountRepository: AccountRepository) : AccountService {
    @Remote
    context(_: RemoteContext<AccountServiceConfig>)
    override suspend fun accountById(id: Long): Account = accountRepository.accountById(id)

    @Remote
    context(_: RemoteContext<AccountServiceConfig>)
    override suspend fun userAccounts(userId: Long): List<Account> = accountRepository.userAccounts(userId)

    @Remote
    context(_: RemoteContext<AccountServiceConfig>)
    override suspend fun transfer(dto: MakePaymentDto) =
        accountRepository.transfer(dto.payerAccountId, dto.payeeAccountId, dto.amount)

    @Remote
    context(_: RemoteContext<AccountServiceConfig>)
    override suspend fun createAccount(dto: CreateAccountDto): Account = accountRepository.createAccount(dto)
}
