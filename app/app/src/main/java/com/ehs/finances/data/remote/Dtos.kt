package com.ehs.finances.data.remote

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Auth (/users)
// ---------------------------------------------------------------------------

@Serializable
data class LoginRequest(
    val email: String? = null,
    val password: String? = null,
    val tokenGmail: String? = null,
)

@Serializable
data class LoginDto(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val lastLogin: String? = null,
    val token: String? = null,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val role: String,
)

@Serializable
data class UserDto(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val createdDate: String? = null,
    val status: String? = null,
    val roles: List<String> = emptyList(),
    val companyName: String? = null,
)

@Serializable
data class UpdateUserRequest(val username: String)

// ---------------------------------------------------------------------------
// Finances (/finances)
// ---------------------------------------------------------------------------

@Serializable
data class CategoryDto(
    val id: String = "",
    val title: String = "",
    val color: String = "#0F766E",
    val goal: Int = 0,
    val classification: String = "ESSENTIAL",
)

@Serializable
data class AddCategoryRequest(
    val title: String,
    val color: String,
    val goal: Int,
    val classification: String,
)

@Serializable
data class UpdateCategoryRequest(
    val title: String,
    val goal: Int,
    val color: String,
)

@Serializable
data class TransactionDto(
    val id: String = "",
    val income: Boolean = false,
    val value: Double = 0.0,
    val category: String = "",
    val classification: String? = null,
    val recurrence: String? = null,
    val executionDate: String? = null,
    val title: String = "",
    val status: String = "OK",
)

@Serializable
data class AddTransactionRequest(
    val income: Boolean,
    val value: Double,
    val categoryID: String,
    val title: String,
    val recurrencyID: String? = null,
    val executionDate: String,
)

@Serializable
data class UpdateTransactionRequest(
    val value: Double,
    val title: String,
    val executionDate: String? = null,
)

@Serializable
data class RecurrenceDto(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val categoryID: String? = null,
    val day: Int = 1,
    val type: String = "MONTH",
    val value: Double = 0.0,
    val income: Boolean = false,
    val installments: Int? = null,
    val payed: Int? = null,
    val status: String = "ACTIVE",
)

@Serializable
data class AddRecurrenceRequest(
    val day: Int,
    val income: Boolean,
    val categoryID: String,
    val type: String,
    val installments: Int? = null,
    val payed: Int? = null,
    val value: Double,
    val title: String,
)

@Serializable
data class UpdateRecurrenceRequest(
    val day: Int,
    val title: String,
    val installments: Int? = null,
    val payed: Int? = null,
    val value: Double,
)
