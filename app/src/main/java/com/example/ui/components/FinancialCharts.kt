package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.CategoryBreakdown
import com.example.domain.FinanceCalculator
import com.example.ui.theme.ExpenseRedDark
import com.example.ui.theme.IncomeGreenDark
import com.example.ui.theme.PngBrightEmerald
import com.example.ui.theme.PngGold
import com.example.ui.theme.RobotoMonoFontFamily
import java.math.BigDecimal
import java.math.RoundingMode

fun getCategoryIcon(categoryName: String, iconName: String = ""): ImageVector {
    val key = (categoryName + " " + iconName).lowercase()
    return when {
        key.contains("dinau") || key.contains("handshake") -> Icons.Default.Handshake
        key.contains("relative") || key.contains("group") || key.contains("family") -> Icons.Default.Groups
        key.contains("food") || key.contains("market") || key.contains("restaurant") -> Icons.Default.Restaurant
        key.contains("transport") || key.contains("bus") || key.contains("pmv") -> Icons.Default.DirectionsBus
        key.contains("phone") || key.contains("credit") -> Icons.Default.PhoneAndroid
        key.contains("flex") || key.contains("rait") || key.contains("sim") -> Icons.Default.SimCard
        key.contains("saving") -> Icons.Default.Savings
        key.contains("salary") || key.contains("income") -> Icons.Default.AccountBalanceWallet
        key.contains("other") || key.contains("receipt") -> Icons.Default.ReceiptLong
        else -> Icons.Default.Category
    }
}

fun parseHexColor(hex: String, fallback: Color = PngBrightEmerald): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val longVal = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(longVal or 0xFF000000L)
        } else {
            Color(longVal)
        }
    } catch (e: Exception) {
        fallback
    }
}

@Composable
fun IncomeVsExpensesChartCard(
    totalIncome: BigDecimal,
    totalExpenses: BigDecimal,
    currentSavings: BigDecimal,
    currencyFormat: String,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(totalIncome, totalExpenses, currentSavings, BigDecimal("1.00"))
    val incomeRatio by animateFloatAsState(
        targetValue = if (totalIncome > FinanceCalculator.ZERO) {
            totalIncome.divide(maxVal, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0.04f, 1f)
        } else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "incomeRatio"
    )
    val expenseRatio by animateFloatAsState(
        targetValue = if (totalExpenses > FinanceCalculator.ZERO) {
            totalExpenses.divide(maxVal, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0.04f, 1f)
        } else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "expenseRatio"
    )
    val savingsRatio by animateFloatAsState(
        targetValue = if (currentSavings > FinanceCalculator.ZERO) {
            currentSavings.divide(maxVal, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0.04f, 1f)
        } else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "savingsRatio"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Income vs Expenses & Savings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Cashflow",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal Comparison Bars
            ComparisonBarRow(
                label = "Income",
                amountText = FinanceCalculator.formatCurrency(totalIncome, currencyFormat),
                fraction = incomeRatio,
                barColor = IncomeGreenDark
            )
            Spacer(modifier = Modifier.height(10.dp))
            ComparisonBarRow(
                label = "Expenses",
                amountText = FinanceCalculator.formatCurrency(totalExpenses, currencyFormat),
                fraction = expenseRatio,
                barColor = ExpenseRedDark
            )
            Spacer(modifier = Modifier.height(10.dp))
            ComparisonBarRow(
                label = "Savings",
                amountText = FinanceCalculator.formatCurrency(currentSavings, currencyFormat),
                fraction = savingsRatio,
                barColor = PngGold
            )
        }
    }
}

@Composable
private fun ComparisonBarRow(
    label: String,
    amountText: String,
    fraction: Float,
    barColor: Color
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = amountText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            drawRoundRect(
                color = trackColor,
                size = size,
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )
            if (fraction > 0f) {
                drawRoundRect(
                    color = barColor,
                    size = Size(width = size.width * fraction.coerceIn(0f, 1f), height = size.height),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun SpendingBreakdownChartCard(
    breakdowns: List<CategoryBreakdown>,
    totalExpenses: BigDecimal,
    currencyFormat: String,
    modifier: Modifier = Modifier
) {
    val activeBreakdowns = breakdowns.filter { it.spentAmount > FinanceCalculator.ZERO }
        .sortedByDescending { it.spentAmount }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Spending Breakdown",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (activeBreakdowns.isEmpty() || totalExpenses <= FinanceCalculator.ZERO) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses recorded yet. Record an expense or check off a budget item to see your category breakdown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Donut Chart Canvas
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(104.dp)) {
                            val strokeWidth = 20.dp.toPx()
                            var startAngle = -90f
                            activeBreakdowns.forEach { item ->
                                val sweep = item.spentAmount
                                    .divide(totalExpenses, 4, RoundingMode.HALF_UP)
                                    .toFloat() * 360f
                                drawArc(
                                    color = parseHexColor(item.category.colorHex),
                                    startAngle = startAngle,
                                    sweepAngle = sweep.coerceAtLeast(2f),
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                    size = Size(size.width - strokeWidth, size.height - strokeWidth)
                                )
                                startAngle += sweep
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FinanceCalculator.formatCurrency(totalExpenses, currencyFormat),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = RobotoMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }

                    // Category Legend
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeBreakdowns.take(5).forEach { item ->
                            val pct = item.spentAmount
                                .multiply(BigDecimal("100"))
                                .divide(totalExpenses, 0, RoundingMode.HALF_UP)
                                .toInt()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColor(item.category.colorHex))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.category.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "$pct% (${FinanceCalculator.formatCurrency(item.spentAmount, currencyFormat)})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = RobotoMonoFontFamily
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
