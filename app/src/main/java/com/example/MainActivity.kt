package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.BudgetCategoryEntity
import com.example.data.local.PayWiseDatabase
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.repository.PayWiseRepository
import com.example.domain.FinanceCalculator
import com.example.ui.MainTab
import com.example.ui.PayWiseUiState
import com.example.ui.PayWiseViewModel
import com.example.ui.components.AddOrEditExpenseDialog
import com.example.ui.components.AddOrEditIncomeDialog
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.PaydayConfigDialog
import com.example.ui.components.SavingsActionDialog
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.PayWiseTheme
import com.example.ui.theme.PngGold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = PayWiseDatabase.getInstance(applicationContext)
        val repository = PayWiseRepository(database.payWiseDao())
        val factory = PayWiseViewModel.Factory(repository)

        setContent {
            val viewModel: PayWiseViewModel = viewModel(factory = factory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (uiState.settings.darkModeSetting) {
                "LIGHT" -> false
                "SYSTEM" -> systemDark
                else -> true
            }

            PayWiseTheme(darkTheme = useDarkTheme) {
                val requiresPin = !uiState.settings.pinCode.isNullOrBlank() && !isUnlocked
                if (requiresPin) {
                    PinLockScreen(
                        biometricEnabled = uiState.settings.biometricEnabled,
                        onAttemptPin = { viewModel.unlockWithPin(it) },
                        onBiometricUnlock = { viewModel.unlockWithBiometricSimulate() }
                    )
                } else {
                    PayWiseMainApp(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayWiseMainApp(
    uiState: PayWiseUiState,
    viewModel: PayWiseViewModel
) {
    var currentTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog states
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var preselectedExpenseCategory by remember { mutableStateOf<BudgetCategoryEntity?>(null) }
    var presetExpenseAmount by remember { mutableStateOf<String?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var deletingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var deletingCategory by remember { mutableStateOf<BudgetCategoryEntity?>(null) }
    var deletingSavingsGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var showSavingsDialog by remember { mutableStateOf(false) }
    var showPaydayDialog by remember { mutableStateOf(false) }

    // BackHandler for non-Dashboard tabs
    BackHandler(enabled = currentTab != MainTab.DASHBOARD) {
        currentTab = MainTab.DASHBOARD
    }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = PngGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "PayWise PNG 🇵🇬",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Fortnightly Finance & Budget Planner",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = FinanceCalculator.formatCurrency(
                                    uiState.financialSummary.availableBalance,
                                    uiState.settings.currencyFormat
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = currentTab == MainTab.DASHBOARD,
                            onClick = { currentTab = MainTab.DASHBOARD },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") },
                            modifier = Modifier.testTag("nav_tab_dashboard")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.BUDGET,
                            onClick = { currentTab = MainTab.BUDGET },
                            icon = { Icon(Icons.Default.PieChart, contentDescription = "Budget") },
                            label = { Text("Budget") },
                            modifier = Modifier.testTag("nav_tab_budget")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.TRANSACTIONS,
                            onClick = { currentTab = MainTab.TRANSACTIONS },
                            icon = { Icon(Icons.Default.History, contentDescription = "Transactions") },
                            label = { Text("Transactions") },
                            modifier = Modifier.testTag("nav_tab_transactions")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.SETTINGS,
                            onClick = { currentTab = MainTab.SETTINGS },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        NavigationRailItem(
                            selected = currentTab == MainTab.DASHBOARD,
                            onClick = { currentTab = MainTab.DASHBOARD },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") },
                            modifier = Modifier.testTag("nav_tab_dashboard")
                        )
                        NavigationRailItem(
                            selected = currentTab == MainTab.BUDGET,
                            onClick = { currentTab = MainTab.BUDGET },
                            icon = { Icon(Icons.Default.PieChart, contentDescription = "Budget") },
                            label = { Text("Budget") },
                            modifier = Modifier.testTag("nav_tab_budget")
                        )
                        NavigationRailItem(
                            selected = currentTab == MainTab.TRANSACTIONS,
                            onClick = { currentTab = MainTab.TRANSACTIONS },
                            icon = { Icon(Icons.Default.History, contentDescription = "Transactions") },
                            label = { Text("Transactions") },
                            modifier = Modifier.testTag("nav_tab_transactions")
                        )
                        NavigationRailItem(
                            selected = currentTab == MainTab.SETTINGS,
                            onClick = { currentTab = MainTab.SETTINGS },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }

                when (currentTab) {
                    MainTab.DASHBOARD -> {
                        DashboardScreen(
                            uiState = uiState,
                            onAddIncomeClick = { showAddIncomeDialog = true },
                            onAddExpenseClick = {
                                preselectedExpenseCategory = null
                                presetExpenseAmount = null
                                showAddExpenseDialog = true
                            },
                            onAddSavingsClick = { showSavingsDialog = true },
                            onViewBudgetClick = { currentTab = MainTab.BUDGET },
                            onConfigurePaydayClick = { showPaydayDialog = true },
                            onEditTransaction = { editingTransaction = it },
                            onDeleteTransaction = { deletingTransaction = it },
                            onLoadDemoData = { viewModel.loadDemoData() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    MainTab.BUDGET -> {
                        BudgetScreen(
                            uiState = uiState,
                            onAddAllocation = { name, amountStr, category ->
                                viewModel.addBudgetAllocation(name, amountStr, category)
                            },
                            onToggleAllocationPaid = { alloc, paid ->
                                viewModel.toggleBudgetAllocationPaid(alloc, paid)
                            },
                            onDeleteAllocation = { alloc ->
                                viewModel.deleteBudgetAllocation(alloc)
                            },
                            onAddCategory = { name, budgetStr ->
                                viewModel.addCategory(name, budgetStr)
                            },
                            onUpdateCategory = { cat, newName, newBudget ->
                                viewModel.updateCategory(cat, newName, newBudget)
                            },
                            onDeleteCategory = { cat ->
                                deletingCategory = cat
                            },
                            onQuickAddExpenseForCategory = { cat, presetAmt ->
                                if (presetAmt != null) {
                                    // Direct 1-tap recording for Flex / Rait Kad K3/K5/K10/K20
                                    viewModel.addExpense(
                                        amountStr = presetAmt,
                                        categoryId = cat.id,
                                        categoryName = cat.name,
                                        description = "${cat.name} K${presetAmt.substringBefore(".")}"
                                    )
                                } else {
                                    preselectedExpenseCategory = cat
                                    presetExpenseAmount = null
                                    showAddExpenseDialog = true
                                }
                            },
                            onOpenSavingsDialog = { showSavingsDialog = true },
                            onDeleteSavingsGoal = { deletingSavingsGoal = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    MainTab.TRANSACTIONS -> {
                        TransactionsScreen(
                            uiState = uiState,
                            onAddIncomeClick = { showAddIncomeDialog = true },
                            onAddExpenseClick = {
                                preselectedExpenseCategory = null
                                presetExpenseAmount = null
                                showAddExpenseDialog = true
                            },
                            onEditTransaction = { editingTransaction = it },
                            onDeleteTransaction = { deletingTransaction = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    MainTab.SETTINGS -> {
                        SettingsScreen(
                            uiState = uiState,
                            onCurrencyChange = { viewModel.setCurrencyFormat(it) },
                            onDarkModeChange = { viewModel.setDarkModeSetting(it) },
                            onConfigurePaydayClick = { showPaydayDialog = true },
                            onManageCategoriesClick = { currentTab = MainTab.BUDGET },
                            onUpdateSecurity = { pin, bio ->
                                viewModel.updateSecuritySettings(pin, bio)
                            },
                            onGenerateCsv = { viewModel.generateCsvExport() },
                            onGenerateBackupJson = { viewModel.generateBackupJson() },
                            onRestoreBackupJson = { viewModel.restoreFromBackupJson(it) },
                            onLoadDemoData = { viewModel.loadDemoData() },
                            onResetAllData = { viewModel.resetAllData() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddIncomeDialog) {
        AddOrEditIncomeDialog(
            initialTransaction = null,
            currencyFormat = uiState.settings.currencyFormat,
            defaultSalary = uiState.settings.fortnightlySalary,
            onDismiss = { showAddIncomeDialog = false },
            onSave = { amountStr, source, desc, epochDay ->
                viewModel.addIncome(amountStr, source, desc, epochDay)
            }
        )
    }

    if (showAddExpenseDialog) {
        AddOrEditExpenseDialog(
            categories = uiState.categories,
            initialTransaction = null,
            preselectedCategory = preselectedExpenseCategory,
            currencyFormat = uiState.settings.currencyFormat,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { amountStr, catId, catName, desc, epochDay ->
                viewModel.addExpense(amountStr, catId, catName, desc, epochDay)
            }
        )
    }

    editingTransaction?.let { tx ->
        if (tx.type == TransactionType.INCOME) {
            AddOrEditIncomeDialog(
                initialTransaction = tx,
                currencyFormat = uiState.settings.currencyFormat,
                defaultSalary = uiState.settings.fortnightlySalary,
                onDismiss = { editingTransaction = null },
                onSave = { amountStr, source, desc, epochDay ->
                    viewModel.updateTransaction(
                        tx.copy(
                            amount = FinanceCalculator.toMoney(amountStr),
                            incomeSource = source,
                            categoryName = source,
                            description = desc.ifBlank { source },
                            dateEpochDay = epochDay
                        )
                    )
                }
            )
        } else {
            AddOrEditExpenseDialog(
                categories = uiState.categories,
                initialTransaction = tx,
                currencyFormat = uiState.settings.currencyFormat,
                onDismiss = { editingTransaction = null },
                onSave = { amountStr, catId, catName, desc, epochDay ->
                    viewModel.updateTransaction(
                        tx.copy(
                            amount = FinanceCalculator.toMoney(amountStr),
                            categoryId = catId,
                            categoryName = catName,
                            description = desc.ifBlank { catName },
                            dateEpochDay = epochDay
                        )
                    )
                }
            )
        }
    }

    deletingTransaction?.let { tx ->
        ConfirmDeleteDialog(
            title = "Delete Financial Record?",
            message = "Are you sure you want to delete '${tx.description}' (${FinanceCalculator.formatCurrency(tx.amount, uiState.settings.currencyFormat)})? This will automatically recalculate your balance.",
            onConfirm = { viewModel.deleteTransaction(tx) },
            onDismiss = { deletingTransaction = null }
        )
    }

    deletingCategory?.let { cat ->
        ConfirmDeleteDialog(
            title = "Delete Category '${cat.name}'?",
            message = "Are you sure you want to remove this custom budget category?",
            onConfirm = { viewModel.deleteCategory(cat) },
            onDismiss = { deletingCategory = null }
        )
    }

    deletingSavingsGoal?.let { goal ->
        ConfirmDeleteDialog(
            title = "Delete Savings Goal '${goal.name}'?",
            message = "Deleting this savings goal will also remove its savings deposit/withdrawal records.",
            onConfirm = { viewModel.deleteSavingsGoal(goal) },
            onDismiss = { deletingSavingsGoal = null }
        )
    }

    if (showSavingsDialog) {
        SavingsActionDialog(
            savingsGoals = uiState.savingsGoals,
            currencyFormat = uiState.settings.currencyFormat,
            onDismiss = { showSavingsDialog = false },
            onCreateGoal = { name, targetStr ->
                viewModel.createSavingsGoal(name, targetStr)
            },
            onDeposit = { goal, amountStr, desc ->
                viewModel.depositToSavings(goal, amountStr, desc)
            },
            onWithdraw = { goal, amountStr, desc ->
                viewModel.withdrawFromSavings(goal, amountStr, desc)
            }
        )
    }

    if (showPaydayDialog) {
        PaydayConfigDialog(
            currentPaydayEpochDay = uiState.settings.anchorPaydayEpochDay,
            currentSalary = uiState.settings.fortnightlySalary,
            currencyFormat = uiState.settings.currencyFormat,
            onDismiss = { showPaydayDialog = false },
            onSave = { newEpochDay, salaryStr, logNow, notes ->
                viewModel.updatePaydayAndSalary(newEpochDay, salaryStr, logNow, notes)
            }
        )
    }
}

@Composable
private fun PinLockScreen(
    biometricEnabled: Boolean,
    onAttemptPin: (String) -> Boolean,
    onBiometricUnlock: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PngGold,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "PayWise PNG 🇵🇬 Locked",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enter your 4-digit PIN to access your offline financial records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                            enteredPin = it
                            errorMsg = null
                        }
                    },
                    label = { Text("4-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        if (!onAttemptPin(enteredPin)) {
                            errorMsg = "Incorrect PIN. Please try again."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock PayWise PNG")
                }

                if (biometricEnabled) {
                    FilledTonalButton(
                        onClick = onBiometricUnlock,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock with Biometrics")
                    }
                }
            }
        }
    }
}
