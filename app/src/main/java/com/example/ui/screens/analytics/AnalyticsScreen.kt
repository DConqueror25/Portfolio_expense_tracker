package com.example.ui.screens.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AnalyticsTimeframe
import com.example.ui.components.CategoryIconBox
import com.example.ui.components.ChartSlice
import com.example.ui.components.DonutPieChart
import com.example.ui.components.Formatters
import com.example.ui.components.MonthlyBarChart
import com.example.ui.components.SmoothAreaTrendChart
import com.example.ui.theme.AssetGreen
import com.example.ui.theme.ExpenseAmber
import com.example.ui.theme.LiabilityRed
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val analyticsData by viewModel.analyticsData.collectAsStateWithLifecycle()
    val currentTimeframe by viewModel.analyticsTimeframe.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("analytics_screen")
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Title
            item {
                Column {
                    Text(
                        text = "Financial Analytics",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Interactive trends, category distributions, and reports",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Timeframe Selector Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsTimeframe.values().forEach { timeframe ->
                        val isSelected = currentTimeframe == timeframe
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setAnalyticsTimeframe(timeframe) },
                            label = { Text(timeframe.label) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            // Net Worth Trend Area Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_net_worth_trend"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Net Worth Evolution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AssetGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Cumulative",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AssetGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        SmoothAreaTrendChart(
                            dataPoints = analyticsData.netWorthTrend,
                            currencySymbol = settings.currencySymbol,
                            height = 180.dp,
                            lineColor = Color(0xFF10B981)
                        )
                    }
                }
            }

            // Monthly Expense Trend Bar Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_expense_trends"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Monthly Expense Trends",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        MonthlyBarChart(
                            dataPoints = analyticsData.monthlyExpenseTrends,
                            currencySymbol = settings.currencySymbol,
                            height = 160.dp,
                            barColor = ExpenseAmber
                        )
                    }
                }
            }

            // Expense Breakdown Donut Chart
            if (analyticsData.expenseBreakdown.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("card_expense_breakdown"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Expense Breakdown by Category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            val slices = analyticsData.expenseBreakdown.map {
                                ChartSlice(
                                    label = it.category.name,
                                    value = it.totalAmount,
                                    percentage = it.percentageOfGroup,
                                    color = Color(it.category.colorHex)
                                )
                            }
                            val totalExp = analyticsData.expenseBreakdown.sumOf { it.totalAmount }
                            DonutPieChart(
                                slices = slices,
                                centerTitle = "Period Total",
                                centerValue = Formatters.formatCurrency(totalExp, settings.currencySymbol),
                                chartSize = 170.dp,
                                strokeWidth = 28f
                            )
                        }
                    }
                }
            }

            // Top Expense Categories Ranked List
            if (analyticsData.topExpenseCategories.isNotEmpty()) {
                item {
                    Text(
                        text = "Top Expense Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(analyticsData.topExpenseCategories, key = { "top_exp_${it.category.id}" }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIconBox(
                                    iconName = item.category.iconName,
                                    colorHex = item.category.colorHex,
                                    size = 40
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item.category.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = Formatters.formatCurrency(item.totalAmount, settings.currencySymbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseAmber
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", item.percentageOfGroup)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (item.percentageOfGroup / 100f).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = ExpenseAmber,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            // Asset & Liability Distributions
            if (analyticsData.assetDistribution.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("card_asset_distribution"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Asset Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            val slices = analyticsData.assetDistribution.map {
                                ChartSlice(
                                    label = it.category.name,
                                    value = it.totalAmount,
                                    percentage = it.percentageOfGroup,
                                    color = Color(it.category.colorHex)
                                )
                            }
                            val totalAssets = analyticsData.assetDistribution.sumOf { it.totalAmount }
                            DonutPieChart(
                                slices = slices,
                                centerTitle = "Assets Total",
                                centerValue = Formatters.formatCurrency(totalAssets, settings.currencySymbol),
                                chartSize = 170.dp,
                                strokeWidth = 28f
                            )
                        }
                    }
                }
            }

            // Monthly Comparison Reports Table
            if (analyticsData.monthlyComparisonReports.isNotEmpty()) {
                item {
                    Text(
                        text = "Monthly Comparison Reports",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            analyticsData.monthlyComparisonReports.take(6).forEachIndexed { index, report ->
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = report.monthLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Net: ${Formatters.formatCurrency(report.netWorthEstimated, settings.currencySymbol)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (report.netWorthEstimated >= 0) AssetGreen else LiabilityRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Expenses: ${Formatters.formatCurrency(report.totalExpenses, settings.currencySymbol)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ExpenseAmber
                                        )
                                        Text(
                                            text = "Assets Inflow: ${Formatters.formatCurrency(report.assetInflow, settings.currencySymbol)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AssetGreen
                                        )
                                    }
                                }
                                if (index < 5 && index < analyticsData.monthlyComparisonReports.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Yearly Summary Reports
            if (analyticsData.yearlySummaryReports.isNotEmpty()) {
                item {
                    Text(
                        text = "Yearly Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(analyticsData.yearlySummaryReports, key = { "year_${it.year}" }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Year ${item.year}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Net Worth: ${Formatters.formatCurrency(item.netWorth, settings.currencySymbol)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.netWorth >= 0) AssetGreen else LiabilityRed
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total Expenses: ${Formatters.formatCurrency(item.totalExpenses, settings.currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Assets: ${Formatters.formatCurrency(item.totalAssets, settings.currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AssetGreen
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
