package com.ehs.finances.data.mappers

import com.ehs.finances.core.network.PageDto
import com.ehs.finances.data.model.AccountMember
import com.ehs.finances.data.model.AccountMembers
import com.ehs.finances.data.model.CashflowProjection
import com.ehs.finances.data.model.Category
import com.ehs.finances.data.model.CategoryShare
import com.ehs.finances.data.model.DashboardCash
import com.ehs.finances.data.model.DashboardInvestments
import com.ehs.finances.data.model.DashboardNetWorth
import com.ehs.finances.data.model.DashboardPending
import com.ehs.finances.data.model.DashboardSummary
import com.ehs.finances.data.model.LivingCost
import com.ehs.finances.data.model.FinancePreferences
import com.ehs.finances.data.model.FinancialAccount
import com.ehs.finances.data.model.IncomingAccountInvite
import com.ehs.finances.data.model.InvestmentSummary
import com.ehs.finances.data.model.MonthlyGoal
import com.ehs.finances.data.model.MonthlyGoalSummary
import com.ehs.finances.data.model.Page
import com.ehs.finances.data.model.PendingItem
import com.ehs.finances.data.model.ProjectionItem
import com.ehs.finances.data.model.ProjectionPoint
import com.ehs.finances.data.model.Recurrence
import com.ehs.finances.data.model.Reserve
import com.ehs.finances.data.model.StatementImportPreview
import com.ehs.finances.data.model.StatementImportPreviewLine
import com.ehs.finances.data.model.StatementImportResult
import com.ehs.finances.data.model.Transaction
import com.ehs.finances.data.model.TransactionImportAudit
import com.ehs.finances.data.remote.AccountMemberDto
import com.ehs.finances.data.remote.AccountMembersDto
import com.ehs.finances.data.remote.CashflowProjectionDto
import com.ehs.finances.data.remote.CategoryDto
import com.ehs.finances.data.remote.CategoryShareDto
import com.ehs.finances.data.remote.DashboardCashDto
import com.ehs.finances.data.remote.DashboardInvestmentsDto
import com.ehs.finances.data.remote.DashboardNetWorthDto
import com.ehs.finances.data.remote.DashboardPendingDto
import com.ehs.finances.data.remote.DashboardSummaryDto
import com.ehs.finances.data.remote.LivingCostDto
import com.ehs.finances.data.remote.FinanceAccountDto
import com.ehs.finances.data.remote.FinancePreferencesDto
import com.ehs.finances.data.remote.IncomingAccountInviteDto
import com.ehs.finances.data.remote.InvestmentSummaryDto
import com.ehs.finances.data.remote.MonthlyGoalDto
import com.ehs.finances.data.remote.MonthlyGoalSummaryDto
import com.ehs.finances.data.remote.PendingItemDto
import com.ehs.finances.data.remote.ProjectionItemDto
import com.ehs.finances.data.remote.ProjectionPointDto
import com.ehs.finances.data.remote.RecurrenceDto
import com.ehs.finances.data.remote.ReserveDto
import com.ehs.finances.data.remote.StatementImportPreviewDto
import com.ehs.finances.data.remote.StatementImportPreviewLineDto
import com.ehs.finances.data.remote.StatementImportResultDto
import com.ehs.finances.data.remote.TransactionDto
import com.ehs.finances.data.remote.TransactionImportAuditDto

fun CategoryDto.toModel(): Category = Category(
    id = id,
    title = title,
    color = color,
    goal = goal,
    classification = classification,
    income = income,
    defaultKey = defaultKey,
)

fun TransactionDto.toModel(): Transaction = Transaction(
    id = id,
    title = title,
    value = value,
    income = income,
    executionDate = executionDate,
    status = status,
    categoryId = categoryID,
    category = category,
    categoryKey = categoryKey,
    classification = classification,
    accountId = accountId,
)

fun RecurrenceDto.toModel(): Recurrence = Recurrence(
    id = id,
    title = title,
    value = value,
    income = income,
    type = type,
    day = day,
    categoryId = categoryID,
    category = category,
    categoryKey = categoryKey,
    installments = installments,
    payed = payed,
    status = status,
    accountId = accountId,
)

fun FinanceAccountDto.toModel(): FinancialAccount = FinancialAccount(
    id = id,
    name = name,
    type = type,
    openingBalance = openingBalance,
    balance = balance,
    currency = currency,
    role = role ?: "OWNER",
)

fun AccountMemberDto.toModel(): AccountMember = AccountMember(
    id = id,
    accountId = accountId,
    email = email,
    name = name,
    userId = userId,
    status = status,
    expiresAt = expiresAt,
    acceptedAt = acceptedAt,
    createdAt = createdAt,
)

fun AccountMembersDto.toModel(): AccountMembers = AccountMembers(
    accountId = accountId,
    accountName = accountName,
    ownerId = ownerId,
    ownerName = ownerName,
    ownerEmail = ownerEmail,
    members = members.map { it.toModel() },
)

fun IncomingAccountInviteDto.toModel(): IncomingAccountInvite = IncomingAccountInvite(
    id = id,
    accountId = accountId,
    accountName = accountName,
    ownerId = ownerId,
    ownerName = ownerName,
    ownerEmail = ownerEmail,
    email = email,
    expiresAt = expiresAt,
)

fun FinancePreferencesDto.toModel(): FinancePreferences = FinancePreferences(
    showBusinessAccount = showBusinessAccount,
    currency = currency,
)

fun MonthlyGoalDto.toModel(): MonthlyGoal = MonthlyGoal(
    id = id,
    accountId = accountId,
    title = title,
    mode = mode,
    amount = amount,
    initialBalance = initialBalance,
    objective = objective,
    periodMonth = periodMonth,
    dtInicio = dtInicio,
    dtFim = dtFim,
    recurring = recurring,
    active = active,
    reserve = reserve,
)

fun MonthlyGoalSummaryDto.toModel(): MonthlyGoalSummary = MonthlyGoalSummary(
    goalId = goalId,
    accountId = accountId,
    title = title,
    periodMonth = periodMonth,
    incomeBase = incomeBase,
    expected = expected,
    realized = realized,
    difference = difference,
    progress = progress,
    initialBalance = initialBalance,
    objective = objective,
    saved = saved,
    objectiveProgress = objectiveProgress,
)

fun InvestmentSummaryDto.toModel(): InvestmentSummary = InvestmentSummary(
    accountId = accountId,
    totalContributions = totalContributions,
    totalWithdrawals = totalWithdrawals,
    balance = balance,
    currency = currency,
)

fun DashboardCashDto.toModel(): DashboardCash = DashboardCash(
    currentBalance = currentBalance,
    monthIncome = monthIncome,
    monthExpenses = monthExpenses,
    monthNet = monthNet,
    projectedClosingBalance = projectedClosingBalance,
)

fun DashboardInvestmentsDto.toModel(): DashboardInvestments = DashboardInvestments(
    total = total,
    monthContributions = monthContributions,
    monthWithdrawals = monthWithdrawals,
    monthNet = monthNet,
    projectedClosingBalance = projectedClosingBalance,
)

fun DashboardNetWorthDto.toModel(): DashboardNetWorth = DashboardNetWorth(
    current = current,
    projected = projected,
)

fun LivingCostDto.toModel(): LivingCost = LivingCost(
    monthlyAverage = monthlyAverage,
    months = months,
    total = total,
    reserve = reserve?.toModel(),
)

fun ReserveDto.toModel(): Reserve = Reserve(
    goalId = goalId,
    title = title,
    total = total,
    months = months,
)

fun CategoryShareDto.toModel(): CategoryShare = CategoryShare(
    categoryId = categoryId,
    title = title,
    color = color,
    amount = amount,
    share = share,
)

fun DashboardSummaryDto.toModel(): DashboardSummary = DashboardSummary(
    accountId = accountId,
    horizonDays = horizonDays,
    calculatedAt = calculatedAt,
    cash = cash.toModel(),
    investments = investments.toModel(),
    netWorth = netWorth.toModel(),
    livingCost = livingCost.toModel(),
    topCategories = topCategories.map(CategoryShareDto::toModel),
)

fun PendingItemDto.toModel(): PendingItem = PendingItem(
    id = id,
    kind = kind,
    title = title,
    amount = amount,
    income = income,
    date = date,
    status = status,
)

fun DashboardPendingDto.toModel(): DashboardPending = DashboardPending(
    accountId = accountId,
    horizonDays = horizonDays,
    items = items.map(PendingItemDto::toModel),
)

fun ProjectionPointDto.toModel(): ProjectionPoint = ProjectionPoint(
    date = date,
    balance = balance,
    knownIncome = knownIncome,
    knownExpenses = knownExpenses,
    estimatedIncome = estimatedIncome,
    estimatedExpenses = estimatedExpenses,
)

fun ProjectionItemDto.toModel(): ProjectionItem = ProjectionItem(
    date = date,
    title = title,
    amount = amount,
    income = income,
    kind = kind,
    recurrenceId = recurrenceId,
    installmentNumber = installmentNumber,
    installments = installments,
    payed = payed,
)

fun CashflowProjectionDto.toModel(): CashflowProjection = CashflowProjection(
    accountId = accountId,
    horizonDays = horizonDays,
    calculatedAt = calculatedAt,
    currentBalance = currentBalance,
    closingBalance = closingBalance,
    confidence = confidence,
    assumptions = assumptions,
    series = series.map(ProjectionPointDto::toModel),
    items = items.map(ProjectionItemDto::toModel),
)

fun StatementImportPreviewLineDto.toModel(): StatementImportPreviewLine =
    StatementImportPreviewLine(
        row = row,
        key = key,
        description = description,
        amount = amount,
        income = income,
        date = date,
        duplicate = duplicate,
    )

fun StatementImportPreviewDto.toModel(): StatementImportPreview = StatementImportPreview(
    lines = lines.map(StatementImportPreviewLineDto::toModel),
    importable = importable,
    duplicates = duplicates,
)

fun StatementImportResultDto.toModel(): StatementImportResult = StatementImportResult(
    imported = imported,
    duplicates = duplicates,
)

fun TransactionImportAuditDto.toModel(): TransactionImportAudit = TransactionImportAudit(
    id = id,
    transactionId = transactionId,
    event = event,
    createdAt = createdAt,
)

fun <T, R> PageDto<T>.toModel(map: (T) -> R): Page<R> = Page(
    items = items.map(map),
    total = total,
    page = page,
    size = size,
)
