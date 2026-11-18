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
    suspend fun register(@Body body: RegisterRequest): Envelope<JsonElement?>

    @GET("users/auth/me")
    suspend fun me(): Envelope<LoginDto>

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body body: UpdateUserRequest): Envelope<UserDto>
}

/** Endpoints do microserviço de finanças, atrás do gateway em `/finances`. */
interface FinancesApi {
    @GET("finances/preferences")
    suspend fun preferences(): Envelope<FinancePreferencesDto>

    @PUT("finances/preferences")
    suspend fun updatePreferences(
        @Body body: UpdateFinancePreferencesRequest,
    ): Envelope<FinancePreferencesDto>

    @GET("finances/accounts")
    suspend fun accounts(): Envelope<List<FinanceAccountDto>>

    @POST("finances/accounts")
    suspend fun createAccount(
        @Body body: CreateFinanceAccountRequest,
    ): Envelope<FinanceAccountDto>

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
        @Query("accountId") accountId: String?,
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

    @POST("finances/transactions/{id}/confirm")
    suspend fun confirmTransaction(
        @Path("id") id: String,
        @Body body: ConfirmValueRequest?,
    ): Envelope<TransactionDto>

    @POST("finances/transactions/import/preview")
    suspend fun previewImport(
        @Body body: StatementImportRequest,
    ): Envelope<StatementImportPreviewDto>

    @POST("finances/transactions/import/confirm")
    suspend fun confirmImport(
        @Body body: StatementImportRequest,
    ): Envelope<StatementImportResultDto>

    @PUT("finances/transactions/{id}/review")
    suspend fun reviewImported(
        @Path("id") id: String,
        @Body body: ReviewImportedTransactionRequest,
    ): Envelope<TransactionDto?>

    @GET("finances/transactions/{id}/import-audit")
    suspend fun importAudit(
        @Path("id") id: String,
    ): Envelope<List<TransactionImportAuditDto>>

    @GET("finances/recurrences")
    suspend fun recurrences(
        @Query("accountId") accountId: String?,
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

    @POST("finances/recurrences/{id}/confirm")
    suspend fun confirmRecurrence(
        @Path("id") id: String,
        @Body body: ConfirmValueRequest?,
    ): Envelope<RecurrenceDto>

    @GET("finances/goals")
    suspend fun goals(
        @Query("accountId") accountId: String,
        @Query("periodMonth") periodMonth: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 200,
    ): Envelope<PageDto<MonthlyGoalDto>>

    @POST("finances/goals")
    suspend fun createGoal(@Body body: MonthlyGoalRequest): Envelope<MonthlyGoalDto>

    @PUT("finances/goals/{id}")
    suspend fun updateGoal(
        @Path("id") id: String,
        @Body body: MonthlyGoalRequest,
    ): Envelope<MonthlyGoalDto>

    @DELETE("finances/goals/{id}")
    suspend fun deleteGoal(@Path("id") id: String): Envelope<JsonElement?>

    @GET("finances/goals/{id}/summary")
    suspend fun goalSummary(
        @Path("id") id: String,
        @Query("periodMonth") periodMonth: String,
    ): Envelope<MonthlyGoalSummaryDto>

    @POST("finances/goals/{id}/contributions")
    suspend fun contributeToGoal(
        @Path("id") id: String,
        @Body body: GoalContributionRequest,
    ): Envelope<JsonElement?>

    @GET("finances/dashboard/projection")
    suspend fun projection(
        @Query("accountId") accountId: String,
        @Query("days") days: Int,
    ): Envelope<CashflowProjectionDto>

    @GET("finances/dashboard/pending")
    suspend fun pending(
        @Query("accountId") accountId: String,
        @Query("days") days: Int,
    ): Envelope<DashboardPendingDto>

    @GET("finances/dashboard/investments")
    suspend fun investments(
        @Query("accountId") accountId: String,
    ): Envelope<InvestmentSummaryDto>

    @GET("finances/accounts/{accountId}/members")
    suspend fun accountMembers(
        @Path("accountId") accountId: String,
    ): Envelope<AccountMembersDto>

    @POST("finances/accounts/{accountId}/members")
    suspend fun inviteAccountMember(
        @Path("accountId") accountId: String,
        @Body body: InviteAccountMemberRequest,
    ): Envelope<AccountMemberDto>

    @POST("finances/accounts/{accountId}/members/{memberId}/resend")
    suspend fun resendAccountInvite(
        @Path("accountId") accountId: String,
        @Path("memberId") memberId: String,
    ): Envelope<AccountMemberDto>

    @DELETE("finances/accounts/{accountId}/members/{memberId}")
    suspend fun revokeAccountMember(
        @Path("accountId") accountId: String,
        @Path("memberId") memberId: String,
    ): Envelope<JsonElement?>

    @GET("finances/account-invites")
    suspend fun accountInvites(): Envelope<List<IncomingAccountInviteDto>>

    @POST("finances/account-invites/{inviteId}/accept")
    suspend fun acceptAccountInvite(
        @Path("inviteId") inviteId: String,
    ): Envelope<JsonElement?>

    @POST("finances/account-invites/{inviteId}/decline")
    suspend fun declineAccountInvite(
        @Path("inviteId") inviteId: String,
    ): Envelope<JsonElement?>
}
