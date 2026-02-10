# AGENTS.md - Context & Guidelines for AI Assistants

##  Project Overview
**Name**: InvestManager via BCS & MOEX
**Package**: `ru.macht.investmanager`
**Language**: Kotlin
**UI**: Jetpack Compose (Material3)

## Architecture
The project follows **Clean Architecture** principles with **Dependency Injection**.

### Layers
1.  **Domain** (`domain/`): Pure Kotlin. Business logic.
    *   `model/`: Data classes (e.g., `PortfolioAsset`).
    *   `repository/`: Interfaces (e.g., `BcsRepository`).
    *   `usecase/`: Single-action logic (e.g., `GetPortfolioNewsUseCase`).
2.  **Data** (`data/`): Implementation details.
    *   `repository/`: Implementations of domain repos (e.g., `BcsRepositoryImpl`).
    *   `source/`: API services (`api/`), Database (`db/`), Preferences (`SettingsManager`).
3.  **Presentation** (`presentation/`): UI & State.
    *   Organized by feature: `news/`, `portfolio/`, `settings/`.
    *   **Pattern**: MVI/MVVM. ViewModels expose `StateFlow<UiState>`.

## Tech Stack
*   **DI**: Hilt (`@HiltAndroidApp`, `@HiltViewModel`, `@Inject`).
*   **Network**: Retrofit + OkHttp.
    *   `AuthInterceptor`: Handles JWT refresh & `401` errors.
    *   `BcsApiService`: Uses `@SerializedName(alternate=...)` for robust JSON parsing.
*   **Async**: Coroutines & Flow (`viewModelScope`, `runBlocking` only in specialized interceptors).
*   **Local Storage**:
    *   **Room**: Caching (if implemented).
    *   **DataStore**: `SettingsManager` for API keys & preferences.

## Coding Guidelines
1.  **Language**: Prefer **Russian** for UI text and Code Comments.
2.  **Naming**:
    *   Classes/Files: PascalCase (e.g., `PortfolioScreen`).
    *   Functions/Vars: camelCase.
3.  **Safety**:
    *   Never hardcode API keys. Use `SettingsManager` or `BuildConfig`.
    *   Handle network errors gracefully ( `Result`/`Resource` wrappers or `try-catch` in UseCases).
4.  **UI**:
    *   Use `MaterialTheme`.
    *   Extract reusable components to `presentation/common/`.


