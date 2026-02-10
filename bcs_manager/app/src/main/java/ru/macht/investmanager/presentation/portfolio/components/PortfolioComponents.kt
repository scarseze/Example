package ru.macht.investmanager.presentation.portfolio.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.macht.investmanager.domain.model.PortfolioAsset
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PortfolioList(
    positions: List<PortfolioAsset>, 
    totalValue: Double,
    totalProfit: Double,
    onDelete: (String) -> Unit = {},
    isManual: Boolean = false
) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Стоимость портфеля", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = formatMoney(totalValue),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val profitColor = if (totalProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                val profitSign = if (totalProfit > 0) "+" else ""
                Text(
                    text = "$profitSign${formatMoney(totalProfit)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = profitColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(positions) { position ->
                PositionItem(position, onDelete, isManual)
            }
        }
    }
}

@Composable
fun PositionItem(position: PortfolioAsset, onDelete: (String) -> Unit, isManual: Boolean) {
    val marketPrice = position.currentPrice ?: position.averagePrice
    val profit = if (position.currentPrice != null) {
        (marketPrice - position.averagePrice) * position.quantity
    } else 0.0
    
    val profitColor = if (profit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    val profitSign = if (profit > 0) "+" else ""

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = position.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "${position.quantity.toInt()} шт. • ${formatMoney(position.averagePrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = formatMoney(position.quantity * marketPrice), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (position.currentPrice != null) {
                    Text(text = "$profitSign${formatMoney(profit)}", style = MaterialTheme.typography.bodySmall, color = profitColor, fontWeight = FontWeight.Bold)
                }
            }
            if (isManual) {
                IconButton(onClick = { onDelete(position.ticker) }) {
                    Text("🗑️")
                }
            }
        }
    }
}

@Composable
fun AddAssetDialog(onDismiss: () -> Unit, onConfirm: (String, Double, Double) -> Unit) {
    var ticker by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Добавить актив") },
        text = {
            Column {
                OutlinedTextField(
                    value = ticker,
                    onValueChange = { ticker = it },
                    label = { Text("Тикер (например, SBER)") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Количество") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Цена покупки (RUB)") },
                    supportingText = { Text("Нужно для расчета прибыли") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = quantity.replace(",", ".").trim().toDoubleOrNull()
                    val p = price.replace(",", ".").trim().toDoubleOrNull()
                    val t = ticker.trim()
                    if (t.isNotBlank() && q != null && p != null) {
                        onConfirm(t, q, p)
                    }
                }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

fun formatMoney(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("ru", "RU"))
    return format.format(amount)
}
