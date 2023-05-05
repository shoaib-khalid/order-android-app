package com.symplified.order.models.printers

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "printers")
data class BtPrinter(
    @PrimaryKey
    val name: String,
    @ColumnInfo(typeAffinity = ColumnInfo.INTEGER, defaultValue = "1")
    val isEnabled: Boolean = true
)