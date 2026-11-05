package com.ehs.finances.data.remote

import com.ehs.finances.core.network.Envelope
import com.ehs.finances.core.network.PageDto
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApi {
    @POST("users/auth/login")
    suspend fun login(@Body body: LoginRequest): Envelope<LoginDto>

    @POST("users/auth/register")
    suspend fun register(@Body body: RegisterRequest): Envelope<UserDto>

    @GET("users/auth/me")
    suspend fun me(): Envelope<LoginDto>

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body body: UpdateUserRequest): Envelope<UserDto>
}

/** Endpoints do microserviço de finanças, atrás do gateway em `/finances`. */
interface FinancesApi {
    @GET("finances/categories")
    suspend fun categories(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 200,
    ): Envelope<PageDto<CategoryDto>>

    @POST("finances/categories")
    suspend fun createCategory(@Body body: AddCategoryRequest): Envelope<CategoryDto>

    @PUT("finances/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: String,
        @Body body: UpdateCategoryRequest,
    ): Envelope<CategoryDto>

    @DELETE("finances/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Envelope<JsonElement?>

    @GET("finances/transactions")
    suspend fun transactions(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 200,
    ): Envelope<PageDto<TransactionDto>>

    @POST("finances/transactions")
    suspend fun createTransaction(@Body body: AddTransactionRequest): Envelope<TransactionDto>

    @PUT("finances/transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: String,
        @Body body: UpdateTransactionRequest,
    ): Envelope<TransactionDto>

    @DELETE("finances/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String): Envelope<JsonElement?>

    @GET("finances/recurrences")
    suspend fun recurrences(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 200,
    ): Envelope<PageDto<RecurrenceDto>>

    @POST("finances/recurrences")
    suspend fun createRecurrence(@Body body: AddRecurrenceRequest): Envelope<RecurrenceDto>

    @PUT("finances/recurrences/{id}")
    suspend fun updateRecurrence(
        @Path("id") id: String,
        @Body body: UpdateRecurrenceRequest,
    ): Envelope<RecurrenceDto>

    @DELETE("finances/recurrences/{id}")
    suspend fun deleteRecurrence(@Path("id") id: String): Envelope<JsonElement?>
}
