package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BudgetAllocationEntity
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.PaydayRecordEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.repository.PayWiseRepository
import com.example.domain.CategoryBreakdown
import com.example.domain.FinanceCalculator
import com.example.domain.FinancialSummary
import com.example.domain.PaydaySummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

enum class MainTab {
    DASHBOARD,
    BUDGET,
    TRANSACTIONS,
    SETTINGS
}

enum class HistoryTimePeriod(val label: String) {
    ALL("All Time"),
    DAILY("Today"),
    WEEKLY("This Week"),
    FORTNIGHTLY("Current Fortnight"),
    MONTHLY("This Month")
}

data class PayWiseUiState(
    val isInitialized: Boolean = false,
    val settings: UserSettingsEntity = UserSettingsEntity(),
    val paydaySummary: PaydaySummary = FinanceCalculator.calculatePaydaySummary(FinanceCalculator.DEFAULT_PAYDAY),
    val financialSummary: FinancialSummary = FinanceCalculator.calculateFinancialSummary(
        emptyList(),
        emptyList(),
        FinanceCalculator.DEFAULT_PAYDAY,
        FinanceCalculator.DEFAULT_PAYDAY.plusDays(13)
    ),
    val categories: List<BudgetCategoryEntity> = emptyList(),
    val categoryBreakdowns: List<CategoryBreakdown> = emptyList(),
    val budgetAllocations: List<BudgetAllocationEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val paydayRecords: List<PaydayRecordEntity> = emptyList(),
    val statusMessage: String? = null
)

class PayWiseViewModel(private val repository: PayWiseRepository) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked

    init {
        viewModelScope.launch {
            repository.ensureDefaultsInitialized()
        }
    }

    val uiState: StateFlow<PayWiseUiState> = combine(
        repository.userSettings,
        repository.allTransactions,
        repository.allCategories,
        repository.allBudgetAllocations,
        combine(
            repository.allSavingsGoals,
            repository.allPaydayRecords,
            _statusMessage
        ) { goals, paydayRecords, msg -> Triple(goals, paydayRecords, msg) }
    ) { settingsOpt, transactions, categories, allocations, (goals, paydayRecords, msg) ->
        val settings = settingsOpt ?: UserSettingsEntity()
        val anchorDate = LocalDate.ofEpochDay(settings.anchorPaydayEpochDay)
        val paydaySummary = FinanceCalculator.calculatePaydaySummary(anchorDate, LocalDate.now())
        val financialSummary = FinanceCalculator.calculateFinancialSummary(
            transactions = transactions,
            categories = categories,
            currentPeriodStart = paydaySummary.currentPeriodStart,
            currentPeriodEnd = paydaySummary.currentPeriodEnd
        )
        val categoryBreakdowns = FinanceCalculator.calculateCategoryBreakdowns(categories, transactions)

        PayWiseUiState(
            isInitialized = true,
            settings = settings,
            paydaySummary = paydaySummary,
            financialSummary = financialSummary,
            categories = categories,
            categoryBreakdowns = categoryBreakdowns,
            budgetAllocations = allocations,
            savingsGoals = goals,
            transactions = transactions,
            paydayRecords = paydayRecords,
            statusMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PayWiseUiState()
    )

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun unlockWithPin(enteredPin: String): Boolean {
        val expected = uiState.value.settings.pinCode
        return if (expected.isNullOrBlank() || enteredPin == expected) {
            _isUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun unlockWithBiometricSimulate() {
        _isUnlocked.value = true
    }

    // Income & Expense Actions
    fun addIncome(
        amountStr: String,
        incomeSource: String,
        description: String,
        dateEpochDay: Long = LocalDate.now().toEpochDay()
    ) {
        val amount = FinanceCalculator.toMoney(amountStr)
        if (amount <= FinanceCalculator.ZERO) {
            showMessage("Please enter a valid income amount greater than K0.00")
            return
        }
        viewModelScope.launch {
            repository.addIncome(amount, incomeSource, description, dateEpochDay)
            showMessage("Income of ${FinanceCalculator.formatCurrency(amount, uiState.value.settings.currencyFormat)} recorded.")
        }
    }

    fun addExpense(
        amountStr: String,
        categoryId: Long?,
        categoryName: String,
        description: String,
        dateEpochDay: Long = LocalDate.now().toEpochDay()
    ) {
        val amount = FinanceCalculator.toMoney(amountStr)
        if (amount <= FinanceCalculator.ZERO) {
            showMessage("Please enter a valid expense amount greater than K0.00")
            return
        }
        viewModelScope.launch {
            repository.addExpense(amount, categoryId, categoryName, description, dateEpochDay)
            showMessage("Expense of ${FinanceCalculator.formatCurrency(amount, uiState.value.settings.currencyFormat)} saved.")
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        if (transaction.amount <= FinanceCalculator.ZERO) {
            showMessage("Amount must be greater than K0.00")
            return
        }
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            showMessage("Transaction updated.")
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            showMessage("Transaction deleted.")
        }
    }

    // Savings Actions
    fun createSavingsGoal(name: String, targetAmountStr: String) {
        val target = FinanceCalculator.toMoney(targetAmountStr)
        if (name.isBlank() || target <= FinanceCalculator.ZERO) {
            showMessage("Enter a goal name and target amount greater than K0.00")
            return
        }
        viewModelScope.launch {
            repository.createSavingsGoal(name, target)
            showMessage("Savings goal '${name.trim()}' created.")
        }
    }

    fun depositToSavings(
        goal: SavingsGoalEntity,
        amountStr: String,
        description: String,
        dateEpochDay: Long = LocalDate.now().toEpochDay()
    ) {
        val amount = FinanceCalculator.toMoney(amountStr)
        if (amount <= FinanceCalculator.ZERO) {
            showMessage("Enter a valid deposit amount greater than K0.00")
            return
        }
        viewModelScope.launch {
            repository.depositToSavings(goal, amount, description, dateEpochDay)
            showMessage("Deposited ${FinanceCalculator.formatCurrency(amount, uiState.value.settings.currencyFormat)} to ${goal.name}.")
        }
    }

    fun withdrawFromSavings(
        goal: SavingsGoalEntity,
        amountStr: String,
        description: String,
        dateEpochDay: Long = LocalDate.now().toEpochDay()
    ) {
        val amount = FinanceCalculator.toMoney(amountStr)
        if (amount <= FinanceCalculator.ZERO) {
            showMessage("Enter a valid withdrawal amount greater than K0.00")
            return
        }
        if (amount > goal.currentAmount) {
            showMessage("Withdrawal exceeds current savings in ${goal.name}.")
            return
        }
        viewModelScope.launch {
            val ok = repository.withdrawFromSavings(goal, amount, description, dateEpochDay)
            if (ok) {
                showMessage("Withdrew ${FinanceCalculator.formatCurrency(amount, uiState.value.settings.currencyFormat)} from ${goal.name}.")
            } else {
                showMessage("Could not process withdrawal.")
            }
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
            showMessage("Savings goal '${goal.name}' deleted.")
        }
    }

    // Budget Category Actions
    fun addCategory(name: String, budgetAmountStr: String, colorHex: String = "#1B7A4E") {
        val budget = FinanceCalculator.toMoney(budgetAmountStr)
        if (name.isBlank() || budget < FinanceCalculator.ZERO) {
            showMessage("Please enter a valid category name and budget.")
            return
        }
        viewModelScope.launch {
            repository.addCategory(name, budget, colorHex)
            showMessage("Category '${name.trim()}' added.")
        }
    }

    fun updateCategory(category: BudgetCategoryEntity, newName: String, newBudgetStr: String) {
        val budget = FinanceCalculator.toMoney(newBudgetStr)
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updateCategory(category.copy(name = newName.trim(), budgetAmount = budget))
            showMessage("Category '${newName.trim()}' updated.")
        }
    }

    fun deleteCategory(category: BudgetCategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            showMessage("Category '${category.name}' removed.")
        }
    }

    // Budget Checkbox System Actions (Section 10)
    fun addBudgetAllocation(name: String, plannedAmountStr: String, category: BudgetCategoryEntity) {
        val amount = FinanceCalculator.toMoney(plannedAmountStr)
        if (name.isBlank() || amount <= FinanceCalculator.ZERO) {
            showMessage("Enter an item name and planned amount greater than K0.00")
            return
        }
        viewModelScope.launch {
            repository.addBudgetAllocation(
                name = name,
                plannedAmount = amount,
                category = category,
                payPeriodStartEpochDay = uiState.value.paydaySummary.currentPeriodStart.toEpochDay()
            )
            showMessage("Planned expense '${name.trim()}' added to checklist.")
        }
    }

    fun toggleBudgetAllocationPaid(allocation: BudgetAllocationEntity, markPaid: Boolean) {
        viewModelScope.launch {
            repository.toggleBudgetAllocationPaid(allocation.id, markPaid)
            val formatted = FinanceCalculator.formatCurrency(allocation.plannedAmount, uiState.value.settings.currencyFormat)
            if (markPaid) {
                showMessage("Paid '${allocation.name}' ($formatted) — deducted from balance.")
            } else {
                showMessage("Unchecked '${allocation.name}' ($formatted) — expense reversed.")
            }
        }
    }

    fun deleteBudgetAllocation(allocation: BudgetAllocationEntity) {
        viewModelScope.launch {
            repository.deleteBudgetAllocation(allocation)
            showMessage("Planned item '${allocation.name}' deleted.")
        }
    }

    // Payday & Settings Actions
    fun updatePaydayAndSalary(newPaydayEpochDay: Long, salaryStr: String, logNewPeriodNow: Boolean, notes: String = "") {
        val salary = FinanceCalculator.toMoney(salaryStr)
        viewModelScope.launch {
            if (logNewPeriodNow) {
                repository.recordNewPaydayPeriod(
                    paydayDate = LocalDate.ofEpochDay(newPaydayEpochDay),
                    salaryAmount = salary,
                    logSalaryIncome = true,
                    notes = notes
                )
                showMessage("New fortnightly pay period started & salary logged!")
            } else {
                repository.updateSettings {
                    it.copy(
                        anchorPaydayEpochDay = newPaydayEpochDay,
                        fortnightlySalary = salary
                    )
                }
                showMessage("Payday schedule and fortnightly salary updated.")
            }
        }
    }

    fun setCurrencyFormat(format: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(currencyFormat = format) }
            showMessage("Currency display changed to $format.")
        }
    }

    fun setDarkModeSetting(mode: String) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(darkModeSetting = mode) }
        }
    }

    fun updateSecuritySettings(pinCode: String?, biometricEnabled: Boolean) {
        viewModelScope.launch {
            val cleanPin = pinCode?.trim()?.ifBlank { null }
            repository.updateSettings {
                it.copy(
                    pinCode = cleanPin,
                    biometricEnabled = if (cleanPin == null) false else biometricEnabled
                )
            }
            _isUnlocked.value = true
            showMessage(if (cleanPin != null) "PIN security enabled." else "PIN security disabled.")
        }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            repository.loadClearlyLabeledDemoData()
            showMessage("Loaded clearly labeled [DEMO] sample PNG fortnight data.")
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllApplicationData()
            showMessage("All application data has been reset to defaults.")
        }
    }

    suspend fun generateCsvExport(): String = repository.exportTransactionsToCsv()

    suspend fun generateBackupJson(): String = repository.createBackupJson()

    fun restoreFromBackupJson(json: String) {
        viewModelScope.launch {
            val success = repository.restoreFromBackupJson(json)
            if (success) {
                showMessage("Backup restored successfully!")
            } else {
                showMessage("Invalid backup JSON format.")
            }
        }
    }

    class Factory(private val repository: PayWiseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PayWiseViewModel(repository) as T
        }
    }
}
