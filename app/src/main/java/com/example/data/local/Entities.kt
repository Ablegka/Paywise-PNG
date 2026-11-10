package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.domain.FinanceCalculator
import java.math.BigDecimal
import java.math.RoundingMode

object TransactionType {
    const val INCOME = "INCOME"
    const val EXPENSE = "EXPENSE"
    const val SAVINGS_DEPOSIT = "SAVINGS_DEPOSIT"
    const val SAVINGS_WITHDRAWAL = "SAVINGS_WITHDRAWAL"
}

class BigDecimalConverters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String {
        return (value ?: BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP).toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal {
        return FinanceCalculator.toMoney(value)
    }
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // INCOME, EXPENSE, SAVINGS_DEPOSIT, SAVINGS_WITHDRAWAL
    val amount: BigDecimal,
    val categoryId: Long? = null,
    val categoryName: String,
    val incomeSource: String? = null,
    val savingsGoalId: Long? = null,
    val budgetAllocationId: Long? = null,
    val description: String,
    val dateEpochDay: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isDemo: Boolean = false
)

@Entity(tableName = "budget_categories")
data class BudgetCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val budgetAmount: BigDecimal,
    val iconName: String = "Category",
    val colorHex: String = "#1B7A4E",
    val isDefault: Boolean = false
)

@Entity(tableName = "budget_allocations")
data class BudgetAllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val plannedAmount: BigDecimal,
    val categoryId: Long,
    val categoryName: String,
    val isPaid: Boolean = false,
    val linkedTransactionId: Long? = null,
    val payPeriodStartEpochDay: Long
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: BigDecimal,
    val currentAmount: BigDecimal = FinanceCalculator.ZERO,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payday_records")
data class PaydayRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val paydayEpochDay: Long,
    val periodStartEpochDay: Long,
    val periodEndEpochDay: Long,
    val salaryAmount: BigDecimal,
    val totalIncomeInPeriod: BigDecimal,
    val totalExpensesInPeriod: BigDecimal,
    val totalSavedInPeriod: BigDecimal,
    val notes: String = ""
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val currencyFormat: String = "K", // "K", "PGK", "KINA"
    val anchorPaydayEpochDay: Long = FinanceCalculator.DEFAULT_PAYDAY.toEpochDay(),
    val fortnightlySalary: BigDecimal = BigDecimal("1450.00"),
    val darkModeSetting: String = "DARK", // "DARK", "LIGHT", "SYSTEM"
    val pinCode: String? = null,
    val biometricEnabled: Boolean = false
)
