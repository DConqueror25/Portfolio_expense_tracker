package com.example.data.repository

import com.example.data.db.CategoryDao
import com.example.data.db.SettingsDao
import com.example.data.db.TransactionDao
import com.example.data.model.BackupData
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.SettingsEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionWithCategory
import com.example.domain.model.AnalyticsData
import com.example.domain.model.AnalyticsTimeframe
import com.example.domain.model.CategoryBalance
import com.example.domain.model.DashboardData
import com.example.domain.model.ExpenseData
import com.example.domain.model.MonthlyDataPoint
import com.example.domain.model.MonthlyReportItem
import com.example.domain.model.PortfolioData
import com.example.domain.model.YearlyReportItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FinanceRepository(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val settingsDao: SettingsDao
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val activeCategories: Flow<List<CategoryEntity>> = categoryDao.getActiveCategories()
    val allTransactionsWithCategory: Flow<List<TransactionWithCategory>> = transactionDao.getTransactionsWithCategory()
    val settings: Flow<SettingsEntity?> = settingsDao.getSettings()

    // Dynamic Single-Source-of-Truth Dashboard calculation
    val dashboardData: Flow<DashboardData> = combine(
        allCategories,
        allTransactionsWithCategory
    ) { categories, transactions ->
        calculateDashboard(categories, transactions)
    }.flowOn(Dispatchers.Default)

    // Dynamic Single-Source-of-Truth Portfolio calculation
    val portfolioData: Flow<PortfolioData> = combine(
        allCategories,
        allTransactionsWithCategory
    ) { categories, transactions ->
        calculatePortfolio(categories, transactions)
    }.flowOn(Dispatchers.Default)

    // Dynamic Single-Source-of-Truth Expense calculation
    val expenseData: Flow<ExpenseData> = combine(
        allCategories,
        allTransactionsWithCategory
    ) { categories, transactions ->
        calculateExpenseSummary(categories, transactions)
    }.flowOn(Dispatchers.Default)

    fun getAnalyticsData(timeframe: AnalyticsTimeframe): Flow<AnalyticsData> = combine(
        allCategories,
        allTransactionsWithCategory
    ) { categories, transactions ->
        calculateAnalytics(categories, transactions, timeframe)
    }.flowOn(Dispatchers.Default)

    private fun calculateDashboard(
        categories: List<CategoryEntity>,
        transactions: List<TransactionWithCategory>
    ): DashboardData {
        val nonArchivedCategories = categories.filter { !it.isArchived }
        val categoryMap = nonArchivedCategories.associateBy { it.id }

        // Category wise sum
        val categorySumMap = mutableMapOf<Long, Double>()
        val categoryCountMap = mutableMapOf<Long, Int>()

        // For month expenses calculation
        val currentCalendar = Calendar.getInstance()
        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH)

        var currentMonthExpenseTotal = 0.0

        for (item in transactions) {
            val cat = item.category
            if (cat.isArchived) continue

            val currentAmount = categorySumMap.getOrDefault(cat.id, 0.0)
            categorySumMap[cat.id] = currentAmount + item.transaction.amount

            val currentCount = categoryCountMap.getOrDefault(cat.id, 0)
            categoryCountMap[cat.id] = currentCount + 1

            if (cat.type == CategoryType.EXPENSE) {
                val cal = Calendar.getInstance().apply { timeInMillis = item.transaction.date }
                if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth) {
                    currentMonthExpenseTotal += item.transaction.amount
                }
            }
        }

        var totalAssets = 0.0
        var totalLiabilities = 0.0

        val assetBalances = mutableListOf<CategoryBalance>()
        val liabilityBalances = mutableListOf<CategoryBalance>()
        val expenseBalances = mutableListOf<CategoryBalance>()

        for (cat in nonArchivedCategories) {
            val total = categorySumMap.getOrDefault(cat.id, 0.0)
            val count = categoryCountMap.getOrDefault(cat.id, 0)
            val balance = CategoryBalance(category = cat, totalAmount = total, transactionCount = count)

            when (cat.type) {
                CategoryType.ASSET -> {
                    totalAssets += total
                    if (total > 0 || count > 0) assetBalances.add(balance)
                }
                CategoryType.LIABILITY -> {
                    totalLiabilities += total
                    if (total > 0 || count > 0) liabilityBalances.add(balance)
                }
                CategoryType.EXPENSE -> {
                    if (total > 0 || count > 0) expenseBalances.add(balance)
                }
            }
        }

        // Percentage calculations
        val assetDistribution = assetBalances
            .map { it.copy(percentageOfGroup = if (totalAssets > 0) (it.totalAmount / totalAssets) * 100 else 0.0) }
            .sortedByDescending { it.totalAmount }

        val liabilityDistribution = liabilityBalances
            .map { it.copy(percentageOfGroup = if (totalLiabilities > 0) (it.totalAmount / totalLiabilities) * 100 else 0.0) }
            .sortedByDescending { it.totalAmount }

        val netWorth = totalAssets - totalLiabilities

        // Top Asset Category
        val topAsset = assetDistribution.firstOrNull()

        // Highest Spending Category (in current month if available, else overall)
        val currentMonthCategoryExpenses = mutableMapOf<Long, Double>()
        for (item in transactions) {
            if (item.category.type == CategoryType.EXPENSE && !item.category.isArchived) {
                val cal = Calendar.getInstance().apply { timeInMillis = item.transaction.date }
                if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth) {
                    currentMonthCategoryExpenses[item.category.id] =
                        currentMonthCategoryExpenses.getOrDefault(item.category.id, 0.0) + item.transaction.amount
                }
            }
        }

        val highestSpendingCategory = currentMonthCategoryExpenses.maxByOrNull { it.value }?.let { entry ->
            categoryMap[entry.key]?.let { cat ->
                CategoryBalance(
                    category = cat,
                    totalAmount = entry.value,
                    transactionCount = categoryCountMap.getOrDefault(cat.id, 0),
                    percentageOfGroup = if (currentMonthExpenseTotal > 0) (entry.value / currentMonthExpenseTotal) * 100 else 0.0
                )
            }
        } ?: expenseBalances.maxByOrNull { it.totalAmount }

        // Monthly Expense Trend for last 6 months
        val monthlyExpenseTrend = calculateMonthlyExpenseTrend(transactions, 6)

        return DashboardData(
            netWorth = netWorth,
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            currentMonthExpenses = currentMonthExpenseTotal,
            highestSpendingCategory = highestSpendingCategory,
            topAssetCategory = topAsset,
            recentTransactions = transactions.take(10),
            monthlyExpenseTrend = monthlyExpenseTrend,
            assetDistribution = assetDistribution,
            liabilityDistribution = liabilityDistribution
        )
    }

    private fun calculatePortfolio(
        categories: List<CategoryEntity>,
        transactions: List<TransactionWithCategory>
    ): PortfolioData {
        // Portfolio includes ONLY categories where Type == ASSET && ShowInPortfolio == true
        val portfolioCategories = categories.filter {
            it.type == CategoryType.ASSET && it.showInPortfolio && !it.isArchived
        }
        val allAssetCategories = categories.filter { it.type == CategoryType.ASSET && !it.isArchived }

        val categorySumMap = mutableMapOf<Long, Double>()
        val categoryCountMap = mutableMapOf<Long, Int>()

        for (item in transactions) {
            if (item.category.type == CategoryType.ASSET && !item.category.isArchived) {
                val catId = item.category.id
                categorySumMap[catId] = categorySumMap.getOrDefault(catId, 0.0) + item.transaction.amount
                categoryCountMap[catId] = categoryCountMap.getOrDefault(catId, 0) + 1
            }
        }

        var totalPortfolioValue = 0.0
        var totalAllAssetsValue = 0.0

        for (cat in allAssetCategories) {
            val amount = categorySumMap.getOrDefault(cat.id, 0.0)
            totalAllAssetsValue += amount
            if (cat.showInPortfolio) {
                totalPortfolioValue += amount
            }
        }

        val visibleAssetBalances = portfolioCategories.map { cat ->
            val total = categorySumMap.getOrDefault(cat.id, 0.0)
            val count = categoryCountMap.getOrDefault(cat.id, 0)
            val pct = if (totalPortfolioValue > 0) (total / totalPortfolioValue) * 100 else 0.0
            CategoryBalance(
                category = cat,
                totalAmount = total,
                transactionCount = count,
                percentageOfGroup = pct
            )
        }.sortedByDescending { it.totalAmount }

        val nonPortfolioCount = allAssetCategories.count { !it.showInPortfolio }

        return PortfolioData(
            totalPortfolioValue = totalPortfolioValue,
            visibleAssetCategories = visibleAssetBalances,
            topAssetCategories = visibleAssetBalances.take(5),
            nonPortfolioAssetCount = nonPortfolioCount,
            totalAllAssetsValue = totalAllAssetsValue
        )
    }

    private fun calculateExpenseSummary(
        categories: List<CategoryEntity>,
        transactions: List<TransactionWithCategory>
    ): ExpenseData {
        val currentCalendar = Calendar.getInstance()
        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        val daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH)

        val expenseCategoryMap = categories.filter { it.type == CategoryType.EXPENSE && !it.isArchived }.associateBy { it.id }

        val monthlyCategorySums = mutableMapOf<Long, Double>()
        val monthlyCategoryCounts = mutableMapOf<Long, Int>()
        var monthlyTotal = 0.0

        for (item in transactions) {
            if (item.category.type == CategoryType.EXPENSE && !item.category.isArchived) {
                val cal = Calendar.getInstance().apply { timeInMillis = item.transaction.date }
                if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth) {
                    val amount = item.transaction.amount
                    monthlyTotal += amount
                    val catId = item.category.id
                    monthlyCategorySums[catId] = monthlyCategorySums.getOrDefault(catId, 0.0) + amount
                    monthlyCategoryCounts[catId] = monthlyCategoryCounts.getOrDefault(catId, 0) + 1
                }
            }
        }

        val categoryWiseList = expenseCategoryMap.values.map { cat ->
            val amount = monthlyCategorySums.getOrDefault(cat.id, 0.0)
            val count = monthlyCategoryCounts.getOrDefault(cat.id, 0)
            val pct = if (monthlyTotal > 0) (amount / monthlyTotal) * 100 else 0.0
            CategoryBalance(
                category = cat,
                totalAmount = amount,
                transactionCount = count,
                percentageOfGroup = pct
            )
        }.filter { it.totalAmount > 0 || it.transactionCount > 0 }
            .sortedByDescending { it.totalAmount }

        val highestSpending = categoryWiseList.firstOrNull()
        val dailyAvg = if (currentDay > 0) monthlyTotal / currentDay else 0.0
        val trends = calculateMonthlyExpenseTrend(transactions, 6)

        return ExpenseData(
            monthlyExpenseTotal = monthlyTotal,
            highestSpendingCategory = highestSpending,
            categoryWiseSpending = categoryWiseList,
            dailyAverage = dailyAvg,
            expenseTrends = trends
        )
    }

    private fun calculateAnalytics(
        categories: List<CategoryEntity>,
        transactions: List<TransactionWithCategory>,
        timeframe: AnalyticsTimeframe
    ): AnalyticsData {
        val nonArchivedCategories = categories.filter { !it.isArchived }
        val cal = Calendar.getInstance()

        val filterStartTime: Long = when (timeframe) {
            AnalyticsTimeframe.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            AnalyticsTimeframe.LAST_3_MONTHS -> {
                cal.add(Calendar.MONTH, -2)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
            AnalyticsTimeframe.LAST_6_MONTHS -> {
                cal.add(Calendar.MONTH, -5)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
            AnalyticsTimeframe.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            AnalyticsTimeframe.ALL_TIME -> 0L
        }

        val filteredTransactions = if (filterStartTime > 0) {
            transactions.filter { it.transaction.date >= filterStartTime }
        } else {
            transactions
        }

        // Category Wise Totals for Filtered Transactions
        val expenseSums = mutableMapOf<Long, Double>()
        val assetSums = mutableMapOf<Long, Double>()
        val liabilitySums = mutableMapOf<Long, Double>()

        var totalExpenses = 0.0
        var totalAssets = 0.0
        var totalLiabilities = 0.0

        for (item in filteredTransactions) {
            if (item.category.isArchived) continue
            val amount = item.transaction.amount
            val catId = item.category.id
            when (item.category.type) {
                CategoryType.EXPENSE -> {
                    expenseSums[catId] = expenseSums.getOrDefault(catId, 0.0) + amount
                    totalExpenses += amount
                }
                CategoryType.ASSET -> {
                    assetSums[catId] = assetSums.getOrDefault(catId, 0.0) + amount
                    totalAssets += amount
                }
                CategoryType.LIABILITY -> {
                    liabilitySums[catId] = liabilitySums.getOrDefault(catId, 0.0) + amount
                    totalLiabilities += amount
                }
            }
        }

        val expenseBreakdown = nonArchivedCategories
            .filter { it.type == CategoryType.EXPENSE }
            .map { cat ->
                val amount = expenseSums.getOrDefault(cat.id, 0.0)
                CategoryBalance(
                    category = cat,
                    totalAmount = amount,
                    transactionCount = 0,
                    percentageOfGroup = if (totalExpenses > 0) (amount / totalExpenses) * 100 else 0.0
                )
            }.filter { it.totalAmount > 0 }
            .sortedByDescending { it.totalAmount }

        val assetDistribution = nonArchivedCategories
            .filter { it.type == CategoryType.ASSET }
            .map { cat ->
                val amount = assetSums.getOrDefault(cat.id, 0.0)
                CategoryBalance(
                    category = cat,
                    totalAmount = amount,
                    transactionCount = 0,
                    percentageOfGroup = if (totalAssets > 0) (amount / totalAssets) * 100 else 0.0
                )
            }.filter { it.totalAmount > 0 }
            .sortedByDescending { it.totalAmount }

        val liabilityDistribution = nonArchivedCategories
            .filter { it.type == CategoryType.LIABILITY }
            .map { cat ->
                val amount = liabilitySums.getOrDefault(cat.id, 0.0)
                CategoryBalance(
                    category = cat,
                    totalAmount = amount,
                    transactionCount = 0,
                    percentageOfGroup = if (totalLiabilities > 0) (amount / totalLiabilities) * 100 else 0.0
                )
            }.filter { it.totalAmount > 0 }
            .sortedByDescending { it.totalAmount }

        // Monthly trends (12 months)
        val monthCount = when (timeframe) {
            AnalyticsTimeframe.THIS_MONTH -> 1
            AnalyticsTimeframe.LAST_3_MONTHS -> 3
            AnalyticsTimeframe.LAST_6_MONTHS -> 6
            AnalyticsTimeframe.THIS_YEAR -> 12
            AnalyticsTimeframe.ALL_TIME -> 12
        }

        val monthlyExpenseTrends = calculateMonthlyExpenseTrend(transactions, monthCount)
        val netWorthTrend = calculateMonthlyNetWorthTrend(categories, transactions, monthCount)
        val monthlyReports = calculateMonthlyReports(transactions, 12)
        val yearlyReports = calculateYearlyReports(transactions)

        return AnalyticsData(
            netWorthTrend = netWorthTrend,
            monthlyExpenseTrends = monthlyExpenseTrends,
            expenseBreakdown = expenseBreakdown,
            assetDistribution = assetDistribution,
            liabilityDistribution = liabilityDistribution,
            topExpenseCategories = expenseBreakdown.take(5),
            topAssetCategories = assetDistribution.take(5),
            monthlyComparisonReports = monthlyReports,
            yearlySummaryReports = yearlyReports
        )
    }

    private fun calculateMonthlyExpenseTrend(
        transactions: List<TransactionWithCategory>,
        monthCount: Int
    ): List<MonthlyDataPoint> {
        val result = mutableListOf<MonthlyDataPoint>()
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        val calendar = Calendar.getInstance()
        // Start from (monthCount - 1) months ago
        calendar.add(Calendar.MONTH, -(monthCount - 1))
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        for (i in 0 until monthCount) {
            val targetYear = calendar.get(Calendar.YEAR)
            val targetMonth = calendar.get(Calendar.MONTH)
            val label = monthFormat.format(calendar.time)
            val ym = yearMonthFormat.format(calendar.time)
            val timestamp = calendar.timeInMillis

            var total = 0.0
            for (item in transactions) {
                if (item.category.type == CategoryType.EXPENSE && !item.category.isArchived) {
                    val itemCal = Calendar.getInstance().apply { timeInMillis = item.transaction.date }
                    if (itemCal.get(Calendar.YEAR) == targetYear && itemCal.get(Calendar.MONTH) == targetMonth) {
                        total += item.transaction.amount
                    }
                }
            }

            result.add(MonthlyDataPoint(label = label, yearMonth = ym, amount = total, timestamp = timestamp))
            calendar.add(Calendar.MONTH, 1)
        }

        return result
    }

    private fun calculateMonthlyNetWorthTrend(
        categories: List<CategoryEntity>,
        transactions: List<TransactionWithCategory>,
        monthCount: Int
    ): List<MonthlyDataPoint> {
        val result = mutableListOf<MonthlyDataPoint>()
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -(monthCount - 1))

        for (i in 0 until monthCount) {
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val endOfMonthMillis = calendar.timeInMillis
            val label = monthFormat.format(calendar.time)
            val ym = yearMonthFormat.format(calendar.time)

            // Cumulative assets and liabilities up to end of this month
            var assets = 0.0
            var liabilities = 0.0

            for (item in transactions) {
                if (item.transaction.date <= endOfMonthMillis && !item.category.isArchived) {
                    when (item.category.type) {
                        CategoryType.ASSET -> assets += item.transaction.amount
                        CategoryType.LIABILITY -> liabilities += item.transaction.amount
                        CategoryType.EXPENSE -> {}
                    }
                }
            }

            val netWorth = assets - liabilities
            result.add(MonthlyDataPoint(label = label, yearMonth = ym, amount = netWorth, timestamp = endOfMonthMillis))
            calendar.add(Calendar.MONTH, 1)
        }

        return result
    }

    private fun calculateMonthlyReports(
        transactions: List<TransactionWithCategory>,
        monthCount: Int
    ): List<MonthlyReportItem> {
        val result = mutableListOf<MonthlyReportItem>()
        val monthLabelFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val ymFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        val calendar = Calendar.getInstance()
        for (i in 0 until monthCount) {
            val targetYear = calendar.get(Calendar.YEAR)
            val targetMonth = calendar.get(Calendar.MONTH)
            val label = monthLabelFormat.format(calendar.time)
            val ym = ymFormat.format(calendar.time)

            var expenses = 0.0
            var assetInflow = 0.0
            var liabilityInflow = 0.0

            for (item in transactions) {
                val itemCal = Calendar.getInstance().apply { timeInMillis = item.transaction.date }
                if (itemCal.get(Calendar.YEAR) == targetYear && itemCal.get(Calendar.MONTH) == targetMonth && !item.category.isArchived) {
                    when (item.category.type) {
                        CategoryType.EXPENSE -> expenses += item.transaction.amount
                        CategoryType.ASSET -> assetInflow += item.transaction.amount
                        CategoryType.LIABILITY -> liabilityInflow += item.transaction.amount
                    }
                }
            }

            // Estimate net cumulative at end of month
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val endOfMonth = calendar.timeInMillis
            var cumAssets = 0.0
            var cumLiab = 0.0
            for (item in transactions) {
                if (item.transaction.date <= endOfMonth && !item.category.isArchived) {
                    when (item.category.type) {
                        CategoryType.ASSET -> cumAssets += item.transaction.amount
                        CategoryType.LIABILITY -> cumLiab += item.transaction.amount
                        CategoryType.EXPENSE -> {}
                    }
                }
            }

            result.add(
                MonthlyReportItem(
                    monthLabel = label,
                    yearMonth = ym,
                    totalExpenses = expenses,
                    assetInflow = assetInflow,
                    liabilityInflow = liabilityInflow,
                    netWorthEstimated = cumAssets - cumLiab
                )
            )

            calendar.add(Calendar.MONTH, -1)
        }

        return result
    }

    private fun calculateYearlyReports(
        transactions: List<TransactionWithCategory>
    ): List<YearlyReportItem> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 2..currentYear).toList().reversed()

        return years.map { yr ->
            var expenses = 0.0
            var assets = 0.0
            var liab = 0.0

            for (item in transactions) {
                val cal = Calendar.getInstance().apply { timeInMillis = item.transaction.date }
                if (cal.get(Calendar.YEAR) == yr && !item.category.isArchived) {
                    when (item.category.type) {
                        CategoryType.EXPENSE -> expenses += item.transaction.amount
                        CategoryType.ASSET -> assets += item.transaction.amount
                        CategoryType.LIABILITY -> liab += item.transaction.amount
                    }
                }
            }

            YearlyReportItem(
                year = yr,
                totalExpenses = expenses,
                totalAssets = assets,
                totalLiabilities = liab,
                netWorth = assets - liab
            )
        }
    }

    // Category CRUD
    suspend fun insertCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity, deleteTransactions: Boolean = true) = withContext(Dispatchers.IO) {
        if (deleteTransactions) {
            transactionDao.deleteTransactionsByCategoryId(category.id)
        }
        categoryDao.deleteCategory(category)
    }

    suspend fun archiveCategory(id: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
        categoryDao.setArchivedStatus(id, isArchived)
    }

    suspend fun getTransactionCountForCategory(categoryId: Long): Int = withContext(Dispatchers.IO) {
        transactionDao.getTransactionCountForCategory(categoryId)
    }

    // Transaction CRUD
    suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun insertTransactionWithAutoDebit(
        categoryId: Long,
        amount: Double,
        date: Long,
        notes: String,
        debitSourceCategoryId: Long?
    ): Long = withContext(Dispatchers.IO) {
        val mainTransaction = TransactionEntity(
            categoryId = categoryId,
            amount = amount,
            date = date,
            notes = notes.trim()
        )
        val mainId = transactionDao.insertTransaction(mainTransaction)

        if (debitSourceCategoryId != null && debitSourceCategoryId != categoryId && amount > 0) {
            val targetCategory = categoryDao.getCategoryByIdSync(categoryId)
            val targetName = targetCategory?.name ?: "Expense/Asset"
            val autoNote = if (notes.isNotBlank()) {
                "Auto-debit for $targetName: $notes"
            } else {
                "Auto-debit for $targetName"
            }
            val debitTransaction = TransactionEntity(
                categoryId = debitSourceCategoryId,
                amount = -Math.abs(amount),
                date = date,
                notes = autoNote,
                linkedTransactionId = mainId
            )
            val debitId = transactionDao.insertTransaction(debitTransaction)
            transactionDao.updateTransaction(mainTransaction.copy(id = mainId, linkedTransactionId = debitId))
        }
        mainId
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction.copy(updatedTimestamp = System.currentTimeMillis()))
        transaction.linkedTransactionId?.let { linkedId ->
            val linked = transactionDao.getTransactionByIdSync(linkedId)
            if (linked != null) {
                val targetCategory = categoryDao.getCategoryByIdSync(transaction.categoryId)
                val targetName = targetCategory?.name ?: "Expense/Asset"
                val autoNote = if (transaction.notes.isNotBlank()) {
                    "Auto-debit for $targetName: ${transaction.notes}"
                } else {
                    "Auto-debit for $targetName"
                }
                transactionDao.updateTransaction(
                    linked.copy(
                        amount = -Math.abs(transaction.amount),
                        date = transaction.date,
                        notes = autoNote,
                        updatedTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
        transaction.linkedTransactionId?.let { linkedId ->
            transactionDao.deleteTransactionById(linkedId)
        }
    }

    suspend fun deleteTransactionById(id: Long) = withContext(Dispatchers.IO) {
        val tx = transactionDao.getTransactionByIdSync(id)
        if (tx != null) {
            deleteTransaction(tx)
        } else {
            transactionDao.deleteTransactionById(id)
        }
    }

    suspend fun importStatementTransactions(
        selectedTransactions: List<com.example.data.model.ParsedBankTransaction>,
        bankAccountId: Long,
        autoDebitExpensesAndAssets: Boolean = true
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (item in selectedTransactions) {
            if (!item.isSelected || item.amount <= 0) continue

            val category = categoryDao.getCategoryByIdSync(item.categoryId)
            val catType = category?.type ?: (if (item.isDebit) CategoryType.EXPENSE else CategoryType.ASSET)

            if (item.isDebit) {
                val mainTx = TransactionEntity(
                    categoryId = item.categoryId,
                    amount = item.amount,
                    date = item.timestamp,
                    notes = item.description
                )
                val mainId = transactionDao.insertTransaction(mainTx)
                count++

                if (autoDebitExpensesAndAssets && item.categoryId != bankAccountId) {
                    val autoNote = "Auto-debit for ${category?.name ?: "Expense"}: ${item.description}"
                    val debitTx = TransactionEntity(
                        categoryId = bankAccountId,
                        amount = -item.amount,
                        date = item.timestamp,
                        notes = autoNote,
                        linkedTransactionId = mainId
                    )
                    val debitId = transactionDao.insertTransaction(debitTx)
                    transactionDao.updateTransaction(mainTx.copy(id = mainId, linkedTransactionId = debitId))
                }
            } else {
                val targetCatId = if (item.categoryId == bankAccountId || catType == CategoryType.ASSET) {
                    item.categoryId
                } else {
                    bankAccountId
                }
                val creditTx = TransactionEntity(
                    categoryId = targetCatId,
                    amount = item.amount,
                    date = item.timestamp,
                    notes = item.description
                )
                transactionDao.insertTransaction(creditTx)
                count++
            }
        }
        count
    }

    // Settings
    suspend fun updateSettings(settings: SettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.insertOrUpdate(settings)
    }

    suspend fun getSettingsSync(): SettingsEntity = withContext(Dispatchers.IO) {
        settingsDao.getSettingsSync() ?: SettingsEntity()
    }

    // Backup & Restore
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val categories = categoryDao.getAllCategoriesSync()
        val transactions = transactionDao.getAllTransactionsSync()
        val currentSettings = settingsDao.getSettingsSync() ?: SettingsEntity()

        val backupData = BackupData(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            appName = "WealthTracker",
            categories = categories,
            transactions = transactions,
            settings = currentSettings
        )

        val adapter = moshi.adapter(BackupData::class.java).indent("  ")
        val json = adapter.toJson(backupData)

        // update last backup timestamp
        settingsDao.insertOrUpdate(currentSettings.copy(lastBackupTimestamp = System.currentTimeMillis()))
        json
    }

    suspend fun validateAndRestoreBackupJson(jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter(BackupData::class.java)
            val backup = adapter.fromJson(jsonString) ?: return@withContext Result.failure(
                IllegalArgumentException("Invalid backup JSON format or empty content.")
            )

            if (backup.categories.isEmpty() && backup.transactions.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Backup contains no category or transaction records."))
            }

            // Perform atomic restore
            transactionDao.deleteAllTransactions()
            categoryDao.deleteAllCategories()

            if (backup.categories.isNotEmpty()) {
                categoryDao.insertCategories(backup.categories)
            }
            if (backup.transactions.isNotEmpty()) {
                transactionDao.insertTransactions(backup.transactions)
            }
            backup.settings?.let {
                settingsDao.insertOrUpdate(it.copy(lastBackupTimestamp = System.currentTimeMillis()))
            }

            val summary = "Restored ${backup.categories.size} categories and ${backup.transactions.size} transactions."
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Seed Defaults
    suspend fun seedDefaultsIfEmpty() = withContext(Dispatchers.IO) {
        val catCount = categoryDao.getCategoryCount()
        if (catCount == 0) {
            val defaultCategories = listOf(
                // Assets
                CategoryEntity(name = "Bank Accounts", type = CategoryType.ASSET, showInPortfolio = true, iconName = "account_balance", colorHex = 0xFF2563EB),
                CategoryEntity(name = "Mutual Funds", type = CategoryType.ASSET, showInPortfolio = true, iconName = "show_chart", colorHex = 0xFF059669),
                CategoryEntity(name = "Stocks", type = CategoryType.ASSET, showInPortfolio = true, iconName = "trending_up", colorHex = 0xFF10B981),
                CategoryEntity(name = "Gold", type = CategoryType.ASSET, showInPortfolio = true, iconName = "monetization_on", colorHex = 0xFFD97706),
                CategoryEntity(name = "EPF", type = CategoryType.ASSET, showInPortfolio = true, iconName = "savings", colorHex = 0xFF0D9488),
                CategoryEntity(name = "PPF", type = CategoryType.ASSET, showInPortfolio = true, iconName = "security", colorHex = 0xFF0284C7),
                CategoryEntity(name = "Fixed Deposits", type = CategoryType.ASSET, showInPortfolio = true, iconName = "lock_clock", colorHex = 0xFF6366F1),
                CategoryEntity(name = "Cash", type = CategoryType.ASSET, showInPortfolio = false, iconName = "payments", colorHex = 0xFF16A34A),
                CategoryEntity(name = "Crypto", type = CategoryType.ASSET, showInPortfolio = true, iconName = "currency_bitcoin", colorHex = 0xFF8B5CF6),
                CategoryEntity(name = "Chit Funds", type = CategoryType.ASSET, showInPortfolio = true, iconName = "group_work", colorHex = 0xFFEC4899),

                // Liabilities
                CategoryEntity(name = "Home Loan", type = CategoryType.LIABILITY, showInPortfolio = false, iconName = "home", colorHex = 0xFFDC2626),
                CategoryEntity(name = "Vehicle Loan", type = CategoryType.LIABILITY, showInPortfolio = false, iconName = "directions_car", colorHex = 0xFFEA580C),
                CategoryEntity(name = "Personal Loan", type = CategoryType.LIABILITY, showInPortfolio = false, iconName = "person", colorHex = 0xFFF97316),
                CategoryEntity(name = "Credit Card", type = CategoryType.LIABILITY, showInPortfolio = false, iconName = "credit_card", colorHex = 0xFFE11D48),

                // Expenses
                CategoryEntity(name = "Food & Dining", type = CategoryType.EXPENSE, showInPortfolio = false, iconName = "restaurant", colorHex = 0xFFF59E0B),
                CategoryEntity(name = "Medical & Health", type = CategoryType.EXPENSE, showInPortfolio = false, iconName = "local_hospital", colorHex = 0xFFEF4444),
                CategoryEntity(name = "Travel & Trips", type = CategoryType.EXPENSE, showInPortfolio = false, iconName = "flight", colorHex = 0xFF3B82F6),
                CategoryEntity(name = "Shopping", type = CategoryType.EXPENSE, showInPortfolio = false, iconName = "shopping_bag", colorHex = 0xFF8B5CF6),
                CategoryEntity(name = "Transport", type = CategoryType.EXPENSE, showInPortfolio = false, iconName = "directions_bus", colorHex = 0xFF06B6D4),
                CategoryEntity(name = "Internet & Cable", type = CategoryType.EXPENSE, showInPortfolio = false, iconName = "wifi", colorHex = 0xFF6366F1),
                CategoryEntity(name = "Electricity & Utilities", type = CategoryType.EXPENSE, showInPortfolio = false, iconName = "bolt", colorHex = 0xFFEAB308),
                CategoryEntity(name = "Personal Care", type = CategoryType.EXPENSE, showInPortfolio = false, iconName = "face", colorHex = 0xFFEC4899),
                CategoryEntity(name = "Entertainment", type = CategoryType.EXPENSE, showInPortfolio = false, iconName = "movie", colorHex = 0xFFA855F7)
            )
            val ids = categoryDao.insertCategories(defaultCategories)

            // Seed realistic sample starter data for great out-of-the-box user experience
            if (ids.isNotEmpty()) {
                val bankId = ids.getOrNull(0) ?: 1L
                val mutualFundsId = ids.getOrNull(1) ?: 2L
                val stocksId = ids.getOrNull(2) ?: 3L
                val goldId = ids.getOrNull(3) ?: 4L
                val homeLoanId = ids.getOrNull(10) ?: 11L
                val creditCardId = ids.getOrNull(13) ?: 14L
                val foodId = ids.getOrNull(14) ?: 15L
                val shoppingId = ids.getOrNull(17) ?: 18L
                val travelId = ids.getOrNull(16) ?: 17L
                val utilitiesId = ids.getOrNull(20) ?: 21L

                val now = System.currentTimeMillis()
                val oneDay = 86400000L
                val oneMonth = 30 * oneDay

                val sampleTransactions = listOf(
                    // Asset holdings / deposits in INR
                    TransactionEntity(categoryId = bankId, amount = 145000.0, date = now - 2 * oneDay, notes = "Primary checking & savings account balance"),
                    TransactionEntity(categoryId = mutualFundsId, amount = 280000.0, date = now - 15 * oneDay, notes = "Index & balanced equity funds SIP"),
                    TransactionEntity(categoryId = stocksId, amount = 365000.0, date = now - 20 * oneDay, notes = "Nifty 50 & bluechip stock portfolio"),
                    TransactionEntity(categoryId = goldId, amount = 85000.0, date = now - 45 * oneDay, notes = "Physical & sovereign gold bonds"),

                    // Liabilities
                    TransactionEntity(categoryId = homeLoanId, amount = 2200000.0, date = now - 5 * oneDay, notes = "Remaining home loan principal balance"),
                    TransactionEntity(categoryId = creditCardId, amount = 12500.0, date = now - 3 * oneDay, notes = "Monthly statement cycle balance"),

                    // Recent Expenses (this month & last month) in INR
                    TransactionEntity(categoryId = foodId, amount = 4500.0, date = now - oneDay, notes = "Weekly grocery & fresh produce"),
                    TransactionEntity(categoryId = foodId, amount = 1800.0, date = now - 4 * oneDay, notes = "Weekend dinner with family"),
                    TransactionEntity(categoryId = shoppingId, amount = 3200.0, date = now - 6 * oneDay, notes = "Work office essentials & attire"),
                    TransactionEntity(categoryId = travelId, amount = 1200.0, date = now - 9 * oneDay, notes = "Fuel and metro card refill"),
                    TransactionEntity(categoryId = utilitiesId, amount = 999.0, date = now - 12 * oneDay, notes = "High-speed optical fiber broadband"),
                    TransactionEntity(categoryId = foodId, amount = 8500.0, date = now - (oneMonth + 2 * oneDay), notes = "Previous month groceries"),
                    TransactionEntity(categoryId = travelId, amount = 4200.0, date = now - (oneMonth + 10 * oneDay), notes = "Train & intercity transit tickets")
                )
                transactionDao.insertTransactions(sampleTransactions)
            }
        }

        val currentSettings = settingsDao.getSettingsSync()
        if (currentSettings == null) {
            settingsDao.insertOrUpdate(SettingsEntity(currencySymbol = "₹", currencyCode = "INR"))
        } else if (currentSettings.currencySymbol == "$" || currentSettings.currencyCode == "USD") {
            settingsDao.insertOrUpdate(currentSettings.copy(currencySymbol = "₹", currencyCode = "INR"))
        }
    }

    suspend fun resetToDefaultSeed() = withContext(Dispatchers.IO) {
        transactionDao.deleteAllTransactions()
        categoryDao.deleteAllCategories()
        seedDefaultsIfEmpty()
    }
}
