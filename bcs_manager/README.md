# BCS Manager (InvestManager)

**Android Application for Investment Portfolio Management.**

![Android](https://img.shields.io/badge/Platform-Android-green) ![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple) ![Architecture](https://img.shields.io/badge/Arch-MVVM-blue)

## Overview

Modern Android application for tracking investment assets, viewing relevant news via AI analysis, and monitoring market metrics.

**New in v2.0 (Optimization Update):**
*   **Async Core**: Rewrote `NewsRepository` to use **Kotlin Coroutines**. The app no longer freezes during network calls.
*   **Stability**: Fixed Critical 401 Auth errors and handled Malformed JSON responses from AI services.
*   **Security**: Implemented safe logging for API keys (scrubbing sensitive data from logs).

## Tech Stack

*   **Language**: Kotlin
*   **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture
*   **Network**: Retrofit + OkHttp
*   **Concurrency**: Coroutines & Flow
*   **Data**: Room Database (Local Cache)

## Key Features

1.  **Portfolio Tracking**: Real-time asset valuation.
2.  **Smart News**: Filters news stream based on your specific tickers using Regex matching.
3.  **AI Integration**: Displays AI-generated sentiment and summaries for news items (Backend integration).


