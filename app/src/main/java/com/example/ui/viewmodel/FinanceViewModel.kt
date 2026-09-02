package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.SettingsEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionWithCategory
import com.example.data.repository.FinanceRepository
import com.example.domain.model.AnalyticsData
import com.example.domain.model.AnalyticsTimeframe
import com.example.domain.model.DashboardData
import com.example.domain.model.ExpenseData
import com.example.domain.model.PortfolioData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiMessage {
    data class Success(val text: String) : UiMessage
    data class Error(val text: String) : UiMessage
}

sealed interface StatementParseState {
    data object Idle : StatementParseState
    data object Parsing : StatementParseState
    data class Success(val statement: com.example.data.model.ParsedBankStatement) : StatementParseState
    data class Error(val message: String) : StatementParseState
}

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _uiMessage = MutableSharedFlow<UiMessage>()
    val uiMessage: SharedFlow<UiMessage> = _uiMessage.asSharedFlow()

    val settings: StateFlow<SettingsEntity> = repository.settings
        .map { it ?: SettingsEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsEntity()
        )

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeCategories: StateFlow<List<CategoryEntity>> = repository.activeCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTransactions: StateFlow<List<TransactionWithCategory>> = repository.allTransactionsWithCategory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dashboardData: StateFlow<DashboardData> = repository.dashboardData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardData()
        )

    val portfolioData: StateFlow<PortfolioData> = repository.portfolioData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PortfolioData()
        )

    val expenseData: StateFlow<ExpenseData> = repository.expenseData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExpenseData()
        )

    val analyticsTimeframe = MutableStateFlow(AnalyticsTimeframe.LAST_6_MONTHS)

    val analyticsData: StateFlow<AnalyticsData> = analyticsTimeframe
        .flatMapLatest { timeframe ->
            repository.getAnalyticsData(timeframe)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnalyticsData()
        )

    // Search and Filter States for Transactions Screen
    private val _transactionSearchQuery = MutableStateFlow("")
    val transactionSearchQuery: StateFlow<String> = _transactionSearchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<CategoryType?>(null)
    val selectedTypeFilter: StateFlow<CategoryType?> = _selectedTypeFilter.asStateFlow()

    private val _selectedCategoryIdFilter = MutableStateFlow<Long?>(null)
    val selectedCategoryIdFilter: StateFlow<Long?> = _selectedCategoryIdFilter.asStateFlow()

    private val _statementParseState = MutableStateFlow<StatementParseState>(StatementParseState.Idle)
    val statementParseState: StateFlow<StatementParseState> = _statementParseState.asStateFlow()

    val filteredTransactions: StateFlow<List<TransactionWithCategory>> = combine(
        allTransactions,
        _transactionSearchQuery,
        _selectedTypeFilter,
        _selectedCategoryIdFilter
    ) { transactions, query, typeFilter, categoryIdFilter ->
        transactions.filter { item ->
            val matchesQuery = query.isEmpty() ||
                item.category.name.contains(query, ignoreCase = true) ||
                item.transaction.notes.contains(query, ignoreCase = true) ||
                item.transaction.amount.toString().contains(query)

            val matchesType = typeFilter == null || item.category.type == typeFilter
            val matchesCategory = categoryIdFilter == null || item.category.id == categoryIdFilter

            matchesQuery && matchesType && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.seedDefaultsIfEmpty()
            val current = repository.getSettingsSync()
            if (current.currencySymbol == "$" || current.currencyCode == "USD") {
                repository.updateSettings(current.copy(currencySymbol = "₹", currencyCode = "INR"))
            }
        }
    }

    // Filter updates
    fun setSearchQuery(query: String) {
        _transactionSearchQuery.value = query
    }

    fun setTypeFilter(type: CategoryType?) {
        _selectedTypeFilter.value = type
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryIdFilter.value = categoryId
    }

    fun setAnalyticsTimeframe(timeframe: AnalyticsTimeframe) {
        analyticsTimeframe.value = timeframe
    }

    // Category CRUD
    fun addCategory(
        name: String,
        type: CategoryType,
        showInPortfolio: Boolean,
        iconName: String,
        colorHex: Long
    ) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                _uiMessage.emit(UiMessage.Error("Category name cannot be empty"))
                return@launch
            }
            val category = CategoryEntity(
                name = trimmed,
                type = type,
                showInPortfolio = if (type == CategoryType.ASSET) showInPortfolio else false,
                iconName = iconName,
                colorHex = colorHex
            )
            repository.insertCategory(category)
            _uiMessage.emit(UiMessage.Success("Category '$trimmed' created"))
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            val trimmed = category.name.trim()
            if (trimmed.isEmpty()) {
                _uiMessage.emit(UiMessage.Error("Category name cannot be empty"))
                return@launch
            }
            repository.updateCategory(category.copy(name = trimmed))
            _uiMessage.emit(UiMessage.Success("Category updated"))
        }
    }

    fun deleteCategory(category: CategoryEntity, deleteTransactions: Boolean) {
        viewModelScope.launch {
            repository.deleteCategory(category, deleteTransactions)
            _uiMessage.emit(UiMessage.Success("Category '${category.name}' deleted"))
        }
    }

    fun toggleArchiveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            val newStatus = !category.isArchived
            repository.archiveCategory(category.id, newStatus)
            val msg = if (newStatus) "archived" else "unarchived"
            _uiMessage.emit(UiMessage.Success("Category '${category.name}' $msg"))
        }
    }

    fun toggleShowInPortfolio(category: CategoryEntity) {
        viewModelScope.launch {
            if (category.type != CategoryType.ASSET) return@launch
            val updated = category.copy(showInPortfolio = !category.showInPortfolio)
            repository.updateCategory(updated)
            val msg = if (updated.showInPortfolio) "included in Portfolio" else "excluded from Portfolio"
            _uiMessage.emit(UiMessage.Success("'${category.name}' $msg"))
        }
    }

    suspend fun getTransactionCountForCategory(categoryId: Long): Int {
        return repository.getTransactionCountForCategory(categoryId)
    }

    // Transaction CRUD
    fun addTransaction(
        categoryId: Long,
        amount: Double,
        date: Long,
        notes: String,
        debitSourceCategoryId: Long? = null
    ) {
        viewModelScope.launch {
            if (amount <= 0.0) {
                _uiMessage.emit(UiMessage.Error("Amount must be greater than 0"))
                return@launch
            }
            repository.insertTransactionWithAutoDebit(
                categoryId = categoryId,
                amount = amount,
                date = date,
                notes = notes,
                debitSourceCategoryId = debitSourceCategoryId
            )
            val msg = if (debitSourceCategoryId != null) {
                "Transaction added & amount debited from Bank Account"
            } else {
                "Transaction added successfully"
            }
            _uiMessage.emit(UiMessage.Success(msg))
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            if (transaction.amount <= 0.0) {
                _uiMessage.emit(UiMessage.Error("Amount must be greater than 0"))
                return@launch
            }
            repository.updateTransaction(transaction.copy(notes = transaction.notes.trim()))
            _uiMessage.emit(UiMessage.Success("Transaction updated"))
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _uiMessage.emit(UiMessage.Success("Transaction deleted"))
        }
    }

    // Settings
    fun updateCurrency(symbol: String, code: String) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(currencySymbol = symbol, currencyCode = code))
            _uiMessage.emit(UiMessage.Success("Currency updated to $code ($symbol)"))
        }
    }

    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(themeMode = themeMode))
            _uiMessage.emit(UiMessage.Success("Theme set to $themeMode"))
        }
    }

    fun updateDefaultTimeframe(timeframe: String) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(defaultTimeframe = timeframe))
            _uiMessage.emit(UiMessage.Success("Default timeframe updated"))
        }
    }

    // Backup & Restore
    suspend fun exportBackupJson(): String {
        return repository.exportBackupJson()
    }

    fun restoreBackupJson(jsonContent: String) {
        viewModelScope.launch {
            val result = repository.validateAndRestoreBackupJson(jsonContent)
            if (result.isSuccess) {
                _uiMessage.emit(UiMessage.Success("Backup restored: ${result.getOrNull()}"))
            } else {
                _uiMessage.emit(UiMessage.Error("Restore failed: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}"))
            }
        }
    }

    fun resetToDemoData() {
        viewModelScope.launch {
            repository.resetToDefaultSeed()
            _uiMessage.emit(UiMessage.Success("Sample data & default categories reloaded"))
        }
    }

    // Bank Statement PDF Processing
    fun parseBankStatement(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            _statementParseState.value = StatementParseState.Parsing
            try {
                val service = com.example.data.service.BankStatementPdfService(context)
                val cats = activeCategories.value.ifEmpty { allCategories.value }
                val parsed = service.parseBankStatementPdf(uri, cats)
                if (parsed.transactions.isEmpty()) {
                    _statementParseState.value = StatementParseState.Error("No transactions found in this bank statement PDF.")
                } else {
                    _statementParseState.value = StatementParseState.Success(parsed)
                }
            } catch (e: Exception) {
                _statementParseState.value = StatementParseState.Error(e.localizedMessage ?: "Failed to read PDF")
            }
        }
    }

    fun dismissStatementDialog() {
        _statementParseState.value = StatementParseState.Idle
    }

    fun importStatementTransactions(
        transactions: List<com.example.data.model.ParsedBankTransaction>,
        bankAccountId: Long,
        autoDebit: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                val count = repository.importStatementTransactions(
                    selectedTransactions = transactions,
                    bankAccountId = bankAccountId,
                    autoDebitExpensesAndAssets = autoDebit
                )
                _statementParseState.value = StatementParseState.Idle
                _uiMessage.emit(UiMessage.Success("Successfully imported $count transactions from bank statement!"))
            } catch (e: Exception) {
                _uiMessage.emit(UiMessage.Error("Failed to import transactions: ${e.localizedMessage}"))
            }
        }
    }
}
