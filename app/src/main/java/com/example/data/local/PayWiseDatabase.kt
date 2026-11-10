package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TransactionEntity::class,
        BudgetCategoryEntity::class,
        BudgetAllocationEntity::class,
        SavingsGoalEntity::class,
        PaydayRecordEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(BigDecimalConverters::class)
abstract class PayWiseDatabase : RoomDatabase() {
    abstract fun payWiseDao(): PayWiseDao

    companion object {
        @Volatile
        private var INSTANCE: PayWiseDatabase? = null

        fun getInstance(context: Context): PayWiseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PayWiseDatabase::class.java,
                    "paywise_png_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
