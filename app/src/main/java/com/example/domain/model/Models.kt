package com.example.domain.model

import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.TransactionWithCategory

data class CategoryBalance(
    val category: CategoryEntity,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentageOfGroup: Double = 0.0
)

data class MonthlyDataPoint(
    val label: String,
    val yearMonth: String,
    val amount: Double,
    val timestamp: Long
)

data class DashboardData(
    val netWorth: Double = 0.0,
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val currentMonthExpenses: Double = 0.0,
    val highestSpendingCategory: CategoryBalance? = null,
    val topAssetCategory: CategoryBalance? = null,
    val recentTransactions: List<TransactionWithCategory> = emptyList(),
    val monthlyExpenseTrend: List<MonthlyDataPoint> = emptyList(),
    val assetDistribution: List<CategoryBalance> = emptyList(),
    val liabilityDistribution: List<CategoryBalance> = emptyList()
)

data class PortfolioData(
    val totalPortfolioValue: Double = 0.0,
    val visibleAssetCategories: List<CategoryBalance> = emptyList(),
    val topAssetCategories: List<CategoryBalance> = emptyList(),
    val nonPortfolioAssetCount: Int = 0,
    val totalAllAssetsValue: Double = 0.0
)

data class ExpenseData(
    val monthlyExpenseTotal: Double = 0.0,
    val highestSpendingCategory: CategoryBalance? = null,
    val categoryWiseSpending: List<CategoryBalance> = emptyList(),
    val dailyAverage: Double = 0.0,
    val expenseTrends: List<MonthlyDataPoint> = emptyList()
)

data class MonthlyReportItem(
    val monthLabel: String,
    val yearMonth: String,
    val totalExpenses: Double,
    val assetInflow: Double,
    val liabilityInflow: Double,
    val netWorthEstimated: Double
)

data class YearlyReportItem(
    val year: Int,
    val totalExpenses: Double,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netWorth: Double
)

data class AnalyticsData(
    val netWorthTrend: List<MonthlyDataPoint> = emptyList(),
    val monthlyExpenseTrends: List<MonthlyDataPoint> = emptyList(),
    val expenseBreakdown: List<CategoryBalance> = emptyList(),
    val assetDistribution: List<CategoryBalance> = emptyList(),
    val liabilityDistribution: List<CategoryBalance> = emptyList(),
    val topExpenseCategories: List<CategoryBalance> = emptyList(),
    val topAssetCategories: List<CategoryBalance> = emptyList(),
    val monthlyComparisonReports: List<MonthlyReportItem> = emptyList(),
    val yearlySummaryReports: List<YearlyReportItem> = emptyList()
)

enum class AnalyticsTimeframe(val label: String) {
    THIS_MONTH("This Month"),
    LAST_3_MONTHS("3 Months"),
    LAST_6_MONTHS("6 Months"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time")
}
