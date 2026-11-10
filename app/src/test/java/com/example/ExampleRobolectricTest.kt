package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PayWiseDatabase
import com.example.data.repository.PayWiseRepository
import com.example.domain.FinanceCalculator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: PayWiseDatabase
    private lateinit var repository: PayWiseRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PayWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PayWiseRepository(database.payWiseDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `verify app_name resource is PayWise PNG`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PayWise PNG", appName)
    }

    @Test
    fun `verify default PNG categories and budget checkbox reversal and savings flow`() = runBlocking {
        repository.ensureDefaultsInitialized()
        val categories = database.payWiseDao().getAllCategoriesOnce()
        assertEquals(7, categories.size)
        assertTrue(categories.any { it.name == "Dinau (Person)" })
        assertTrue(categories.any { it.name == "Flex / Rait Kad" })

        val today = LocalDate.now()
        // 1. Add Fortnightly Salary Income K1,500.00
        repository.addIncome(BigDecimal("1500.00"), "Fortnightly Salary", "Payday", today.toEpochDay())

        // 2. Add Planned Budget Allocation item (K80.00 for Relatives)
        val relativesCat = categories.first { it.name == "Relatives" }
        val allocId = repository.addBudgetAllocation(
            name = "Support Relatives",
            plannedAmount = BigDecimal("80.00"),
            category = relativesCat,
            payPeriodStartEpochDay = today.toEpochDay()
        )

        // Check item as Paid -> should deduct K80.00 and record 1 expense transaction
        repository.toggleBudgetAllocationPaid(allocId, markPaid = true, dateEpochDay = today.toEpochDay())
        // Toggle Paid a second time -> must NOT duplicate deduction
        repository.toggleBudgetAllocationPaid(allocId, markPaid = true, dateEpochDay = today.toEpochDay())

        var txs = database.payWiseDao().getAllTransactionsOnce()
        var summary = FinanceCalculator.calculateFinancialSummary(txs, categories, today, today.plusDays(13))
        assertEquals(BigDecimal("80.00"), summary.totalExpenses)
        assertEquals(BigDecimal("1420.00"), summary.availableBalance)

        // Uncheck paid item -> must reverse transaction cleanly
        repository.toggleBudgetAllocationPaid(allocId, markPaid = false, dateEpochDay = today.toEpochDay())
        txs = database.payWiseDao().getAllTransactionsOnce()
        summary = FinanceCalculator.calculateFinancialSummary(txs, categories, today, today.plusDays(13))
        assertEquals(BigDecimal("0.00"), summary.totalExpenses)
        assertEquals(BigDecimal("1500.00"), summary.availableBalance)

        // 3. Create Savings Goal (K2,000), deposit K500, withdraw K100
        val goalId = repository.createSavingsGoal("Emergency Fund", BigDecimal("2000.00"))
        val goal = database.payWiseDao().getSavingsGoalById(goalId)!!
        repository.depositToSavings(goal, BigDecimal("500.00"), "Initial Save", today.toEpochDay())
        val updatedGoalAfterDeposit = database.payWiseDao().getSavingsGoalById(goalId)!!
        assertEquals(BigDecimal("500.00"), updatedGoalAfterDeposit.currentAmount)

        val withdrewOk = repository.withdrawFromSavings(updatedGoalAfterDeposit, BigDecimal("100.00"), "Emergency use", today.toEpochDay())
        assertTrue(withdrewOk)
        val updatedGoalAfterWithdraw = database.payWiseDao().getSavingsGoalById(goalId)!!
        assertEquals(BigDecimal("400.00"), updatedGoalAfterWithdraw.currentAmount)

        // Reject withdrawal larger than current savings
        val invalidWithdraw = repository.withdrawFromSavings(updatedGoalAfterWithdraw, BigDecimal("900.00"), "Too much", today.toEpochDay())
        assertFalse(invalidWithdraw)

        txs = database.payWiseDao().getAllTransactionsOnce()
        summary = FinanceCalculator.calculateFinancialSummary(txs, categories, today, today.plusDays(13))
        assertEquals(BigDecimal("400.00"), summary.currentSavings)
        assertEquals(BigDecimal("1100.00"), summary.availableBalance)
    }
}
