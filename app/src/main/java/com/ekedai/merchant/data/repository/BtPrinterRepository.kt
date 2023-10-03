package com.ekedai.merchant.data.repository

import com.ekedai.merchant.data.dao.BtPrinterDao
import com.ekedai.merchant.models.printers.BtPrinter
import kotlinx.coroutines.flow.Flow

class BtPrinterRepository(private val printerDao: BtPrinterDao) {
    val allPrinters: Flow<List<BtPrinter>> = printerDao.allPrinters()

    suspend fun insert(printer: BtPrinter) = printerDao.insert(printer)

    suspend fun togglePrinter(name: String, isEnabled: Boolean) =
        printerDao.togglePrinting(name, isEnabled)

    suspend fun delete(printer: BtPrinter) = printerDao.delete(printer)

    suspend fun clear() = printerDao.clear()
}