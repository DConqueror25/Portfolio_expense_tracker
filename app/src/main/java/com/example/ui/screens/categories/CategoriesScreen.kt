package com.example.ui.screens.categories

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.ui.components.CategoryIconBox
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.Formatters
import com.example.ui.theme.AssetGreen
import com.example.ui.theme.LiabilityRed
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Assets", "Liabilities", "Expenses", "Archived")

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryTxCountToDelete by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()

    // Calculate dynamic balance for each category from all transactions
    val categoryBalanceMap = remember(allTransactions) {
        val sumMap = mutableMapOf<Long, Double>()
        val countMap = mutableMapOf<Long, Int>()
        for (item in allTransactions) {
            val catId = item.category.id
            sumMap[catId] = sumMap.getOrDefault(catId, 0.0) + item.transaction.amount
            countMap[catId] = countMap.getOrDefault(catId, 0) + 1
        }
        sumMap to countMap
    }

    val displayedCategories = remember(allCategories, selectedTabIndex) {
        when (selectedTabIndex) {
            0 -> allCategories.filter { it.type == CategoryType.ASSET && !it.isArchived }
            1 -> allCategories.filter { it.type == CategoryType.LIABILITY && !it.isArchived }
            2 -> allCategories.filter { it.type == CategoryType.EXPENSE && !it.isArchived }
            3 -> allCategories.filter { it.isArchived }
            else -> emptyList()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("categories_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCategoryDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_category")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Screen Header
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Manage custom Asset, Liability, and Expense groups",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Tabs Row
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth().testTag("categories_tab_row")
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Category List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (displayedCategories.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No ${tabs[selectedTabIndex]} Categories",
                            message = if (selectedTabIndex == 3) {
                                "No archived categories found."
                            } else {
                                "Create a custom ${tabs[selectedTabIndex].dropLast(1)} category to start logging transactions."
                            },
                            actionButtonText = if (selectedTabIndex != 3) "Add ${tabs[selectedTabIndex].dropLast(1)}" else null,
                            onAction = { showAddCategoryDialog = true }
                        )
                    }
                } else {
                    items(displayedCategories, key = { it.id }) { category ->
                        val balance = categoryBalanceMap.first.getOrDefault(category.id, 0.0)
                        val count = categoryBalanceMap.second.getOrDefault(category.id, 0)
                        var menuExpanded by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("category_card_${category.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIconBox(
                                    iconName = category.iconName,
                                    colorHex = category.colorHex,
                                    size = 44
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (category.type == CategoryType.ASSET && category.showInPortfolio) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.PieChart,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = "Portfolio",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$count transactions recorded",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = Formatters.formatCurrency(balance, settings.currencySymbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(category.colorHex)
                                    )
                                    Text(
                                        text = "Balance",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { menuExpanded = true },
                                        modifier = Modifier.testTag("category_menu_${category.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit") },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                categoryToEdit = category
                                            }
                                        )

                                        if (category.type == CategoryType.ASSET) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(if (category.showInPortfolio) "Exclude from Portfolio" else "Include in Portfolio")
                                                },
                                                leadingIcon = { Icon(Icons.Default.PieChart, contentDescription = null) },
                                                onClick = {
                                                    menuExpanded = false
                                                    viewModel.toggleShowInPortfolio(category)
                                                }
                                            )
                                        }

                                        DropdownMenuItem(
                                            text = { Text(if (category.isArchived) "Unarchive" else "Archive") },
                                            leadingIcon = {
                                                Icon(
                                                    if (category.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.toggleArchiveCategory(category)
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("Delete", color = LiabilityRed) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = LiabilityRed) },
                                            onClick = {
                                                menuExpanded = false
                                                scope.launch {
                                                    val txCount = viewModel.getTransactionCountForCategory(category.id)
                                                    categoryTxCountToDelete = txCount
                                                    categoryToDelete = category
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AddEditCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSave = { name, type, showInPort, icon, color ->
                viewModel.addCategory(name, type, showInPort, icon, color)
                showAddCategoryDialog = false
            }
        )
    }

    categoryToEdit?.let { cat ->
        AddEditCategoryDialog(
            initialCategory = cat,
            onDismiss = { categoryToEdit = null },
            onSave = { name, type, showInPort, icon, color ->
                viewModel.updateCategory(
                    cat.copy(
                        name = name,
                        type = type,
                        showInPortfolio = showInPort,
                        iconName = icon,
                        colorHex = color
                    )
                )
                categoryToEdit = null
            }
        )
    }

    categoryToDelete?.let { cat ->
        ConfirmActionDialog(
            title = "Delete Category",
            message = if (categoryTxCountToDelete > 0) {
                "Category '${cat.name}' has $categoryTxCountToDelete associated transactions. Deleting it will also delete all its transactions and update your balances and net worth."
            } else {
                "Are you sure you want to delete category '${cat.name}'?"
            },
            confirmButtonText = "Delete Category",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteCategory(cat, deleteTransactions = true)
                categoryToDelete = null
            },
            onDismiss = { categoryToDelete = null }
        )
    }
}
