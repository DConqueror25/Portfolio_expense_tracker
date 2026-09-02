package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val themeMode: String = "SYSTEM",
    val defaultTimeframe: String = "MONTH",
    val lastBackupTimestamp: Long? = null
)
