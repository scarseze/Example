# SGR Agent Client (VS Code Extension)

**Подключите вашу IDE к автономному SGR Агенту.**

![VS Code](https://img.shields.io/badge/IDE-VS%20Code-blue) ![TypeScript](https://img.shields.io/badge/Language-TypeScript-blue) ![Localhost](https://img.shields.io/badge/Connection-Localhost-green)

## Обзор

Это расширение интегрирует агента **Schema-Guided Reasoning (SGR)** напрямую в ваш рабочий процесс. Оно работает как клиент "Reverse MCP" (Model Context Protocol), захватывая контекст редактора (выделенный код, позицию курсора, ошибки компиляции) и отправляя его на ваш локальный сервер SGR Core для анализа.

## Возможности

*   **Контекстный Чат**: Выделите код и задайте вопрос. Агент видит то же, что и вы.
*   **Быстрый доступ**: Горячая клавиша `Ctrl+Shift+A` (или `Cmd+Shift+A` на Mac) открывает боковую панель.
*   **Приватность**: Никакого облака. Все данные отправляются на ваш локальный инстанс (`http://localhost:8000`).

## Настройка

| Настройка | По умолчанию | Описание |
| :--- | :--- | :--- |
| `sgr.serverUrl` | `http://localhost:8000/v1/agent/process` | Эндпоинт вашего запущенного сервера SGR Core. |

## Установка

1.  **Сборка из исходников**:
    ```bash
    npm install
    npm run compile
    ```
2.  **Отладка**: Нажмите `F5`, чтобы запустить окно разработки расширения.
3.  **Установка VSIX**:
    ```bash
    code --install-extension sgr-agent-client-0.1.0.vsix
    ```

