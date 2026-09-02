package com.example.ui.screens.transactions

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.TransactionEntity
import com.example.ui.components.CategoryIconBox
import com.example.ui.components.Formatters
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    activeCategories: List<CategoryEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (categoryId: Long, amount: Double, date: Long, notes: String, debitSourceCategoryId: Long?) -> Unit,
    initialTransaction: TransactionEntity? = null
) {
    val isEditing = initialTransaction != null
    val context = LocalContext.current

    val assetCategories = activeCategories.filter { it.type == CategoryType.ASSET }
    val defaultBankCategory = assetCategories.find { 
        it.name.contains("Bank", ignoreCase = true) || it.iconName == "account_balance" 
    } ?: assetCategories.firstOrNull()

    var selectedType by remember {
        mutableStateOf(
            if (initialTransaction != null) {
                activeCategories.find { it.id == initialTransaction.categoryId }?.type ?: CategoryType.EXPENSE
            } else {
                CategoryType.EXPENSE
            }
        )
    }

    val availableCategoriesForType = activeCategories.filter { it.type == selectedType }

    var selectedCategory by remember {
        mutableStateOf(
            if (initialTransaction != null) {
                activeCategories.find { it.id == initialTransaction.categoryId } ?: availableCategoriesForType.firstOrNull()
            } else {
                availableCategoriesForType.firstOrNull()
            }
        )
    }

    var amountText by remember {
        mutableStateOf(if (initialTransaction != null) Math.abs(initialTransaction.amount).toString() else "")
    }
    var notesText by remember {
        mutableStateOf(initialTransaction?.notes ?: "")
    }
    var selectedDateMillis by remember {
        mutableStateOf(initialTransaction?.date ?: System.currentTimeMillis())
    }

    var autoDebitEnabled by remember {
        mutableStateOf(
            if (!isEditing) {
                selectedType == CategoryType.EXPENSE || (selectedType == CategoryType.ASSET && selectedCategory?.id != defaultBankCategory?.id)
            } else false
        )
    }

    var selectedBankCategory by remember {
        mutableStateOf(defaultBankCategory)
    }

    var amountError by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var debitDropdownExpanded by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            selectedDateMillis = cal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val availableDebitCategories = assetCategories.filter { 
        if (selectedType == CategoryType.ASSET) it.id != selectedCategory?.id else true 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Transaction" else "Add Transaction",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category Type Filter Tabs
                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(CategoryType.values()) { type ->
                        val isSelected = selectedType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedType = type
                                val newCats = activeCategories.filter { it.type == type }
                                selectedCategory = newCats.firstOrNull()
                                if (type == CategoryType.EXPENSE) {
                                    autoDebitEnabled = true
                                    selectedBankCategory = defaultBankCategory
                                } else if (type == CategoryType.ASSET) {
                                    autoDebitEnabled = (selectedCategory?.id != defaultBankCategory?.id)
                                    selectedBankCategory = defaultBankCategory
                                } else {
                                    autoDebitEnabled = false
                                }
                            },
                            label = { Text(type.displayName) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Category Selector
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (availableCategoriesForType.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No active ${selectedType.displayName} categories available. Please create one in Categories screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "Select Category",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            leadingIcon = {
                                selectedCategory?.let {
                                    CategoryIconBox(
                                        iconName = it.iconName,
                                        colorHex = it.colorHex,
                                        size = 32
                                    )
                                }
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("transaction_category_selector"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            availableCategoriesForType.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    leadingIcon = {
                                        CategoryIconBox(
                                            iconName = category.iconName,
                                            colorHex = category.colorHex,
                                            size = 30
                                        )
                                    },
                                    onClick = {
                                        selectedCategory = category
                                        categoryDropdownExpanded = false
                                        if (selectedType == CategoryType.ASSET) {
                                            autoDebitEnabled = (category.id != defaultBankCategory?.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Amount Input Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = false
                    },
                    label = { Text("Amount ($currencySymbol)") },
                    placeholder = { Text("0.00") },
                    isError = amountError,
                    supportingText = if (amountError) { { Text("Please enter a valid amount > 0") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_transaction_amount"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Bento Section: Auto-debit from Bank Account Asset
                if (!isEditing && assetCategories.isNotEmpty() && (selectedType == CategoryType.EXPENSE || (selectedType == CategoryType.ASSET && selectedCategory?.id != defaultBankCategory?.id))) {
                    val isAssetPurchase = (selectedType == CategoryType.ASSET)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isAssetPurchase) "Debit from Bank" else "Debit from Bank Account",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isAssetPurchase) "Fund investment from bank" else "Auto-deduct money from asset",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = autoDebitEnabled,
                                    onCheckedChange = { autoDebitEnabled = it },
                                    modifier = Modifier.testTag("switch_auto_debit")
                                )
                            }

                            if (autoDebitEnabled && availableDebitCategories.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Text(
                                    text = "Deduct From Account",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                ExposedDropdownMenuBox(
                                    expanded = debitDropdownExpanded,
                                    onExpandedChange = { debitDropdownExpanded = !debitDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedBankCategory?.name ?: "Select Bank Account",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = debitDropdownExpanded) },
                                        leadingIcon = {
                                            selectedBankCategory?.let {
                                                CategoryIconBox(
                                                    iconName = it.iconName,
                                                    colorHex = it.colorHex,
                                                    size = 26
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                            .testTag("debit_source_selector"),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = debitDropdownExpanded,
                                        onDismissRequest = { debitDropdownExpanded = false }
                                    ) {
                                        availableDebitCategories.forEach { debitCat ->
                                            DropdownMenuItem(
                                                text = { Text(debitCat.name) },
                                                leadingIcon = {
                                                    CategoryIconBox(
                                                        iconName = debitCat.iconName,
                                                        colorHex = debitCat.colorHex,
                                                        size = 24
                                                    )
                                                },
                                                onClick = {
                                                    selectedBankCategory = debitCat
                                                    debitDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                val enteredAmount = amountText.toDoubleOrNull() ?: 0.0
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val targetName = selectedCategory?.name ?: "category"
                                        val bankName = selectedBankCategory?.name ?: "Bank Accounts"
                                        Text(
                                            text = if (enteredAmount > 0) {
                                                if (isAssetPurchase) {
                                                    "${Formatters.formatCurrency(enteredAmount, currencySymbol)} invested in $targetName will be debited from $bankName"
                                                } else {
                                                    "${Formatters.formatCurrency(enteredAmount, currencySymbol)} will be automatically debited from $bankName"
                                                }
                                            } else {
                                                "Amount will be automatically debited from $bankName"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (!isEditing && selectedType == CategoryType.ASSET && selectedCategory?.id == defaultBankCategory?.id) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Direct deposit/credit to ${selectedCategory?.name ?: "Bank Accounts"} (e.g. Salary, Income, Cash deposit)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Date Picker Row
                OutlinedTextField(
                    value = Formatters.formatDate(selectedDateMillis),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                        .testTag("input_transaction_date"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Notes Field
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g., Grocery shopping, SIP deposit...") },
                    modifier = Modifier.fillMaxWidth().testTag("input_transaction_notes"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    val cat = selectedCategory
                    if (amount == null || amount <= 0.0) {
                        amountError = true
                        return@Button
                    }
                    if (cat == null) {
                        return@Button
                    }
                    val debitCategoryId = if (!isEditing && autoDebitEnabled && (selectedType == CategoryType.EXPENSE || (selectedType == CategoryType.ASSET && cat.id != selectedBankCategory?.id))) {
                        selectedBankCategory?.id
                    } else null

                    onSave(cat.id, amount, selectedDateMillis, notesText, debitCategoryId)
                },
                enabled = selectedCategory != null,
                modifier = Modifier.testTag("button_save_transaction")
            ) {
                Text(if (isEditing) "Update" else "Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("button_cancel_transaction")
            ) {
                Text("Cancel")
            }
        }
    )
}
