package com.ehs.finances.data.mappers

import com.ehs.finances.core.network.PageDto
import com.ehs.finances.data.model.Category
import com.ehs.finances.data.model.Page
import com.ehs.finances.data.model.Recurrence
import com.ehs.finances.data.model.Transaction
import com.ehs.finances.data.remote.CategoryDto
import com.ehs.finances.data.remote.RecurrenceDto
import com.ehs.finances.data.remote.TransactionDto

fun CategoryDto.toModel(): Category = Category(
    id = id,
    title = title,
    color = color,
    goal = goal,
    classification = classification,
)

fun TransactionDto.toModel(): Transaction = Transaction(
    id = id,
    title = title,
    value = value,
    income = income,
    executionDate = executionDate,
    status = status,
    categoryId = null,
    category = category,
    classification = classification,
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
    installments = installments,
    payed = payed,
    status = status,
)

fun <T, R> PageDto<T>.toModel(map: (T) -> R): Page<R> = Page(
    items = items.map(map),
    total = total,
    page = page,
    size = size,
)
