package com.example.ui.screens.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntity
import com.example.ui.components.ChartSlice
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.DonutPieChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.Formatters
import com.example.ui.components.MonthlyBarChart
import com.example.ui.components.TransactionItemCard
import com.example.ui.screens.transactions.AddEditTransactionDialog
import com.example.ui.screens.transactions.BankStatementImportBottomSheet
import com.example.ui.theme.AssetGreen
import com.example.ui.theme.BentoOnPrimaryContainer
import com.example.ui.theme.BentoPrimaryContainer
import com.example.ui.theme.BentoTagBg
import com.example.ui.theme.BentoTagText
import com.example.ui.theme.LiabilityRed
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dashboardData by viewModel.dashboardData.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activeCategories by viewModel.activeCategories.collectAsStateWithLifecycle()
    val statementParseState by viewModel.statementParseState.collectAsStateWithLifecycle()

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.parseBankStatement(context, uri)
        }
    }

    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("dashboard_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTransactionDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bento Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bento_header"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "YOUR NET WORTH",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Formatters.formatCurrency(dashboardData.netWorth, settings.currencySymbol),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = if (dashboardData.netWorth >= 0) MaterialTheme.colorScheme.primary else LiabilityRed
                        )
                    }

                    // Avatar badge circle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "WT",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            // Bento Card 1: Monthly Summary Card (Lavender Bento Hero Tile)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bento_monthly_summary_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Pill badge tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BentoTagBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = BentoTagText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "MONTHLY SUMMARY",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoTagText,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Text(
                                text = Formatters.formatMonthYear(System.currentTimeMillis()),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = BentoOnPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "Monthly Spending",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BentoOnPrimaryContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Formatters.formatCurrency(dashboardData.currentMonthExpenses, settings.currencySymbol),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BentoOnPrimaryContainer
                                )
                            }

                            // Mini Bar Preview Graph
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.height(44.dp)
                            ) {
                                val bars = if (dashboardData.monthlyExpenseTrend.isNotEmpty()) {
                                    val max = dashboardData.monthlyExpenseTrend.maxOfOrNull { it.amount } ?: 1.0
                                    dashboardData.monthlyExpenseTrend.takeLast(5).map {
                                        ((it.amount / (if (max <= 0) 1.0 else max)) * 36f + 8f).dp
                                    }
                                } else {
                                    listOf(14.dp, 24.dp, 18.dp, 36.dp, 28.dp)
                                }

                                bars.forEachIndexed { index, barHeight ->
                                    Box(
                                        modifier = Modifier
                                            .width(8.dp)
                                            .height(barHeight)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (index == bars.size - 1) BentoTagBg else BentoOnPrimaryContainer.copy(alpha = 0.35f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bento Row 2: 2-Column Bento Stat Cards (Assets & Liabilities)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Assets Bento Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bento_card_assets"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AssetGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "ASSETS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = Formatters.formatCurrency(dashboardData.totalAssets, settings.currencySymbol),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${dashboardData.assetDistribution.size} holdings",
                                style = MaterialTheme.typography.bodySmall,
                                color = AssetGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Liabilities Bento Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bento_card_liabilities"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(LiabilityRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "LIABILITIES",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = Formatters.formatCurrency(dashboardData.totalLiabilities, settings.currencySymbol),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val debtRatio = if (dashboardData.totalAssets > 0) {
                                (dashboardData.totalLiabilities / dashboardData.totalAssets) * 100.0
                            } else 0.0
                            Text(
                                text = String.format(Locale.US, "%.1f%% debt ratio", debtRatio),
                                style = MaterialTheme.typography.bodySmall,
                                color = LiabilityRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Bento Row 3: Quick Action & Shortcut Tiles
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Statement PDF Quick Tile
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { pdfLauncher.launch(arrayOf("application/pdf")) }
                            .testTag("bento_shortcut_import_pdf"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Import PDF",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Bank Statement",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Categories Quick Tile (Lilac Bento Action)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToCategories() }
                            .testTag("bento_shortcut_categories"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Categories",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Manage Rules",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Bento Card 4: Monthly Expense Trend Chart
            if (dashboardData.monthlyExpenseTrend.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bento_spending_trend_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Monthly Spending Trend",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Last 6 Months",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            MonthlyBarChart(
                                dataPoints = dashboardData.monthlyExpenseTrend,
                                currencySymbol = settings.currencySymbol,
                                height = 150.dp,
                                barColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Bento Card 5: Asset Distribution Breakdown
            if (dashboardData.assetDistribution.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bento_asset_distribution_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Asset Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val slices = dashboardData.assetDistribution.map {
                                ChartSlice(
                                    label = it.category.name,
                                    value = it.totalAmount,
                                    percentage = it.percentageOfGroup,
                                    color = Color(it.category.colorHex)
                                )
                            }
                            DonutPieChart(
                                slices = slices,
                                centerTitle = "Total Assets",
                                centerValue = Formatters.formatCurrency(dashboardData.totalAssets, settings.currencySymbol),
                                chartSize = 160.dp,
                                strokeWidth = 28f
                            )
                        }
                    }
                }
            }

            // Bento Card 6: Recent Activity Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onNavigateToTransactions,
                        modifier = Modifier.testTag("button_view_all_transactions")
                    ) {
                        Text("View All", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Recent Transactions List
            if (dashboardData.recentTransactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No Transactions Recorded",
                        message = "Start logging your asset deposits, liability payments, or daily expenses.",
                        actionButtonText = "Add First Transaction",
                        onAction = { showAddTransactionDialog = true }
                    )
                }
            } else {
                items(dashboardData.recentTransactions.take(5), key = { it.transaction.id }) { item ->
                    TransactionItemCard(
                        item = item,
                        currencySymbol = settings.currencySymbol,
                        onEdit = { transactionToEdit = item.transaction },
                        onDelete = { transactionToDelete = item.transaction }
                    )
                }
            }

            // Bottom Spacing for FAB
            item {
                Spacer(modifier = Modifier.height(56.dp))
            }
        }
    }

    // Add / Edit Transaction Dialog
    if (showAddTransactionDialog) {
        AddEditTransactionDialog(
            activeCategories = activeCategories,
            currencySymbol = settings.currencySymbol,
            onDismiss = { showAddTransactionDialog = false },
            onSave = { catId, amount, date, notes, debitSourceCategoryId ->
                viewModel.addTransaction(catId, amount, date, notes, debitSourceCategoryId)
                showAddTransactionDialog = false
            }
        )
    }

    transactionToEdit?.let { tx ->
        AddEditTransactionDialog(
            initialTransaction = tx,
            activeCategories = activeCategories,
            currencySymbol = settings.currencySymbol,
            onDismiss = { transactionToEdit = null },
            onSave = { catId, amount, date, notes, _ ->
                val adjustedAmount = if (tx.amount < 0) -amount else amount
                viewModel.updateTransaction(tx.copy(categoryId = catId, amount = adjustedAmount, date = date, notes = notes))
                transactionToEdit = null
            }
        )
    }

    transactionToDelete?.let { tx ->
        ConfirmActionDialog(
            title = "Delete Transaction",
            message = "Are you sure you want to delete this transaction record? This will immediately recalculate your balances and net worth.",
            confirmButtonText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteTransaction(tx)
                transactionToDelete = null
            },
            onDismiss = { transactionToDelete = null }
        )
    }

    BankStatementImportBottomSheet(
        state = statementParseState,
        activeCategories = activeCategories,
        currencySymbol = settings.currencySymbol,
        onDismiss = { viewModel.dismissStatementDialog() },
        onImport = { transactions, bankAccountId, autoDebit ->
            viewModel.importStatementTransactions(transactions, bankAccountId, autoDebit)
        },
        onRetryUpload = {
            pdfLauncher.launch(arrayOf("application/pdf"))
        }
    )
}

