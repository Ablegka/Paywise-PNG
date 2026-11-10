package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.FinanceCalculator
import com.example.ui.PayWiseUiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.PngBrightEmerald
import com.example.ui.theme.PngGold
import com.example.ui.theme.RobotoMonoFontFamily
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    uiState: PayWiseUiState,
    onCurrencyChange: (String) -> Unit,
    onDarkModeChange: (String) -> Unit,
    onConfigurePaydayClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onUpdateSecurity: (pinCode: String?, biometricEnabled: Boolean) -> Unit,
    onGenerateCsv: suspend () -> String,
    onGenerateBackupJson: suspend () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onLoadDemoData: () -> Unit,
    onResetAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = uiState.settings

    var showPinDialog by remember { mutableStateOf(false) }
    var showBackupPreviewDialog by remember { mutableStateOf<String?>(null) }
    var showCsvPreviewDialog by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Currency & Display Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Currency Display (Papua New Guinea Kina)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "K" to "K (e.g. K1,450.00)",
                            "PGK" to "PGK (e.g. PGK 1,450.00)",
                            "KINA" to "Kina"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = settings.currencyFormat == code,
                                onClick = { onCurrencyChange(code) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("currency_chip_$code")
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Appearance & Theme",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "DARK" to "Dark Green",
                            "LIGHT" to "Light White",
                            "SYSTEM" to "System"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = settings.darkModeSetting == mode,
                                onClick = { onDarkModeChange(mode) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("theme_chip_$mode")
                            )
                        }
                    }
                }
            }
        }

        // 2. Payday, Salary & Historical Payday Records
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = PngGold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Payday & Fortnightly Salary",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Next Payday: ${FinanceCalculator.formatDate(uiState.paydaySummary.nextPayday.toEpochDay())} (${uiState.paydaySummary.daysRemaining}d left)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = onConfigurePaydayClick,
                            modifier = Modifier.testTag("settings_change_payday_button")
                        ) {
                            Text("Configure")
                        }
                    }

                    Text(
                        text = "Fortnightly Salary: ${FinanceCalculator.formatCurrency(settings.fortnightlySalary, settings.currencyFormat)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = RobotoMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = PngBrightEmerald
                    )

                    if (uiState.paydayRecords.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "Historical Payday Records (${uiState.paydayRecords.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = PngGold
                        )
                        uiState.paydayRecords.take(4).forEach { rec ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${FinanceCalculator.formatDate(rec.paydayEpochDay)} • ${rec.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = FinanceCalculator.formatCurrency(rec.salaryAmount, settings.currencyFormat),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = RobotoMonoFontFamily),
                                    color = PngBrightEmerald
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Budget Categories (${uiState.categories.size})",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        FilledTonalButton(onClick = onManageCategoriesClick) {
                            Text("Manage Categories")
                        }
                    }
                }
            }
        }

        // 3. Security & Data Protection (Section 15)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Security & Offline Privacy",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "All data is stored 100% locally on your device. Optional PIN & Biometric lock.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (settings.pinCode.isNullOrBlank()) "PIN Protection: Disabled"
                            else "PIN Protection: Active (4-digit PIN)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(
                            onClick = { showPinDialog = true },
                            modifier = Modifier.testTag("configure_pin_button")
                        ) {
                            Text(if (settings.pinCode.isNullOrBlank()) "Set PIN" else "Change / Disable PIN")
                        }
                    }

                    if (!settings.pinCode.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = PngGold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Biometric Unlock",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Switch(
                                checked = settings.biometricEnabled,
                                onCheckedChange = { enabled ->
                                    onUpdateSecurity(settings.pinCode, enabled)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. Backup, Restore, CSV Export & Reset (Section 12)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Backup, Restore & CSV Export",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    showBackupPreviewDialog = onGenerateBackupJson()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("backup_data_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup Data")
                        }
                        FilledTonalButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_data_button")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Data")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    showCsvPreviewDialog = onGenerateCsv()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export to CSV")
                        }
                        OutlinedButton(
                            onClick = onLoadDemoData,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Load [DEMO] Data")
                        }
                    }

                    HorizontalDivider()

                    Button(
                        onClick = { showResetConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_app_data_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Application Data")
                    }
                }
            }
        }

        // 5. About PayWise PNG (Section 16 Exact Identity Display)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_paywise_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PngGold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.about_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.about_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.about_audience),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PngBrightEmerald
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.about_developer),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(id = R.string.about_author),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = stringResource(id = R.string.about_handle),
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = RobotoMonoFontFamily),
                                color = PngGold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        PinSetupDialog(
            currentPin = settings.pinCode,
            biometricEnabled = settings.biometricEnabled,
            onDismiss = { showPinDialog = false },
            onSave = { newPin, bio ->
                onUpdateSecurity(newPin, bio)
            }
        )
    }

    showBackupPreviewDialog?.let { backupJson ->
        AlertDialog(
            onDismissRequest = { showBackupPreviewDialog = null },
            title = { Text("Backup Data (JSON)") },
            text = {
                OutlinedTextField(
                    value = backupJson,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_SUBJECT, "PayWise PNG Backup")
                            putExtra(Intent.EXTRA_TEXT, backupJson)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share PayWise PNG Backup"))
                        showBackupPreviewDialog = null
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share / Save Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupPreviewDialog = null }) {
                    Text("Close")
                }
            }
        )
    }

    showCsvPreviewDialog?.let { csvContent ->
        AlertDialog(
            onDismissRequest = { showCsvPreviewDialog = null },
            title = { Text("Export Transactions (CSV)") },
            text = {
                OutlinedTextField(
                    value = csvContent,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("csv_export_preview_text")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "PayWise_PNG_Transactions.csv")
                            putExtra(Intent.EXTRA_TEXT, csvContent)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Export CSV"))
                        showCsvPreviewDialog = null
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvPreviewDialog = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (showRestoreDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Backup Data") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste your PayWise PNG backup JSON below to restore your categories, transactions, savings goals, and settings:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        label = { Text("Backup JSON") },
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackupJson(jsonInput)
                        showRestoreDialog = false
                    }
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        ConfirmDeleteDialog(
            title = "Reset All Application Data?",
            message = "This will permanently delete all recorded transactions, savings goals, and custom categories, and restore PayWise PNG to default settings.",
            onConfirm = onResetAllData,
            onDismiss = { showResetConfirmDialog = false }
        )
    }
}

@Composable
private fun PinSetupDialog(
    currentPin: String?,
    biometricEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (String?, Boolean) -> Unit
) {
    var pinInput by remember { mutableStateOf(currentPin ?: "") }
    var bio by remember { mutableStateOf(biometricEnabled) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN & Biometric Protection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter a 4-digit PIN to protect your financial records on startup, or leave blank to disable PIN lock.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                            pinInput = it
                            errorText = null
                        }
                    },
                    label = { Text("4-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Biometric Unlock", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = bio, onCheckedChange = { bio = it })
                }
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinInput.isNotEmpty() && pinInput.length != 4) {
                        errorText = "PIN must be exactly 4 digits (or empty to disable)"
                    } else {
                        onSave(pinInput.ifBlank { null }, bio)
                        onDismiss()
                    }
                }
            ) {
                Text("Save Security")
            }
        },
        dismissButton = {
            if (!currentPin.isNullOrBlank()) {
                TextButton(
                    onClick = {
                        onSave(null, false)
                        onDismiss()
                    }
                ) {
                    Text("Disable PIN", color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
