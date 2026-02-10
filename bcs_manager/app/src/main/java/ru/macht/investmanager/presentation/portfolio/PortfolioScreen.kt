package ru.macht.investmanager.presentation.portfolio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.macht.investmanager.domain.model.PortfolioAsset
import ru.macht.investmanager.domain.model.BrokerType
import ru.macht.investmanager.domain.model.PortfolioResult
import ru.macht.investmanager.presentation.common.WarningBanner
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel = hiltViewModel(),
    onNavigateToNews: () -> Unit,
    onNavigateToAddAsset: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Состояние видимости сумм (Глаз)
    var isValueVisible by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои Инвестиции") },
                actions = {
                    // Кнопка Глаз
                    IconButton(onClick = { isValueVisible = !isValueVisible }) {
                        val iconRes = if (isValueVisible) android.R.drawable.ic_menu_view else android.R.drawable.ic_secure
                        // Используем стандартные иконки или заглушку, если нет ресурсов
                        // Используем стандартные иконки или заглушку, если нет ресурсов
                        // Для надежности используем текст или вектор, если ресурсы не подтянуты
                        Text(if (isValueVisible) "👁" else "🙈", style = MaterialTheme.typography.titleLarge)
                    }
                    // Кнопка Аналитики (ОФЗ)
                    IconButton(onClick = onNavigateToAnalytics) {
                        // Используем иконку, похожую на график. DateRange или Info пока сойдет, лучше бы ShowChart
                         Icon(Icons.Default.Info, contentDescription = "Аналитика ОФЗ")
                    }
                    TextButton(onClick = onNavigateToNews) {
                        Text("Новости")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddAsset) {
                Icon(Icons.Default.Add, contentDescription = "Добавить актив")
            }
        }
    ) { padding ->
        val isRefreshing = uiState is PortfolioUiState.Loading
        val pullRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.loadPortfolio() },
            state = pullRefreshState,
            modifier = Modifier.padding(padding)
        ) {
            when (val currentState = uiState) {
                is PortfolioUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize())
                }
                is PortfolioUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Ошибка: ${currentState.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = viewModel::loadPortfolio) {
                            Text("Повторить")
                        }
                    }
                }
                is PortfolioUiState.Success -> {
                    PortfolioContent(
                        result = currentState.result,
                        isValueVisible = isValueVisible,
                        onDeleteAsset = viewModel::deleteAsset
                    )
                }
            }
        }
    }
}

@Composable
fun PortfolioContent(
    result: PortfolioResult,
    isValueVisible: Boolean,
    onDeleteAsset: (String) -> Unit
) {
    var showErrorDialog by remember { mutableStateOf(false) }

    if (showErrorDialog && result.bcsErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Ошибка загрузки BCS") },
            text = { Text(result.bcsErrorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        
        if (result.hasBcsError) {
            WarningBanner(
                message = "Частичный сбой: Нажмите для деталей",
                onClick = { showErrorDialog = true }
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                PortfolioHeader(
                    totalValue = result.totalValue,
                    totalProfit = result.totalProfit,
                    isVisible = isValueVisible
                )
            }

            // Группировка по типу актива
            val groupedAssets = result.assets.groupBy { it.type }
            
            groupedAssets.forEach { (type, assets) ->
                item {
                    ExpandableAssetGroup(
                        title = type,
                        assets = assets,
                        isValueVisible = isValueVisible,
                        onDeleteAsset = onDeleteAsset
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandableAssetGroup(
    title: String,
    assets: List<PortfolioAsset>,
    isValueVisible: Boolean,
    onDeleteAsset: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$title (${assets.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    modifier = Modifier.rotate(rotationState)
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    assets.forEach { asset ->
                        // Разрешаем удалять только если это не BCS
                        if (asset.broker != BrokerType.BCS) {
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        onDeleteAsset(asset.ticker)
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = { DismissBackground(dismissState) },
                                content = { PortfolioItem(asset, isValueVisible) },
                                enableDismissFromStartToEnd = false
                            )
                        } else {
                            PortfolioItem(asset, isValueVisible)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PortfolioHeader(totalValue: Double, totalProfit: Double, isVisible: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Общий баланс", style = MaterialTheme.typography.labelMedium)
            
            if (isVisible) {
                Text(
                    text = String.format(Locale.US, "%.2f ₽", totalValue),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val profitColor = if (totalProfit >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                val sign = if (totalProfit >= 0) "+" else ""
                Text(
                    text = String.format(Locale.US, "%s%.2f ₽", sign, totalProfit),
                    color = profitColor,
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                Text(
                    text = "****** ₽",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "***",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun PortfolioItem(asset: PortfolioAsset, isVisible: Boolean) {
    val currentPrice = asset.currentPrice ?: asset.averagePrice
    val profit = (currentPrice - asset.averagePrice) * asset.quantity
    val profitColor = if (profit >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)

    ListItem(
        headlineContent = { Text(asset.name, fontWeight = FontWeight.Bold) },
        supportingContent = { 
            if (isVisible) {
                Text("${asset.quantity} шт. x ${String.format(Locale.US, "%.2f", currentPrice)}") 
            } else {
                Text("${asset.quantity} шт.")
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                if (isVisible) {
                    Text(String.format(Locale.US, "%.2f ₽", currentPrice * asset.quantity))
                    Text(
                        text = String.format(Locale.US, "%+.2f", profit),
                        color = profitColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text("******")
                }
            }
        }
    )
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissBackground(dismissState: SwipeToDismissBoxState) {
    val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.onErrorContainer)
    }
}
