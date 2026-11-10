package com.example.data.repository

import com.example.data.local.BudgetAllocationEntity
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.PayWiseDao
import com.example.data.local.PaydayRecordEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.UserSettingsEntity
import com.example.domain.FinanceCalculator
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class PayWiseRepository(private val dao: PayWiseDao) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allCategories: Flow<List<BudgetCategoryEntity>> = dao.getAllCategories()
    val allBudgetAllocations: Flow<List<BudgetAllocationEntity>> = dao.getAllBudgetAllocations()
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = dao.getAllSavingsGoals()
    val allPaydayRecords: Flow<List<PaydayRecordEntity>> = dao.getAllPaydayRecords()
    val userSettings: Flow<UserSettingsEntity?> = dao.getUserSettings()

    suspend fun ensureDefaultsInitialized() {
        val currentSettings = dao.getUserSettingsOnce()
        if (currentSettings == null) {
            dao.insertUserSettings(UserSettingsEntity())
        }
        val existingCategories = dao.getAllCategoriesOnce()
        if (existingCategories.isEmpty()) {
            dao.insertCategories(defaultPngCategories())
        }
    }

    fun defaultPngCategories(): List<BudgetCategoryEntity> = listOf(
        BudgetCategoryEntity(
            name = "Dinau (Person)",
            budgetAmount = BigDecimal("200.00"),
            iconName = "Handshake",
            colorHex = "#EF4444",
            isDefault = true
        ),
        BudgetCategoryEntity(
            name = "Relatives",
            budgetAmount = BigDecimal("150.00"),
            iconName = "Groups",
            colorHex = "#F59E0B",
            isDefault = true
        ),
        BudgetCategoryEntity(
            name = "Food",
            budgetAmount = BigDecimal("350.00"),
            iconName = "Restaurant",
            colorHex = "#10B981",
            isDefault = true
        ),
        BudgetCategoryEntity(
            name = "Transport",
            budgetAmount = BigDecimal("120.00"),
            iconName = "DirectionsBus",
            colorHex = "#3B82F6",
            isDefault = true
        ),
        BudgetCategoryEntity(
            name = "Phone Credit",
            budgetAmount = BigDecimal("40.00"),
            iconName = "PhoneAndroid",
            colorHex = "#8B5CF6",
            isDefault = true
        ),
        BudgetCategoryEntity(
            name = "Flex / Rait Kad",
            budgetAmount = BigDecimal("30.00"),
            iconName = "SimCard",
            colorHex = "#EC4899",
            isDefault = true
        ),
        BudgetCategoryEntity(
            name = "Other Expenses",
            budgetAmount = BigDecimal("100.00"),
            iconName = "ReceiptLong",
            colorHex = "#14B8A6",
            isDefault = true
        )
    )

    // Income & Expense Transactions
    suspend fun addIncome(
        amount: BigDecimal,
        incomeSource: String,
        description: String,
        dateEpochDay: Long,
        isDemo: Boolean = false
    ): Long {
        val tx = TransactionEntity(
            type = TransactionType.INCOME,
            amount = FinanceCalculator.toMoney(amount),
            categoryName = incomeSource,
            incomeSource = incomeSource,
            description = description.ifBlank { incomeSource },
            dateEpochDay = dateEpochDay,
            isDemo = isDemo
        )
        return dao.insertTransaction(tx)
    }

    suspend fun addExpense(
        amount: BigDecimal,
        categoryId: Long?,
        categoryName: String,
        description: String,
        dateEpochDay: Long,
        isDemo: Boolean = false
    ): Long {
        val tx = TransactionEntity(
            type = TransactionType.EXPENSE,
            amount = FinanceCalculator.toMoney(amount),
            categoryId = categoryId,
            categoryName = categoryName,
            description = description.ifBlank { categoryName },
            dateEpochDay = dateEpochDay,
            isDemo = isDemo
        )
        return dao.insertTransaction(tx)
    }

    suspend fun updateTransaction(updated: TransactionEntity) {
        val existing = dao.getTransactionById(updated.id)
        dao.updateTransaction(updated.copy(amount = FinanceCalculator.toMoney(updated.amount)))

        // If linked to a budget allocation, keep plannedAmount in sync
        val allocId = updated.budgetAllocationId ?: existing?.budgetAllocationId
        if (allocId != null) {
            val alloc = dao.getBudgetAllocationById(allocId)
            if (alloc != null) {
                dao.updateBudgetAllocation(
                    alloc.copy(
                        plannedAmount = FinanceCalculator.toMoney(updated.amount),
                        categoryName = updated.categoryName
                    )
                )
            }
        }
        // Recalculate savings goal if linked
        val goalId = updated.savingsGoalId ?: existing?.savingsGoalId
        if (goalId != null) {
            recalculateSavingsGoalBalance(goalId)
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        // If this transaction came from a Budget Allocation checkbox, uncheck that allocation
        val allocId = transaction.budgetAllocationId
        if (allocId != null) {
            val alloc = dao.getBudgetAllocationById(allocId)
            if (alloc != null) {
                dao.updateBudgetAllocation(
                    alloc.copy(isPaid = false, linkedTransactionId = null)
                )
            }
        }
        dao.deleteTransaction(transaction)
        if (transaction.savingsGoalId != null) {
            recalculateSavingsGoalBalance(transaction.savingsGoalId)
        }
    }

    // Savings Management (Section 9 & 14)
    suspend fun createSavingsGoal(name: String, targetAmount: BigDecimal): Long {
        return dao.insertSavingsGoal(
            SavingsGoalEntity(
                name = name.trim(),
                targetAmount = FinanceCalculator.toMoney(targetAmount),
                currentAmount = FinanceCalculator.ZERO
            )
        )
    }

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) {
        dao.updateSavingsGoal(
            goal.copy(
                targetAmount = FinanceCalculator.toMoney(goal.targetAmount),
                currentAmount = FinanceCalculator.toMoney(goal.currentAmount)
            )
        )
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        val allTxs = dao.getAllTransactionsOnce().filter { it.savingsGoalId == goal.id }
        allTxs.forEach { dao.deleteTransaction(it) }
        dao.deleteSavingsGoal(goal)
    }

    suspend fun depositToSavings(
        goal: SavingsGoalEntity,
        amount: BigDecimal,
        description: String,
        dateEpochDay: Long = LocalDate.now().toEpochDay(),
        isDemo: Boolean = false
    ): Long {
        val scaledAmount = FinanceCalculator.toMoney(amount)
        val txId = dao.insertTransaction(
            TransactionEntity(
                type = TransactionType.SAVINGS_DEPOSIT,
                amount = scaledAmount,
                categoryName = "Savings: ${goal.name}",
                savingsGoalId = goal.id,
                description = description.ifBlank { "Deposit to ${goal.name}" },
                dateEpochDay = dateEpochDay,
                isDemo = isDemo
            )
        )
        recalculateSavingsGoalBalance(goal.id)
        return txId
    }

    suspend fun withdrawFromSavings(
        goal: SavingsGoalEntity,
        amount: BigDecimal,
        description: String,
        dateEpochDay: Long = LocalDate.now().toEpochDay(),
        isDemo: Boolean = false
    ): Boolean {
        val scaledAmount = FinanceCalculator.toMoney(amount)
        val freshGoal = dao.getSavingsGoalById(goal.id) ?: return false
        if (scaledAmount <= FinanceCalculator.ZERO || scaledAmount > freshGoal.currentAmount) {
            return false
        }
        dao.insertTransaction(
            TransactionEntity(
                type = TransactionType.SAVINGS_WITHDRAWAL,
                amount = scaledAmount,
                categoryName = "Withdrawal: ${freshGoal.name}",
                savingsGoalId = freshGoal.id,
                description = description.ifBlank { "Withdrawal from ${freshGoal.name}" },
                dateEpochDay = dateEpochDay,
                isDemo = isDemo
            )
        )
        recalculateSavingsGoalBalance(freshGoal.id)
        return true
    }

    private suspend fun recalculateSavingsGoalBalance(goalId: Long) {
        val goal = dao.getSavingsGoalById(goalId) ?: return
        val txs = dao.getAllTransactionsOnce().filter { it.savingsGoalId == goalId }
        var balance = FinanceCalculator.ZERO
        for (tx in txs) {
            when (tx.type) {
                TransactionType.SAVINGS_DEPOSIT -> balance = balance.add(FinanceCalculator.toMoney(tx.amount))
                TransactionType.SAVINGS_WITHDRAWAL -> balance = balance.subtract(FinanceCalculator.toMoney(tx.amount))
            }
        }
        dao.updateSavingsGoal(goal.copy(currentAmount = balance.max(FinanceCalculator.ZERO)))
    }

    // Budget Categories (Section 6)
    suspend fun addCategory(name: String, budgetAmount: BigDecimal, colorHex: String = "#1B7A4E"): Long {
        return dao.insertCategory(
            BudgetCategoryEntity(
                name = name.trim(),
                budgetAmount = FinanceCalculator.toMoney(budgetAmount),
                iconName = "Category",
                colorHex = colorHex,
                isDefault = false
            )
        )
    }

    suspend fun updateCategory(category: BudgetCategoryEntity) {
        dao.updateCategory(category.copy(budgetAmount = FinanceCalculator.toMoney(category.budgetAmount)))
    }

    suspend fun deleteCategory(category: BudgetCategoryEntity) {
        dao.deleteCategory(category)
    }

    // Budget Checkbox System (Section 10)
    suspend fun addBudgetAllocation(
        name: String,
        plannedAmount: BigDecimal,
        category: BudgetCategoryEntity,
        payPeriodStartEpochDay: Long
    ): Long {
        return dao.insertBudgetAllocation(
            BudgetAllocationEntity(
                name = name.trim(),
                plannedAmount = FinanceCalculator.toMoney(plannedAmount),
                categoryId = category.id,
                categoryName = category.name,
                isPaid = false,
                linkedTransactionId = null,
                payPeriodStartEpochDay = payPeriodStartEpochDay
            )
        )
    }

    /**
     * Toggles a planned budget item between paid and unpaid.
     * - Checking (markPaid = true): Deducts from available balance, adds to total expenses,
     *   records the transaction, and links it so duplicate deductions never occur.
     * - Unchecking (markPaid = false): Deletes the linked expense transaction and reverses
     *   the deduction cleanly.
     */
    suspend fun toggleBudgetAllocationPaid(
        allocationId: Long,
        markPaid: Boolean,
        dateEpochDay: Long = LocalDate.now().toEpochDay()
    ) {
        val allocation = dao.getBudgetAllocationById(allocationId) ?: return
        val existingTx = dao.getTransactionByAllocationId(allocation.id)
            ?: allocation.linkedTransactionId?.let { dao.getTransactionById(it) }

        if (markPaid) {
            if (allocation.isPaid && existingTx != null) {
                // Already marked paid and transaction exists -> prevent duplicate deduction
                return
            }
            val txId = existingTx?.id ?: dao.insertTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    amount = FinanceCalculator.toMoney(allocation.plannedAmount),
                    categoryId = allocation.categoryId,
                    categoryName = allocation.categoryName,
                    budgetAllocationId = allocation.id,
                    description = "${allocation.name} (Planned Budget)",
                    dateEpochDay = dateEpochDay
                )
            )
            dao.updateBudgetAllocation(
                allocation.copy(
                    isPaid = true,
                    linkedTransactionId = txId
                )
            )
        } else {
            // Uncheck: reverse the transaction
            if (existingTx != null) {
                dao.deleteTransaction(existingTx)
            }
            if (allocation.linkedTransactionId != null) {
                dao.deleteTransactionById(allocation.linkedTransactionId)
            }
            dao.updateBudgetAllocation(
                allocation.copy(
                    isPaid = false,
                    linkedTransactionId = null
                )
            )
        }
    }

    suspend fun deleteBudgetAllocation(allocation: BudgetAllocationEntity) {
        // If paid, also remove linked transaction or keep? Let's reverse if unchecked or remove allocation cleanly
        val existingTx = dao.getTransactionByAllocationId(allocation.id)
            ?: allocation.linkedTransactionId?.let { dao.getTransactionById(it) }
        if (existingTx != null) {
            dao.deleteTransaction(existingTx)
        }
        dao.deleteBudgetAllocation(allocation)
    }

    // Payday Management (Section 5)
    suspend fun recordNewPaydayPeriod(
        paydayDate: LocalDate,
        salaryAmount: BigDecimal,
        logSalaryIncome: Boolean,
        notes: String = ""
    ) {
        val settings = dao.getUserSettingsOnce() ?: UserSettingsEntity()
        val paydaySummary = FinanceCalculator.calculatePaydaySummary(
            LocalDate.ofEpochDay(settings.anchorPaydayEpochDay),
            paydayDate
        )
        val txs = dao.getAllTransactionsOnce()
        val cats = dao.getAllCategoriesOnce()
        val finSummary = FinanceCalculator.calculateFinancialSummary(
            txs,
            cats,
            paydaySummary.currentPeriodStart,
            paydaySummary.currentPeriodEnd
        )

        dao.insertPaydayRecord(
            PaydayRecordEntity(
                paydayEpochDay = paydayDate.toEpochDay(),
                periodStartEpochDay = paydayDate.toEpochDay(),
                periodEndEpochDay = paydayDate.plusDays(13).toEpochDay(),
                salaryAmount = FinanceCalculator.toMoney(salaryAmount),
                totalIncomeInPeriod = finSummary.periodIncome.add(
                    if (logSalaryIncome) FinanceCalculator.toMoney(salaryAmount) else FinanceCalculator.ZERO
                ),
                totalExpensesInPeriod = finSummary.periodExpenses,
                totalSavedInPeriod = finSummary.periodSavings,
                notes = notes.ifBlank { "Fortnightly Payday Recorded" }
            )
        )

        if (logSalaryIncome && salaryAmount > FinanceCalculator.ZERO) {
            addIncome(
                amount = salaryAmount,
                incomeSource = "Fortnightly Salary",
                description = "Fortnightly Salary (${FinanceCalculator.formatDate(paydayDate.toEpochDay())})",
                dateEpochDay = paydayDate.toEpochDay()
            )
        }

        dao.insertUserSettings(
            settings.copy(
                anchorPaydayEpochDay = paydayDate.toEpochDay(),
                fortnightlySalary = FinanceCalculator.toMoney(salaryAmount)
            )
        )
    }

    suspend fun updateSettings(transform: (UserSettingsEntity) -> UserSettingsEntity) {
        val current = dao.getUserSettingsOnce() ?: UserSettingsEntity()
        dao.insertUserSettings(transform(current))
    }

    // Optional clearly-labeled Demo Data (Section 17)
    suspend fun loadClearlyLabeledDemoData() {
        ensureDefaultsInitialized()
        val cats = dao.getAllCategoriesOnce()
        val today = LocalDate.now().toEpochDay()
        val settings = dao.getUserSettingsOnce() ?: UserSettingsEntity()

        // 1. Salary Income
        addIncome(
            amount = settings.fortnightlySalary,
            incomeSource = "Fortnightly Salary",
            description = "[DEMO] Fortnightly Wage Credit",
            dateEpochDay = today - 3,
            isDemo = true
        )
        addIncome(
            amount = BigDecimal("180.00"),
            incomeSource = "Overtime",
            description = "[DEMO] Weekend Overtime Allowance",
            dateEpochDay = today - 2,
            isDemo = true
        )

        // 2. Typical PNG Expenses
        val foodCat = cats.find { it.name.contains("Food", true) }
        val transportCat = cats.find { it.name.contains("Transport", true) }
        val flexCat = cats.find { it.name.contains("Flex", true) }
        val relativesCat = cats.find { it.name.contains("Relatives", true) }
        val dinauCat = cats.find { it.name.contains("Dinau", true) }

        if (foodCat != null) {
            addExpense(BigDecimal("145.50"), foodCat.id, foodCat.name, "[DEMO] Gordons Market & Rice 10kg", today - 2, isDemo = true)
        }
        if (transportCat != null) {
            addExpense(BigDecimal("28.00"), transportCat.id, transportCat.name, "[DEMO] PMV Bus Fares", today - 1, isDemo = true)
        }
        if (flexCat != null) {
            addExpense(BigDecimal("10.00"), flexCat.id, flexCat.name, "[DEMO] Rait Kad K10 Data Bundle", today - 1, isDemo = true)
        }
        if (relativesCat != null) {
            addExpense(BigDecimal("80.00"), relativesCat.id, relativesCat.name, "[DEMO] Family Hauslain Support", today, isDemo = true)
        }
        if (dinauCat != null) {
            addExpense(BigDecimal("50.00"), dinauCat.id, dinauCat.name, "[DEMO] Dinau Repayment to Wanpis", today, isDemo = true)
        }

        // 3. Demo Savings Goal & Deposit
        val existingGoals = dao.getAllSavingsGoalsOnce()
        val goalId = if (existingGoals.isEmpty()) {
            createSavingsGoal("Emergency & School Fees [DEMO]", BigDecimal("2000.00"))
        } else {
            existingGoals.first().id
        }
        val goal = dao.getSavingsGoalById(goalId)
        if (goal != null) {
            depositToSavings(goal, BigDecimal("500.00"), "[DEMO] Fortnight Savings Transfer", today, isDemo = true)
        }

        // 4. Demo Planned Budget Checkboxes
        val existingAllocations = dao.getAllBudgetAllocationsOnce()
        if (existingAllocations.isEmpty() && foodCat != null && flexCat != null) {
            val paydaySummary = FinanceCalculator.calculatePaydaySummary(
                LocalDate.ofEpochDay(settings.anchorPaydayEpochDay)
            )
            addBudgetAllocation(
                name = "[DEMO] Second Week Market Shopping",
                plannedAmount = BigDecimal("85.00"),
                category = foodCat,
                payPeriodStartEpochDay = paydaySummary.currentPeriodStart.toEpochDay()
            )
            addBudgetAllocation(
                name = "[DEMO] Top-up Flex / Rait Kad K20",
                plannedAmount = BigDecimal("20.00"),
                category = flexCat,
                payPeriodStartEpochDay = paydaySummary.currentPeriodStart.toEpochDay()
            )
        }
    }

    suspend fun resetAllApplicationData() {
        dao.deleteAllTransactions()
        dao.deleteAllBudgetAllocations()
        dao.deleteAllSavingsGoals()
        dao.deleteAllPaydayRecords()
        dao.deleteAllCategories()
        dao.insertCategories(defaultPngCategories())
        dao.insertUserSettings(UserSettingsEntity())
    }

    // Export to CSV (Section 12)
    suspend fun exportTransactionsToCsv(): String {
        val txs = dao.getAllTransactionsOnce()
        val sb = StringBuilder()
        sb.appendLine("ID,Date,Type,Category,IncomeSource,Description,Amount_PGK,IsDemo")
        for (tx in txs) {
            val dateStr = FinanceCalculator.formatDate(tx.dateEpochDay)
            val safeDesc = tx.description.replace("\"", "\"\"")
            val safeCat = tx.categoryName.replace("\"", "\"\"")
            val safeSource = (tx.incomeSource ?: "").replace("\"", "\"\"")
            sb.appendLine(
                "${tx.id},\"$dateStr\",${tx.type},\"$safeCat\",\"$safeSource\",\"$safeDesc\",${tx.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()},${tx.isDemo}"
            )
        }
        return sb.toString()
    }

    // Backup & Restore JSON (Section 12 & 15)
    suspend fun createBackupJson(): String {
        val root = JSONObject()
        root.put("app", "PayWise PNG")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val settings = dao.getUserSettingsOnce() ?: UserSettingsEntity()
        val settingsObj = JSONObject().apply {
            put("currencyFormat", settings.currencyFormat)
            put("anchorPaydayEpochDay", settings.anchorPaydayEpochDay)
            put("fortnightlySalary", settings.fortnightlySalary.toPlainString())
            put("darkModeSetting", settings.darkModeSetting)
            put("pinCode", settings.pinCode ?: "")
            put("biometricEnabled", settings.biometricEnabled)
        }
        root.put("settings", settingsObj)

        val categoriesArr = JSONArray()
        dao.getAllCategoriesOnce().forEach { cat ->
            categoriesArr.put(JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("budgetAmount", cat.budgetAmount.toPlainString())
                put("iconName", cat.iconName)
                put("colorHex", cat.colorHex)
                put("isDefault", cat.isDefault)
            })
        }
        root.put("categories", categoriesArr)

        val goalsArr = JSONArray()
        dao.getAllSavingsGoalsOnce().forEach { g ->
            goalsArr.put(JSONObject().apply {
                put("id", g.id)
                put("name", g.name)
                put("targetAmount", g.targetAmount.toPlainString())
                put("currentAmount", g.currentAmount.toPlainString())
                put("createdAt", g.createdAt)
            })
        }
        root.put("savingsGoals", goalsArr)

        val allocationsArr = JSONArray()
        dao.getAllBudgetAllocationsOnce().forEach { a ->
            allocationsArr.put(JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("plannedAmount", a.plannedAmount.toPlainString())
                put("categoryId", a.categoryId)
                put("categoryName", a.categoryName)
                put("isPaid", a.isPaid)
                if (a.linkedTransactionId != null) put("linkedTransactionId", a.linkedTransactionId)
                put("payPeriodStartEpochDay", a.payPeriodStartEpochDay)
            })
        }
        root.put("budgetAllocations", allocationsArr)

        val txsArr = JSONArray()
        dao.getAllTransactionsOnce().forEach { tx ->
            txsArr.put(JSONObject().apply {
                put("id", tx.id)
                put("type", tx.type)
                put("amount", tx.amount.toPlainString())
                if (tx.categoryId != null) put("categoryId", tx.categoryId)
                put("categoryName", tx.categoryName)
                if (tx.incomeSource != null) put("incomeSource", tx.incomeSource)
                if (tx.savingsGoalId != null) put("savingsGoalId", tx.savingsGoalId)
                if (tx.budgetAllocationId != null) put("budgetAllocationId", tx.budgetAllocationId)
                put("description", tx.description)
                put("dateEpochDay", tx.dateEpochDay)
                put("timestamp", tx.timestamp)
                put("isDemo", tx.isDemo)
            })
        }
        root.put("transactions", txsArr)

        return root.toString(2)
    }

    suspend fun restoreFromBackupJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("transactions") && !root.has("categories")) return false

            dao.deleteAllTransactions()
            dao.deleteAllBudgetAllocations()
            dao.deleteAllSavingsGoals()
            dao.deleteAllCategories()

            root.optJSONObject("settings")?.let { s ->
                val pin = s.optString("pinCode", "").ifBlank { null }
                dao.insertUserSettings(
                    UserSettingsEntity(
                        id = 1,
                        currencyFormat = s.optString("currencyFormat", "K"),
                        anchorPaydayEpochDay = s.optLong("anchorPaydayEpochDay", FinanceCalculator.DEFAULT_PAYDAY.toEpochDay()),
                        fortnightlySalary = FinanceCalculator.toMoney(s.optString("fortnightlySalary", "1450.00")),
                        darkModeSetting = s.optString("darkModeSetting", "DARK"),
                        pinCode = pin,
                        biometricEnabled = s.optBoolean("biometricEnabled", false)
                    )
                )
            }

            root.optJSONArray("categories")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.insertCategory(
                        BudgetCategoryEntity(
                            id = o.optLong("id", 0),
                            name = o.getString("name"),
                            budgetAmount = FinanceCalculator.toMoney(o.optString("budgetAmount", "0.00")),
                            iconName = o.optString("iconName", "Category"),
                            colorHex = o.optString("colorHex", "#1B7A4E"),
                            isDefault = o.optBoolean("isDefault", false)
                        )
                    )
                }
            }

            root.optJSONArray("savingsGoals")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.insertSavingsGoal(
                        SavingsGoalEntity(
                            id = o.optLong("id", 0),
                            name = o.getString("name"),
                            targetAmount = FinanceCalculator.toMoney(o.optString("targetAmount", "0.00")),
                            currentAmount = FinanceCalculator.toMoney(o.optString("currentAmount", "0.00")),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            root.optJSONArray("budgetAllocations")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val linkedTxId = if (o.has("linkedTransactionId")) o.getLong("linkedTransactionId") else null
                    dao.insertBudgetAllocation(
                        BudgetAllocationEntity(
                            id = o.optLong("id", 0),
                            name = o.getString("name"),
                            plannedAmount = FinanceCalculator.toMoney(o.optString("plannedAmount", "0.00")),
                            categoryId = o.optLong("categoryId", 0),
                            categoryName = o.optString("categoryName", "Other Expenses"),
                            isPaid = o.optBoolean("isPaid", false),
                            linkedTransactionId = linkedTxId,
                            payPeriodStartEpochDay = o.optLong("payPeriodStartEpochDay", LocalDate.now().toEpochDay())
                        )
                    )
                }
            }

            root.optJSONArray("transactions")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    dao.insertTransaction(
                        TransactionEntity(
                            id = o.optLong("id", 0),
                            type = o.getString("type"),
                            amount = FinanceCalculator.toMoney(o.optString("amount", "0.00")),
                            categoryId = if (o.has("categoryId")) o.getLong("categoryId") else null,
                            categoryName = o.optString("categoryName", ""),
                            incomeSource = if (o.has("incomeSource")) o.getString("incomeSource") else null,
                            savingsGoalId = if (o.has("savingsGoalId")) o.getLong("savingsGoalId") else null,
                            budgetAllocationId = if (o.has("budgetAllocationId")) o.getLong("budgetAllocationId") else null,
                            description = o.optString("description", ""),
                            dateEpochDay = o.optLong("dateEpochDay", LocalDate.now().toEpochDay()),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                            isDemo = o.optBoolean("isDemo", false)
                        )
                    )
                }
            }

            ensureDefaultsInitialized()
            true
        } catch (e: Exception) {
            false
        }
    }
}
