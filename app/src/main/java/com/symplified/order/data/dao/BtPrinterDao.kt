package com.symplified.order.data.dao

import androidx.room.*
import com.symplified.order.models.printers.BtPrinter
import kotlinx.coroutines.flow.Flow

@Dao
interface BtPrinterDao {

    @Query("SELECT * FROM printers")
    fun allPrinters(): Flow<List<BtPrinter>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(printer: BtPrinter)

    @Query("UPDATE printers SET isEnabled = :isEnabled WHERE name = :name")
    suspend fun togglePrinting(name: String, isEnabled: Boolean)

    @Delete
    fun delete(printer: BtPrinter)

    @Query("DELETE FROM printers")
    fun clear()
}