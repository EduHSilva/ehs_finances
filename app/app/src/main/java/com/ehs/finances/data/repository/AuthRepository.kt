package com.ehs.finances.data.repository

import com.ehs.finances.core.network.ApiException
import com.ehs.finances.core.network.apiCall
import com.ehs.finances.core.network.unwrap
import com.ehs.finances.core.session.AuthSession
import com.ehs.finances.core.session.AuthUser
import com.ehs.finances.core.session.SessionManager
import com.ehs.finances.data.remote.AuthApi
import com.ehs.finances.data.remote.LoginDto
import com.ehs.finances.data.remote.LoginRequest
import com.ehs.finances.data.remote.RegisterRequest
import com.ehs.finances.data.remote.UpdateUserRequest
import com.ehs.finances.data.remote.UserDto

/** Autenticação contra o microserviço `users`, espelhando o fluxo do web. */
class AuthRepository(
    private val api: AuthApi,
    private val session: SessionManager,
) {
    val currentSession = session.session

    suspend fun login(email: String, password: String): AuthSession {
        val dto = apiCall {
            api.login(
                LoginRequest(
                    email = email.trim(),
                    password = password,
                    app = APP,
                ),
            ).unwrap()
        }
        return persist(dto)
    }

    suspend fun loginWithGoogle(idToken: String): AuthSession {
        val dto = apiCall {
            api.login(
                LoginRequest(
                    tokenGmail = idToken,
                    app = APP,
                ),
            ).unwrap()
        }
        return persist(dto)
    }

    suspend fun register(email: String, username: String, password: String) {
        apiCall {
            api.register(
                RegisterRequest(
                    email = email.trim(),
                    username = username.trim(),
                    password = password,
                    role = ROLE,
                ),
            )
        }
    }

    suspend fun refreshUser() {
        val dto = apiCall { api.me().unwrap() }
        session.updateUser(dto.toAuthUser())
    }

    suspend fun updateUsername(username: String) {
        val id = session.session.value?.user?.id
            ?: throw ApiException("Sessão expirada. Entre novamente.")
        val dto = apiCall {
            api.updateUser(id, UpdateUserRequest(username.trim())).unwrap()
        }
        session.updateUser(dto.toAuthUser())
    }

    fun logout() = session.expire()

    private fun persist(dto: LoginDto): AuthSession {
        val token = dto.token?.takeIf { it.isNotBlank() }
            ?: throw ApiException("A API não retornou o token de acesso.")
        val auth = AuthSession(token = token, user = dto.toAuthUser())
        session.save(auth)
        return auth
    }

    private fun LoginDto.toAuthUser(): AuthUser = AuthUser(
        id = id,
        name = username.ifBlank { email.substringBefore('@') },
        email = email,
        role = roles.firstOrNull(),
    )

    private fun UserDto.toAuthUser(): AuthUser = AuthUser(
        id = id,
        name = username.ifBlank { email.substringBefore('@') },
        email = email,
        role = roles.firstOrNull(),
    )

    private companion object {
        const val APP = "FINANCES"
        const val ROLE = "FINANCES"
    }
}
