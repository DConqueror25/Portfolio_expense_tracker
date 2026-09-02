package com.example.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.ui.components.getCategoryIcon

val availableCategoryIcons = listOf(
    "account_balance", "show_chart", "trending_up", "monetization_on",
    "savings", "security", "lock_clock", "payments",
    "currency_bitcoin", "group_work", "home", "directions_car",
    "person", "credit_card", "restaurant", "local_hospital",
    "flight", "shopping_bag", "directions_bus", "wifi",
    "bolt", "face", "movie", "category"
)

val availableCategoryColors = listOf(
    0xFF10B981, // Emerald
    0xFF059669, // Green
    0xFF2563EB, // Blue
    0xFF0284C7, // Sky
    0xFF8B5CF6, // Purple
    0xFFEC4899, // Pink
    0xFFD97706, // Amber
    0xFFF59E0B, // Gold
    0xFFEF4444, // Red
    0xFFDC2626, // Crimson
    0xFF0D9488, // Teal
    0xFF6366F1  // Indigo
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, type: CategoryType, showInPortfolio: Boolean, iconName: String, colorHex: Long) -> Unit,
    initialCategory: CategoryEntity? = null
) {
    val isEditing = initialCategory != null

    var nameText by remember { mutableStateOf(initialCategory?.name ?: "") }
    var selectedType by remember { mutableStateOf(initialCategory?.type ?: CategoryType.EXPENSE) }
    var showInPortfolio by remember { mutableStateOf(initialCategory?.showInPortfolio ?: (selectedType == CategoryType.ASSET)) }
    var selectedIcon by remember { mutableStateOf(initialCategory?.iconName ?: "category") }
    var selectedColorHex by remember { mutableStateOf(initialCategory?.colorHex ?: 0xFF10B981) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Category" else "New Category",
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
                // Category Name
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        nameError = false
                    },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g., Dividend Stocks, Gym, Dining") },
                    isError = nameError,
                    supportingText = if (nameError) { { Text("Please enter a category name") } } else null,
                    modifier = Modifier.fillMaxWidth().testTag("input_category_name"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Category Type Selector
                if (!isEditing) {
                    Text(
                        text = "Category Type",
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
                                    showInPortfolio = (type == CategoryType.ASSET)
                                },
                                label = { Text(type.displayName) },
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

                // Show in Portfolio toggle for Assets
                if (selectedType == CategoryType.ASSET) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Include in Portfolio",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Track this asset in the Portfolio analytics screen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showInPortfolio,
                            onCheckedChange = { showInPortfolio = it },
                            modifier = Modifier.testTag("switch_show_in_portfolio")
                        )
                    }
                }

                // Icon Selector
                Text(
                    text = "Select Icon",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableCategoryIcons.forEach { iconName ->
                        val isSelected = selectedIcon == iconName
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIcon = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(iconName),
                                contentDescription = iconName,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Color Selector
                Text(
                    text = "Select Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableCategoryColors.forEach { colorHex ->
                        val isSelected = selectedColorHex == colorHex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.trim().isEmpty()) {
                        nameError = true
                        return@Button
                    }
                    onSave(nameText.trim(), selectedType, showInPortfolio, selectedIcon, selectedColorHex)
                },
                modifier = Modifier.testTag("button_save_category")
            ) {
                Text(if (isEditing) "Update" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("button_cancel_category")) {
                Text("Cancel")
            }
        }
    )
}
