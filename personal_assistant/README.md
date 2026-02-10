# Personal Assistant Reference (Legacy)

**This project served as the prototype for the Voice Interface of the SGR Ecosystem.**

> **NOTE**: The core functionality (Voice processing via Groq, Calendar skills) has been **merged into [SGR Core](../sgr_core)**. This repository is kept for reference as a clean, lightweight example of a Voice Bot.

![Python](https://img.shields.io/badge/Python-3.11-blue) ![Groq](https://img.shields.io/badge/AI-Groq-orange) ![Status](https://img.shields.io/badge/Status-Merged-yellow)

## Overview

A lightweight Telegram bot designed to demonstrate **extremely fast voice interactions**.
It uses **Groq API** (Whisper) for near-instant transcription and **DeepSeek** for intent classification.

**Key Features:**
*   **Speech-to-Text**: Powered by Groq `whisper-large-v3`.
*   **Intent Parsing**: Distinguishes between "Create Event", "Take Note", and "General Chat".
*   **Calendar**: Generates `.ics` files for scheduling.

## Stack

*   `aiogram` (Telegram Bot API)
*   `groq` (Python SDK)
*   `ics` (Python Library)

## How to Run (Standalone)

If you wish to run this lightweight version instead of the full SGR Core:

1.  Set `.env`:
    ```
    TELEGRAM_BOT_TOKEN=...
    GROQ_API_KEY=...
    DEEPSEEK_API_KEY=...
    ```
2.  Run:
    ```bash
    python bot.py
    ```
