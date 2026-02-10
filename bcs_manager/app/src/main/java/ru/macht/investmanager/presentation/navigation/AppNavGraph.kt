package ru.macht.investmanager.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.macht.investmanager.presentation.portfolio.PortfolioScreen
import ru.macht.investmanager.presentation.news.NewsScreen
import ru.macht.investmanager.presentation.addasset.AddAssetScreen
import ru.macht.investmanager.presentation.settings.SettingsScreen
import ru.macht.investmanager.presentation.analytics.AnalyticsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Portfolio.route,
        modifier = modifier
    ) {
        // Маршрут Портфеля
        composable(route = Screen.Portfolio.route) {
            PortfolioScreen(
                onNavigateToNews = { navController.navigate(Screen.News.route) },
                onNavigateToAddAsset = { navController.navigate(Screen.AddAsset.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) }
            )
        }

        // Маршрут Новостей
        composable(route = Screen.News.route) {
            NewsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Маршрут Добавления Актива
        composable(route = Screen.AddAsset.route) {
            AddAssetScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Маршрут Настроек
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Маршрут Аналитики
        composable(route = Screen.Analytics.route) {
            AnalyticsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
