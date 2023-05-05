package com.symplified.order.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.symplified.order.R
import com.symplified.order.data.dao.BtPrinterDao
import com.symplified.order.models.printers.BtPrinter

@Database(
    version = 1,
    entities = [BtPrinter::class],
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun printerDao(): BtPrinterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    context.resources.getString(R.string.db_name)
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}