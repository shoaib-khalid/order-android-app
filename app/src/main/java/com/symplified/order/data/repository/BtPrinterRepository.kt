package com.symplified.order.data.repository

import com.symplified.order.data.dao.BtPrinterDao
import com.symplified.order.models.printers.BtPrinter
import kotlinx.coroutines.flow.Flow

class BtPrinterRepository(private val printerDao: BtPrinterDao) {
    val allPrinters: Flow<List<BtPrinter>> = printerDao.allPrinters()

    suspend fun insert(printer: BtPrinter) = printerDao.insert(printer)

    suspend fun togglePrinter(name: String, isEnabled: Boolean) =
        printerDao.togglePrinting(name, isEnabled)

    suspend fun delete(printer: BtPrinter) = printerDao.delete(printer)

    suspend fun clear() = printerDao.clear()
}