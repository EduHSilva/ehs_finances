package com.ehs.finances.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehs.finances.core.network.ApiException
import com.ehs.finances.core.ui.UiMessage
import com.ehs.finances.data.model.Category
import com.ehs.finances.data.model.StatementImportLineInput
import com.ehs.finances.data.model.StatementImportResult
import com.ehs.finances.data.model.Transaction
import com.ehs.finances.data.repository.FinanceState
import com.ehs.finances.data.repository.FinancesRepository
import com.ehs.finances.domain.StatementFormat
import com.ehs.finances.domain.StatementLineError
import com.ehs.finances.domain.StatementParseErrorCode
import com.ehs.finances.domain.StatementParseException
import com.ehs.finances.domain.StatementParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ImportStep { UPLOAD, PREVIEW, RESULT }

data class ImportRowState(
    val row: Int,
    val description: String,
    val amount: Double?,
    val income: Boolean,
    val date: String?,
    val sourceTransactionId: String?,
    val errors: List<StatementLineError>,
    val include: Boolean,
    val duplicate: Boolean = false,
    val categoryId: String? = null,
) {
    val isValid: Boolean get() = errors.isEmpty()
}

data class ImportUiState(
    val step: ImportStep = ImportStep.UPLOAD,
    val accountId: String? = null,
    val fileName: String = "",
    val format: StatementFormat = StatementFormat.CSV,
    val source: String = "import",
    val rows: List<ImportRowState> = emptyList(),
    val importable: Int = 0,
    val duplicates: Int = 0,
    val result: StatementImportResult? = null,
    val categories: List<Category> = emptyList(),
    val reviewing: List<Transaction> = emptyList(),
    val reviewCategories: Map<String, String?> = emptyMap(),
    val loadingPreview: Boolean = false,
    val confirming: Boolean = false,
    val reviewingId: String? = null,
    val discardTarget: Transaction? = null,
    val reviewOnly: Boolean = false,
) {
    val invalidCount: Int get() = rows.count { !it.isValid }
    val includedCount: Int get() = rows.count { it.isValid && it.include }
    val canConfirm: Boolean get() = includedCount > 0 && !loadingPreview
}

class StatementImportViewModel(
    private val financeState: FinanceState,
    private val repository: FinancesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    fun consumeMessage() {
        _message.value = null
    }

    /** Prepara a tela; em [reviewOnly] abre direto na fila de revisão da importação. */
    fun start(reviewOnly: Boolean) {
        val accountId = financeState.activeAccount.value?.id
        _state.value = _state.value.copy(
            accountId = accountId,
            reviewOnly = reviewOnly,
            step = if (reviewOnly) ImportStep.RESULT else ImportStep.UPLOAD,
        )
        viewModelScope.launch {
            if (_state.value.categories.isEmpty()) {
                runCatching { repository.categories() }.getOrNull()?.let { categories ->
                    _state.value = _state.value.copy(categories = categories)
                }
            }
            if (reviewOnly) loadReviewing()
        }
    }

    fun onFilePicked(fileName: String, bytes: ByteArray?) {
        val accountId = _state.value.accountId
        if (accountId.isNullOrBlank()) {
            _message.value = UiMessage.Res("importAccountRequired")
            return
        }
        if (bytes == null) {
            _message.value = UiMessage.Res("importFileReadError")
            return
        }
        if (bytes.size > MAX_FILE_SIZE) {
            _message.value = UiMessage.Res("importFileTooLarge")
            return
        }
        if (fileName.endsWith(".pdf", ignoreCase = true)) {
            _message.value = UiMessage.Res("importPdfUnsupported")
            return
        }

        val parsed = try {
            StatementParser.parseFile(StatementParser.decode(bytes), fileName)
        } catch (e: StatementParseException) {
            _message.value = UiMessage.Res(
                when (e.code) {
                    StatementParseErrorCode.EMPTY_FILE -> "importParseErrorEmptyFile"
                    StatementParseErrorCode.NO_TRANSACTIONS -> "importParseErrorNoTransactions"
                },
            )
            return
        } catch (e: Exception) {
            _message.value = UiMessage.Res("importFileReadError")
            return
        }

        if (parsed.lines.size > MAX_LINES) {
            _message.value = UiMessage.Res("importLimitReached", mapOf("count" to MAX_LINES))
        }

        _state.value = _state.value.copy(
            fileName = fileName,
            format = parsed.format,
            source = StatementParser.sanitizeSource(fileName),
            step = ImportStep.PREVIEW,
            result = null,
            rows = parsed.lines.take(MAX_LINES).map { line ->
                ImportRowState(
                    row = line.row,
                    description = line.description,
                    amount = line.amount,
                    income = line.income,
                    date = line.date,
                    sourceTransactionId = line.sourceTransactionId,
                    errors = line.errors,
                    include = line.isValid,
                )
            },
        )
        runPreview()
    }

    fun reset() {
        _state.value = _state.value.copy(
            step = if (_state.value.reviewOnly) ImportStep.RESULT else ImportStep.UPLOAD,
            fileName = "",
            rows = emptyList(),
            importable = 0,
            duplicates = 0,
            result = null,
        )
    }

    fun toggleInclude(row: Int) {
        _state.value = _state.value.copy(
            rows = _state.value.rows.map { item ->
                if (item.row == row && item.isValid && !item.duplicate) {
                    item.copy(include = !item.include)
                } else {
                    item
                }
            },
        )
    }

    fun setRowIncome(row: Int, income: Boolean) {
        _state.value = _state.value.copy(
            rows = _state.value.rows.map { item ->
                if (item.row != row) return@map item
                val selected = _state.value.categories.firstOrNull { it.id == item.categoryId }
                val categoryId = if (selected != null && selected.income != income) null else item.categoryId
                item.copy(income = income, categoryId = categoryId)
            },
        )
    }

    fun setRowCategory(row: Int, categoryId: String?) {
        _state.value = _state.value.copy(
            rows = _state.value.rows.map { item ->
                if (item.row == row) item.copy(categoryId = categoryId) else item
            },
        )
    }

    fun confirmImport() {
        val current = _state.value
        val accountId = current.accountId
        if (accountId.isNullOrBlank() || current.confirming || !current.canConfirm) return
        val included = current.rows.filter { it.isValid && it.include }
        viewModelScope.launch {
            _state.value = _state.value.copy(confirming = true)
            try {
                val result = repository.confirmImport(
                    accountId = accountId,
                    source = current.source,
                    lines = included.map { it.toInput() },
                )
                _state.value = _state.value.copy(
                    confirming = false,
                    result = result,
                    step = ImportStep.RESULT,
                )
                loadReviewing()
            } catch (e: ApiException) {
                _state.value = _state.value.copy(confirming = false)
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _state.value = _state.value.copy(confirming = false)
                _message.value = UiMessage.Res("importConfirmFailed")
            }
        }
    }

    fun setReviewCategory(transactionId: String, categoryId: String?) {
        _state.value = _state.value.copy(
            reviewCategories = _state.value.reviewCategories + (transactionId to categoryId),
        )
    }

    fun approve(transactionId: String) {
        if (_state.value.reviewingId != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(reviewingId = transactionId)
            try {
                repository.reviewImportedTransaction(
                    id = transactionId,
                    approved = true,
                    categoryId = _state.value.reviewCategories[transactionId],
                )
                _message.value = UiMessage.Res("importReviewApproved")
                loadReviewing()
            } catch (e: ApiException) {
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _message.value = UiMessage.Res("importReviewFailed")
            } finally {
                _state.value = _state.value.copy(reviewingId = null)
            }
        }
    }

    fun requestDiscard(transaction: Transaction) {
        _state.value = _state.value.copy(discardTarget = transaction)
    }

    fun dismissDiscard() {
        _state.value = _state.value.copy(discardTarget = null)
    }

    fun confirmDiscard() {
        val target = _state.value.discardTarget ?: return
        if (_state.value.reviewingId != null) return
        viewModelScope.launch {
            _state.value = _state.value.copy(reviewingId = target.id)
            try {
                repository.reviewImportedTransaction(target.id, approved = false)
                _state.value = _state.value.copy(discardTarget = null)
                _message.value = UiMessage.Res("importReviewDiscarded")
                loadReviewing()
            } catch (e: ApiException) {
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _message.value = UiMessage.Res("importReviewFailed")
            } finally {
                _state.value = _state.value.copy(reviewingId = null)
            }
        }
    }

    fun refreshReview() {
        viewModelScope.launch { loadReviewing() }
    }

    private fun runPreview() {
        val current = _state.value
        val accountId = current.accountId ?: return
        val validRows = current.rows.filter { it.isValid }
        if (validRows.isEmpty()) {
            _state.value = _state.value.copy(importable = 0, duplicates = 0)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(loadingPreview = true)
            try {
                val preview = repository.previewImport(
                    accountId = accountId,
                    source = current.source,
                    lines = validRows.map { it.toInput() },
                )
                val byRow = preview.lines.associateBy { it.row }
                var validIndex = 0
                val updated = _state.value.rows.map { row ->
                    if (!row.isValid) return@map row.copy(include = false)
                    validIndex++
                    val duplicate = byRow[validIndex]?.duplicate ?: false
                    row.copy(duplicate = duplicate, include = !duplicate)
                }
                _state.value = _state.value.copy(
                    loadingPreview = false,
                    rows = updated,
                    importable = preview.importable,
                    duplicates = preview.duplicates,
                )
            } catch (e: ApiException) {
                _state.value = _state.value.copy(loadingPreview = false, step = ImportStep.UPLOAD)
                _message.value = UiMessage.Text(e.message)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loadingPreview = false, step = ImportStep.UPLOAD)
                _message.value = UiMessage.Res("importPreviewFailed")
            }
        }
    }

    private suspend fun loadReviewing() {
        val accountId = _state.value.accountId ?: return
        try {
            val all = repository.transactions(accountId)
            val reviewing = all.filter { it.status.equals("REVIEW", ignoreCase = true) }
            _state.value = _state.value.copy(
                reviewing = reviewing,
                reviewCategories = reviewing.associate { it.id to it.categoryId },
            )
        } catch (e: ApiException) {
            _message.value = UiMessage.Text(e.message)
        } catch (e: Exception) {
            _message.value = UiMessage.Res("importReviewLoadFailed")
        }
    }

    private fun ImportRowState.toInput(): StatementImportLineInput = StatementImportLineInput(
        description = description,
        amount = amount ?: 0.0,
        income = income,
        date = date.orEmpty(),
        categoryId = categoryId,
        sourceTransactionId = sourceTransactionId,
    )

    private companion object {
        const val MAX_LINES = 1000
        const val MAX_FILE_SIZE = 5 * 1024 * 1024
    }
}
