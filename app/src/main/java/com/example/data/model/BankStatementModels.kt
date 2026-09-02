package com.example.data.model

import java.util.UUID

data class ParsedBankTransaction(
    val id: String = UUID.randomUUID().toString(),
    val dateString: String,
    val timestamp: Long,
    val description: String,
    val amount: Double,
    val isDebit: Boolean, // true for money debited/outgoing, false for credited/incoming
    var categoryId: Long,
    var categoryName: String,
    var categoryType: CategoryType,
    var isSelected: Boolean = true,
    val originalNarration: String = ""
)

data class ParsedBankStatement(
    val bankName: String,
    val accountNumber: String? = null,
    val statementPeriod: String? = null,
    val transactions: List<ParsedBankTransaction> = emptyList(),
    val totalDebits: Double = 0.0,
    val totalCredits: Double = 0.0
)
