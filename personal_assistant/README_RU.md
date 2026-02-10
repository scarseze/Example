# Personal Assistant Reference (Legacy)

**Прототип голосового интерфейса для экосистемы SGR.**

> **ВАЖНО**: Основной функционал (обработка голоса через Groq, календарь) был **перенесен в [SGR Core](../sgr_core)**. Этот репозиторий оставлен как эталонный пример (Reference Implementation) легкого голосового бота.

![Python](https://img.shields.io/badge/Python-3.11-blue) ![Groq](https://img.shields.io/badge/AI-Groq-orange) ![Status](https://img.shields.io/badge/Status-Merged-yellow)

## Обзор

Легковесный Telegram-бот, созданный для демонстрации **сверхбыстрого голосового взаимодействия**.
Использует **Groq API** (Whisper) для мгновенной транскрибации и **DeepSeek** для классификации намерений.

**Ключевые функции:**
*   **Speech-to-Text**: На базе Groq `whisper-large-v3`.
*   **Распознавание интентов**: Различает "Создать событие", "Заметка", "Простой чат".
*   **Календарь**: Генерирует файлы `.ics` для добавления встреч.

## Стек

*   `aiogram` (Telegram Bot API)
*   `groq` (Python SDK)
*   `ics` (Библиотека генерации календаря)

## Как запустить (Отдельно)

Если вы хотите запустить эту облегченную версию вместо полного ядра SGR Core:

1.  Настройте `.env`:
    ```
    TELEGRAM_BOT_TOKEN=...
    GROQ_API_KEY=...
    DEEPSEEK_API_KEY=...
    ```
2.  Запустите:
    ```bash
    python bot.py
    ```
