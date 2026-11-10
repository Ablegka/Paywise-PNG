package com.example

import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.domain.FinanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class ExampleUnitTest {

    @Test
    fun `available balance follows Total Income minus Total Expenses minus Current Savings`() {
        val payday = LocalDate.of(2026, 7, 17)
        val categories = listOf(
            BudgetCategoryEntity(id = 1, name = "Food", budgetAmount = BigDecimal("350.00")),
            BudgetCategoryEntity(id = 2, name = "Dinau (Person)", budgetAmount = BigDecimal("200.00"))
        )
        val transactions = listOf(
            TransactionEntity(
                id = 1,
                type = TransactionType.INCOME,
                amount = BigDecimal("2000.00"),
                categoryName = "Fortnightly Salary",
                incomeSource = "Fortnightly Salary",
                description = "Salary",
                dateEpochDay = payday.toEpochDay()
            ),
            TransactionEntity(
                id = 2,
                type = TransactionType.EXPENSE,
                amount = BigDecimal("300.50"),
                categoryId = 1,
                categoryName = "Food",
                description = "Market",
                dateEpochDay = payday.plusDays(1).toEpochDay()
            ),
            TransactionEntity(
                id = 3,
                type = TransactionType.SAVINGS_DEPOSIT,
                amount = BigDecimal("500.00"),
                categoryName = "Savings: Emergency",
                savingsGoalId = 10,
                description = "Deposit",
                dateEpochDay = payday.plusDays(2).toEpochDay()
            ),
            TransactionEntity(
                id = 4,
                type = TransactionType.SAVINGS_WITHDRAWAL,
                amount = BigDecimal("100.00"),
                categoryName = "Withdrawal: Emergency",
                savingsGoalId = 10,
                description = "Withdrawal",
                dateEpochDay = payday.plusDays(3).toEpochDay()
            )
        )

        val summary = FinanceCalculator.calculateFinancialSummary(
            transactions = transactions,
            categories = categories,
            currentPeriodStart = payday,
            currentPeriodEnd = payday.plusDays(13)
        )

        // Total Income = 2000.00
        assertEquals(BigDecimal("2000.00"), summary.totalIncome)
        // Total Expenses = 300.50 (savings deposits are NOT counted as ordinary expenses)
        assertEquals(BigDecimal("300.50"), summary.totalExpenses)
        // Current Savings = 500.00 - 100.00 = 400.00
        assertEquals(BigDecimal("400.00"), summary.currentSavings)
        // Available Balance = 2000.00 - 300.50 - 400.00 = 1299.50
        assertEquals(BigDecimal("1299.50"), summary.availableBalance)
    }

    @Test
    fun `duplicate transaction IDs are never counted twice`() {
        val payday = LocalDate.of(2026, 7, 17)
        val tx = TransactionEntity(
            id = 99,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("50.00"),
            categoryName = "Transport",
            description = "PMV Bus",
            dateEpochDay = payday.toEpochDay()
        )
        val summary = FinanceCalculator.calculateFinancialSummary(
            transactions = listOf(tx, tx),
            categories = emptyList(),
            currentPeriodStart = payday,
            currentPeriodEnd = payday.plusDays(13)
        )
        assertEquals(BigDecimal("50.00"), summary.totalExpenses)
    }

    @Test
    fun `fortnightly payday calculation from default 17 July 2026`() {
        val anchor = LocalDate.of(2026, 7, 17)
        // On 20 July 2026 (3 days into the fortnight):
        val refDate = LocalDate.of(2026, 7, 20)
        val summary = FinanceCalculator.calculatePaydaySummary(anchor, refDate)

        assertEquals(LocalDate.of(2026, 7, 17), summary.currentPeriodStart)
        assertEquals(LocalDate.of(2026, 7, 30), summary.currentPeriodEnd)
        assertEquals(LocalDate.of(2026, 7, 31), summary.nextPayday)
        assertEquals(11L, summary.daysRemaining)
    }

    @Test
    fun `savings goal progress percentage matches specification example`() {
        // Example from spec: Savings goal K2,000, Current savings K500 -> Progress 25%
        val pct = FinanceCalculator.calculateSavingsPercentage(
            current = BigDecimal("500.00"),
            target = BigDecimal("2000.00")
        )
        assertEquals(25, pct)
    }
}
