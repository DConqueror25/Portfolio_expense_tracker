package com.example.ui.screens.transactions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.model.CategoryType
import com.example.data.model.TransactionEntity
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.Formatters
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.AssetGreen
import com.example.ui.theme.ExpenseAmber
import com.example.ui.theme.LiabilityRed
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val activeCategories by viewModel.activeCategories.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val searchQuery by viewModel.transactionSearchQuery.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryIdFilter.collectAsStateWithLifecycle()
    val statementParseState by viewModel.statementParseState.collectAsStateWithLifecycle()

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.parseBankStatement(context, uri)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    // Summary calculations for filtered items
    val totalInflow = filteredTransactions
        .filter { it.category.type == CategoryType.ASSET }
        .sumOf { it.transaction.amount }
    val totalExpense = filteredTransactions
        .filter { it.category.type == CategoryType.EXPENSE }
        .sumOf { it.transaction.amount }
    val totalLiability = filteredTransactions
        .filter { it.category.type == CategoryType.LIABILITY }
        .sumOf { it.transaction.amount }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("transactions_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_transaction_screen")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Screen Title & Import Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Transactions",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Filter, search, and track all entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // PDF Statement Import Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clickable { pdfLauncher.launch(arrayOf("application/pdf")) }
                            .testTag("button_upload_pdf_statement")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Upload PDF",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search by category, notes, amount...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("search_transactions_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            // Type Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { viewModel.setTypeFilter(null) },
                        label = { Text("All Types") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    CategoryType.values().forEach { type ->
                        FilterChip(
                            selected = selectedTypeFilter == type,
                            onClick = {
                                viewModel.setTypeFilter(if (selectedTypeFilter == type) null else type)
                            },
                            label = { Text(type.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            // Summary Banner Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Filtered Entries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${filteredTransactions.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (selectedTypeFilter == null || selectedTypeFilter == CategoryType.EXPENSE) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Formatters.formatCurrency(totalExpense, settings.currencySymbol), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpenseAmber)
                            }
                        }
                        if (selectedTypeFilter == null || selectedTypeFilter == CategoryType.ASSET) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Assets", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Formatters.formatCurrency(totalInflow, settings.currencySymbol), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AssetGreen)
                            }
                        }
                    }
                }
            }

            // Transaction Items List
            if (filteredTransactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No Transactions Found",
                        message = if (searchQuery.isNotEmpty() || selectedTypeFilter != null) {
                            "No transactions match your current search or filter criteria."
                        } else {
                            "No transactions recorded yet. Tap + to add your first transaction."
                        },
                        actionButtonText = if (searchQuery.isNotEmpty() || selectedTypeFilter != null) "Clear Filters" else "Add Transaction",
                        onAction = {
                            if (searchQuery.isNotEmpty() || selectedTypeFilter != null) {
                                viewModel.setSearchQuery("")
                                viewModel.setTypeFilter(null)
                            } else {
                                showAddDialog = true
                            }
                        }
                    )
                }
            } else {
                items(filteredTransactions, key = { it.transaction.id }) { item ->
                    TransactionItemCard(
                        item = item,
                        currencySymbol = settings.currencySymbol,
                        onEdit = { transactionToEdit = item.transaction },
                        onDelete = { transactionToDelete = item.transaction }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    if (showAddDialog) {
        AddEditTransactionDialog(
            activeCategories = activeCategories,
            currencySymbol = settings.currencySymbol,
            onDismiss = { showAddDialog = false },
            onSave = { catId, amount, date, notes, debitSourceCategoryId ->
                viewModel.addTransaction(catId, amount, date, notes, debitSourceCategoryId)
                showAddDialog = false
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
            message = "Are you sure you want to permanently delete this transaction?",
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
