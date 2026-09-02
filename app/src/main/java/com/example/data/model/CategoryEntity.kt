package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val showInPortfolio: Boolean = (type == CategoryType.ASSET),
    val isArchived: Boolean = false,
    val iconName: String = "category",
    val colorHex: Long = 0xFF10B981,
    val createdDate: Long = System.currentTimeMillis()
)
