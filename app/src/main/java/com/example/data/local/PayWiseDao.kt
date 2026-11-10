package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PayWiseDao {
    // Transactions
    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, timestamp DESC")
    suspend fun getAllTransactionsOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE budgetAllocationId = :allocationId LIMIT 1")
    suspend fun getTransactionByAllocationId(allocationId: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    // Budget Categories
    @Query("SELECT * FROM budget_categories ORDER BY isDefault DESC, id ASC")
    fun getAllCategories(): Flow<List<BudgetCategoryEntity>>

    @Query("SELECT * FROM budget_categories ORDER BY isDefault DESC, id ASC")
    suspend fun getAllCategoriesOnce(): List<BudgetCategoryEntity>

    @Query("SELECT * FROM budget_categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): BudgetCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: BudgetCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<BudgetCategoryEntity>)

    @Update
    suspend fun updateCategory(category: BudgetCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: BudgetCategoryEntity)

    @Query("DELETE FROM budget_categories")
    suspend fun deleteAllCategories()

    // Budget Allocations (Budget Checkbox System)
    @Query("SELECT * FROM budget_allocations ORDER BY isPaid ASC, id DESC")
    fun getAllBudgetAllocations(): Flow<List<BudgetAllocationEntity>>

    @Query("SELECT * FROM budget_allocations ORDER BY isPaid ASC, id DESC")
    suspend fun getAllBudgetAllocationsOnce(): List<BudgetAllocationEntity>

    @Query("SELECT * FROM budget_allocations WHERE id = :id LIMIT 1")
    suspend fun getBudgetAllocationById(id: Long): BudgetAllocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetAllocation(allocation: BudgetAllocationEntity): Long

    @Update
    suspend fun updateBudgetAllocation(allocation: BudgetAllocationEntity)

    @Delete
    suspend fun deleteBudgetAllocation(allocation: BudgetAllocationEntity)

    @Query("DELETE FROM budget_allocations")
    suspend fun deleteAllBudgetAllocations()

    // Savings Goals
    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun getAllSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    suspend fun getAllSavingsGoalsOnce(): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getSavingsGoalById(id: Long): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAllSavingsGoals()

    // Payday Records
    @Query("SELECT * FROM payday_records ORDER BY paydayEpochDay DESC")
    fun getAllPaydayRecords(): Flow<List<PaydayRecordEntity>>

    @Query("SELECT * FROM payday_records ORDER BY paydayEpochDay DESC")
    suspend fun getAllPaydayRecordsOnce(): List<PaydayRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaydayRecord(record: PaydayRecordEntity): Long

    @Query("DELETE FROM payday_records")
    suspend fun deleteAllPaydayRecords()

    // User Settings
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getUserSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getUserSettingsOnce(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(settings: UserSettingsEntity)
}
