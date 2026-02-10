package ru.macht.investmanager.presentation.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.macht.investmanager.domain.model.BondAnalysis
import ru.macht.investmanager.domain.model.YieldPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBackClick: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аналитика ОФЗ") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is AnalyticsUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is AnalyticsUiState.Error -> Text(
                    text = "Ошибка: ${state.message}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                is AnalyticsUiState.Success -> AnalyticsContent(state)
            }
        }
    }
}

@Composable
fun AnalyticsContent(state: AnalyticsUiState.Success) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Кривая бескупонной доходности (ZCYC)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Дата: ${state.yieldCurve.date}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Chart Area
        Box(modifier = Modifier.height(300.dp).fillMaxWidth().padding(vertical = 16.dp)) {
            YieldChart(points = state.yieldCurve.points)
        }

        Text(
            text = "Анализ портфеля",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )

        if (state.analyzedBonds.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.analyzedBonds) { bond ->
                    BondAnalysisItem(bond)
                }
            }
        } else {
            Text(
                text = "Нет облигаций ОФЗ в портфеле для анализа.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun BondAnalysisItem(bond: BondAnalysis) {
    val potentialColor = if (bond.upsidePercent > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = bond.ticker, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "Погашение: ${bond.maturityDate} (${String.format("%.1f", bond.yearsToMaturity)} г.)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Fair: ${bond.fairPrice.toInt()} ₽",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Потенциал: ${String.format("%+.1f%%", bond.upsidePercent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = potentialColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun YieldChart(
    points: List<YieldPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    if (points.isEmpty()) return

    val maxYear = points.maxOf { it.maturityYears }
    val maxYield = points.maxOf { it.yieldPercent }
    val minYield = points.minOf { it.yieldPercent }
    
    val yMin = (minYield - 0.5).coerceAtLeast(0.0)
    val yMax = maxYield + 0.5
    val xMax = maxYear + 1.0

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val padding = 40.dp.toPx()
        
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        fun getX(year: Double): Float =
            (padding + (year / xMax) * chartWidth).toFloat()

        fun getY(yield: Double): Float =
            (height - padding - ((yield - yMin) / (yMax - yMin)) * chartHeight).toFloat()

        // Draw Grid
        val ySteps = 5
        for (i in 0..ySteps) {
            val yVal = yMin + (yMax - yMin) * i / ySteps
            val yPos = getY(yVal)
            
            drawLine(
                color = gridColor,
                start = Offset(padding, yPos),
                end = Offset(width - padding, yPos),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    String.format("%.1f%%", yVal),
                    padding - 10f,
                    yPos + 5f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
            }
        }

        // Draw Line
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = getX(point.maturityYears)
            val y = getY(point.yieldPercent)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path = path, color = lineColor, style = Stroke(width = 5.dp.toPx()))
    }
}
