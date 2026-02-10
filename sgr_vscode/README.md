# SGR Agent Client (VS Code Extension)

**Connect your IDE to your autonomous SGR Agent.**

![VS Code](https://img.shields.io/badge/IDE-VS%20Code-blue) ![TypeScript](https://img.shields.io/badge/Language-TypeScript-blue) ![Localhost](https://img.shields.io/badge/Connection-Localhost-green)

## Overview

This extension integrates the **Schema-Guided Reasoning (SGR)** agent directly into your coding workflow. It acts as a "Reverse MCP" (Model Context Protocol) client, capturing your current editor context (selected code, cursor position, diagnostics) and sending it to your local SGR Core server for analysis.

## Features

*   **Context-Aware Chat**: Select code and ask questions. The agent sees what you see.
*   **One-Click Ask**: Shortcut `Ctrl+Shift+A` (or `Cmd+Shift+A` on Mac) to open the chat sidebar.
*   **Privacy First**: No cloud. All data is sent to your local instance (`http://localhost:8000`).

## Configuration

| Setting | Default | Description |
| :--- | :--- | :--- |
| `sgr.serverUrl` | `http://localhost:8000/v1/agent/process` | The endpoint of your running SGR Core server. |

## Installation

1.  **Build from source**:
    ```bash
    npm install
    npm run compile
    ```
2.  **Debug**: Press `F5` to launch a new Extension Development Host window.
3.  **Install VSIX**:
    ```bash
    code --install-extension sgr-agent-client-0.1.0.vsix
    ```

