package bank.api

import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc

@Rpc
interface UserService : RemoteService {
    suspend fun login(login: String, password: String): User
    suspend fun register(dto: UserRegisterDto): User
}
