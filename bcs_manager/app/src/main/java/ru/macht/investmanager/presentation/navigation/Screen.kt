package ru.macht.investmanager.presentation.navigation

sealed class Screen(val route: String) {
    // Главный экран с портфелем (BCS + Manual)
    data object Portfolio : Screen("portfolio_screen")
    
    // Экран новостей
    data object News : Screen("news_screen")
    
    // Экран добавления актива
    data object AddAsset : Screen("add_asset_screen")

    // Экран настройки
    data object Settings : Screen("settings_screen")

    // Экран аналитики
    data object Analytics : Screen("analytics_screen")
}
