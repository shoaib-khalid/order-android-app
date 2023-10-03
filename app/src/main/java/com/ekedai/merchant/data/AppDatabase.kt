package com.ekedai.merchant.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ekedai.merchant.R
import com.ekedai.merchant.data.dao.BtPrinterDao
import com.ekedai.merchant.models.printers.BtPrinter

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