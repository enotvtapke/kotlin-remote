package bank.accountService

import bank.remote.AccountServiceConfig
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext

object AccountServiceDeps {
    private val accountRepository: AccountRepository = AccountRepositoryImpl()
    private val accountService: AccountService = AccountServiceImpl(accountRepository)

    @Remote
    context(_: RemoteContext<AccountServiceConfig>)
    suspend fun accountService(): AccountService = accountService
}