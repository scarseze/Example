package ru.macht.investmanager.data

object TickerNames {
    fun getName(secId: String, instrumentType: String): String {
        // Простой маппинг или заглушка. В будущем можно расширить.
        return when(secId) {
            "SBER" -> "Сбербанк"
            "GAZP" -> "Газпром"
            "LKOH" -> "Лукойл"
            "VTBR" -> "ВТБ"
            "YNDX" -> "Яндекс"
            "TCSG" -> "Т-Банк"
            "ROSN" -> "Роснефть"
            "NVTK" -> "Новатэк"
            "GMKN" -> "Норникель"
            "SNGS" -> "Сургутнефтегаз"
            "SNGSP" -> "Сургутнефтегаз-п"
            "TATN" -> "Татнефть"
            "PLZL" -> "Полюс"
            "MGNT" -> "Магнит"
            "NLMK" -> "НЛМК"
            "CHMF" -> "Северсталь"
            "ALRS" -> "Алроса"
            "MTSS" -> "МТС"
            else -> secId // Возвращаем тикер, если имя не найдено
        }
    }
}
