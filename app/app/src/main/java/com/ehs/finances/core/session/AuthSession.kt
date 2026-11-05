package com.ehs.finances.core.session

import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String? = null,
)

@Serializable
data class AuthSession(
    val token: String,
    val user: AuthUser,
)
