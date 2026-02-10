package ru.macht.investmanager.presentation.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.macht.investmanager.domain.model.NewsArticle
import ru.macht.investmanager.domain.model.Sentiment
import ru.macht.investmanager.presentation.common.WarningBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    viewModel: NewsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новости портфеля") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        val isRefreshing = uiState is NewsUiState.Loading
        val pullRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.loadNews() },
            state = pullRefreshState,
            modifier = Modifier.padding(padding)
        ) {
            when (val state = uiState) {
                is NewsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize())
                }
                is NewsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Ошибка: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is NewsUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (state.errors.isNotEmpty()) {
                            state.errors.forEach { error ->
                                WarningBanner(message = error)
                            }
                        }
                        
                        if (state.news.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Нет новостей для вашего портфеля")
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.news) { article ->
                                    NewsItemCard(article)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsItemCard(article: NewsArticle) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = article.pubDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = article.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            if (article.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "AI Summary: ${article.summary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            val sentimentColor = when (article.sentiment) {
                Sentiment.POSITIVE -> Color(0xFF4CAF50)
                Sentiment.NEGATIVE -> Color(0xFFE53935)
                Sentiment.NEUTRAL -> Color.Gray
            }
            Text(
                text = "Sentiment: ${article.sentiment}",
                style = MaterialTheme.typography.labelMedium,
                color = sentimentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}