package com.example.domain

import com.example.data.local.BudgetAllocationEntity
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class PaydaySummary(
    val anchorPayday: LocalDate,
    val currentPeriodStart: LocalDate,
    val currentPeriodEnd: LocalDate,
    val nextPayday: LocalDate,
    val daysRemaining: Long,
    val progressFraction: Float
)

data class FinancialSummary(
    val totalIncome: BigDecimal,
    val totalExpenses: BigDecimal,
    val totalSavingsDeposits: BigDecimal,
    val totalSavingsWithdrawals: BigDecimal,
    val currentSavings: BigDecimal,
    val availableBalance: BigDecimal,
    val totalBudgetPlanned: BigDecimal,
    val remainingBudget: BigDecimal,
    val periodIncome: BigDecimal,
    val periodExpenses: BigDecimal,
    val periodSavings: BigDecimal
)

data class CategoryBreakdown(
    val category: BudgetCategoryEntity,
    val spentAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val progressFraction: Float,
    val transactions: List<TransactionEntity>
)

object FinanceCalculator {
    val ZERO: BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    private val HUNDRED: BigDecimal = BigDecimal("100.00")
    val DEFAULT_PAYDAY: LocalDate = LocalDate.of(2026, 7, 17)
    const val FORTNIGHT_DAYS: Long = 14L

    val FLEX_RAIT_KAD_OPTIONS: List<BigDecimal> = listOf(
        BigDecimal("3.00"),
        BigDecimal("5.00"),
        BigDecimal("10.00"),
        BigDecimal("20.00")
    )

    val DEFAULT_INCOME_SOURCES: List<String> = listOf(
        "Fortnightly Salary",
        "Overtime",
        "Business Income",
        "Other Income",
        "Gifts"
    )

    fun toMoney(value: String?): BigDecimal {
        if (value.isNullOrBlank()) return ZERO
        return try {
            BigDecimal(value.trim().replace(",", "")).setScale(2, RoundingMode.HALF_UP)
        } catch (e: NumberFormatException) {
            ZERO
        }
    }

    fun toMoney(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)

    /**
     * Financial Calculation Rules (Section 14):
     * Available Balance = Total Income - Total Expenses - Current Savings.
     * Savings deposits reduce available spending money without being counted as ordinary expenses.
     * Savings withdrawals increase available spending money (by reducing Current Savings).
     * Never count the same transaction twice (deduplicated by unique ID).
     */
    fun calculateFinancialSummary(
        transactions: List<TransactionEntity>,
        categories: List<BudgetCategoryEntity>,
        currentPeriodStart: LocalDate,
        currentPeriodEnd: LocalDate
    ): FinancialSummary {
        val uniqueTransactions = transactions.distinctBy { it.id }

        var totalIncome = ZERO
        var totalExpenses = ZERO
        var totalSavingsDeposits = ZERO
        var totalSavingsWithdrawals = ZERO

        var periodIncome = ZERO
        var periodExpenses = ZERO
        var periodDeposits = ZERO
        var periodWithdrawals = ZERO

        val startEpoch = currentPeriodStart.toEpochDay()
        val endEpoch = currentPeriodEnd.toEpochDay()

        for (tx in uniqueTransactions) {
            val amt = toMoney(tx.amount)
            val inPeriod = tx.dateEpochDay in startEpoch..endEpoch
            when (tx.type) {
                TransactionType.INCOME -> {
                    totalIncome = totalIncome.add(amt)
                    if (inPeriod) periodIncome = periodIncome.add(amt)
                }
                TransactionType.EXPENSE -> {
                    totalExpenses = totalExpenses.add(amt)
                    if (inPeriod) periodExpenses = periodExpenses.add(amt)
                }
                TransactionType.SAVINGS_DEPOSIT -> {
                    totalSavingsDeposits = totalSavingsDeposits.add(amt)
                    if (inPeriod) periodDeposits = periodDeposits.add(amt)
                }
                TransactionType.SAVINGS_WITHDRAWAL -> {
                    totalSavingsWithdrawals = totalSavingsWithdrawals.add(amt)
                    if (inPeriod) periodWithdrawals = periodWithdrawals.add(amt)
                }
            }
        }

        val currentSavings = totalSavingsDeposits.subtract(totalSavingsWithdrawals)
            .max(ZERO)
            .setScale(2, RoundingMode.HALF_UP)

        val availableBalance = totalIncome
            .subtract(totalExpenses)
            .subtract(currentSavings)
            .setScale(2, RoundingMode.HALF_UP)

        val totalBudgetPlanned = categories.fold(ZERO) { acc, cat ->
            acc.add(toMoney(cat.budgetAmount))
        }

        val remainingBudget = totalBudgetPlanned.subtract(totalExpenses)
            .setScale(2, RoundingMode.HALF_UP)

        val periodSavings = periodDeposits.subtract(periodWithdrawals)
            .max(ZERO)
            .setScale(2, RoundingMode.HALF_UP)

        return FinancialSummary(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            totalSavingsDeposits = totalSavingsDeposits,
            totalSavingsWithdrawals = totalSavingsWithdrawals,
            currentSavings = currentSavings,
            availableBalance = availableBalance,
            totalBudgetPlanned = totalBudgetPlanned,
            remainingBudget = remainingBudget,
            periodIncome = periodIncome,
            periodExpenses = periodExpenses,
            periodSavings = periodSavings
        )
    }

    /**
     * Fortnightly Payday Calculation (Section 5):
     * Given an anchor payday (default 17 July 2026) and reference date (today),
     * computes the active 14-day pay period and next payday in 14-day increments.
     */
    fun calculatePaydaySummary(
        anchorPayday: LocalDate,
        referenceDate: LocalDate = LocalDate.now()
    ): PaydaySummary {
        val diffDays = ChronoUnit.DAYS.between(anchorPayday, referenceDate)
        val periodsElapsed = Math.floorDiv(diffDays, FORTNIGHT_DAYS)
        val currentPeriodStart = anchorPayday.plusDays(periodsElapsed * FORTNIGHT_DAYS)
        val nextPayday = currentPeriodStart.plusDays(FORTNIGHT_DAYS)
        val currentPeriodEnd = nextPayday.minusDays(1)
        val daysRemaining = ChronoUnit.DAYS.between(referenceDate, nextPayday).coerceIn(0L, FORTNIGHT_DAYS)
        val daysIntoPeriod = (FORTNIGHT_DAYS - daysRemaining).coerceIn(0L, FORTNIGHT_DAYS)
        val progressFraction = (daysIntoPeriod.toFloat() / FORTNIGHT_DAYS.toFloat()).coerceIn(0f, 1f)

        return PaydaySummary(
            anchorPayday = anchorPayday,
            currentPeriodStart = currentPeriodStart,
            currentPeriodEnd = currentPeriodEnd,
            nextPayday = nextPayday,
            daysRemaining = daysRemaining,
            progressFraction = progressFraction
        )
    }

    fun calculateCategoryBreakdowns(
        categories: List<BudgetCategoryEntity>,
        transactions: List<TransactionEntity>
    ): List<CategoryBreakdown> {
        val expenseTransactions = transactions
            .distinctBy { it.id }
            .filter { it.type == TransactionType.EXPENSE }

        return categories.map { category ->
            val catTxs = expenseTransactions.filter {
                it.categoryId == category.id || it.categoryName.equals(category.name, ignoreCase = true)
            }
            val spent = catTxs.fold(ZERO) { acc, tx -> acc.add(toMoney(tx.amount)) }
            val budget = toMoney(category.budgetAmount)
            val remaining = budget.subtract(spent).setScale(2, RoundingMode.HALF_UP)
            val fraction = if (budget > ZERO) {
                spent.divide(budget, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }
            CategoryBreakdown(
                category = category,
                spentAmount = spent,
                remainingAmount = remaining,
                progressFraction = fraction,
                transactions = catTxs
            )
        }
    }

    fun calculateSavingsPercentage(current: BigDecimal, target: BigDecimal): Int {
        val safeTarget = toMoney(target)
        if (safeTarget <= ZERO) return 0
        val safeCurrent = toMoney(current).max(ZERO)
        return safeCurrent.multiply(HUNDRED)
            .divide(safeTarget, 0, RoundingMode.HALF_UP)
            .toInt()
            .coerceIn(0, 100)
    }

    fun formatCurrency(amount: BigDecimal, currencyFormat: String = "K"): String {
        val scaled = toMoney(amount)
        val isNegative = scaled < ZERO
        val absAmount = scaled.abs()
        val formatter = DecimalFormat("#,##0.00")
        val formattedNumber = formatter.format(absAmount)
        val sign = if (isNegative) "-" else ""
        return when (currencyFormat.uppercase()) {
            "PGK" -> "${sign}PGK $formattedNumber"
            "KINA" -> "${sign}$formattedNumber Kina"
            else -> "${sign}K$formattedNumber"
        }
    }

    fun formatDate(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    }

    fun formatShortDate(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.format(DateTimeFormatter.ofPattern("dd MMM"))
    }
}
