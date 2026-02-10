import os
import asyncio
import logging
from aiogram import Bot, Dispatcher, F, types
from aiogram.types import ContentType
from dotenv import load_dotenv

from voice_processor import VoiceProcessor
from intent_parser import IntentParser
from ics_generator import IcsGenerator

# Configuration
load_dotenv()
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

TOKEN = os.getenv("TELEGRAM_BOT_TOKEN")
if not TOKEN:
    raise ValueError("TELEGRAM_BOT_TOKEN is required")

# Initialize modules
bot = Bot(token=TOKEN)
dp = Dispatcher()

voice_proc = VoiceProcessor()
intent_parser = IntentParser()
ics_gen = IcsGenerator()

@dp.message(F.content_type == ContentType.VOICE)
async def handle_voice(message: types.Message):
    user_id = message.from_user.id
    status_msg = await message.reply("👂 Слушаю...")

    try:
        # 1. Download File
        file_id = message.voice.file_id
        file = await bot.get_file(file_id)
        file_path = f"temp_{user_id}_{file_id}.ogg"
        await bot.download_file(file.file_path, file_path)
        
        # 2. Transcribe (Groq)
        await bot.edit_message_text("✍️ Транскрибирую...", chat_id=message.chat.id, message_id=status_msg.message_id)
        text = await voice_proc.transcribe(file_path)
        
        # Cleanup file
        if os.path.exists(file_path):
            os.remove(file_path)

        if not text:
            await bot.edit_message_text("❌ Не удалось разобрать речь.", chat_id=message.chat.id, message_id=status_msg.message_id)
            return

        # 3. Analyze Intent (DeepSeek)
        await bot.edit_message_text(f"🧠 Думаю: \"{text}\"", chat_id=message.chat.id, message_id=status_msg.message_id)
        result = await intent_parser.extract_intent(text)
        
        intent = result.get("intent")
        
        if intent == "create_event":
            # 4. Execute Action (Generate ICS)
            summary = result.get("summary", "New Event")
            start = result.get("start_time")
            end = result.get("end_time")
            desc = result.get("description", text)
            
            if start and end:
                ics_path = ics_gen.create_ics(summary, start, end, desc)
                if ics_path:
                    # Send ICS file
                    caption = f"📅 **Событие:** {summary}\n🕒 **Начало:** {start}"
                    file_input = types.FSInputFile(ics_path)
                    await bot.send_document(message.chat.id, file_input, caption=caption, parse_mode="Markdown")
                    
                    # Cleanup ICS
                    if os.path.exists(ics_path):
                        os.remove(ics_path)
                    
                    # Delete status message to keep chat clean
                    await bot.delete_message(chat_id=message.chat.id, message_id=status_msg.message_id)
                else:
                    await bot.edit_message_text("❌ Ошибка генерации файла календаря.", chat_id=message.chat.id, message_id=status_msg.message_id)
            else:
                response = f"⚠️ Не удалось определить точное время.\nРаспознано: {text}"
                await bot.edit_message_text(response, chat_id=message.chat.id, message_id=status_msg.message_id)
                
        elif intent == "note":
            response = f"📝 **Заметка (пока не сохраняю):**\n{result.get('content')}"
            await bot.edit_message_text(response, chat_id=message.chat.id, message_id=status_msg.message_id, parse_mode="Markdown")
            
        else:
            response = f"🤷‍♂️ **Не понял команду:**\n{text}\n\nПопробуйте: \"Запиши встречу завтра в 5\""
            await bot.edit_message_text(response, chat_id=message.chat.id, message_id=status_msg.message_id, parse_mode="Markdown")

    except Exception as e:
        logger.error(f"Error handling voice: {e}")
        await bot.edit_message_text("💥 Произошла системная ошибка.", chat_id=message.chat.id, message_id=status_msg.message_id)

@dp.message(F.text)
async def handle_text(message: types.Message):
    # Same logic for text messages, skipping step 1 & 2
    status_msg = await message.reply("🧠 Анализирую текст...")
    text = message.text
    
    intent = await intent_parser.extract_intent(text)
    # ... (Logic duplicated implicitly or refactor. For MVP voice is main priority)
    # Let's just dump the raw intent for debug or proper handling
    await bot.edit_message_text(f"Debug Raw Intent: {intent}", chat_id=message.chat.id, message_id=status_msg.message_id)


async def main():
    logger.info("Bot started!")
    await dp.start_polling(bot)

if __name__ == "__main__":
    asyncio.run(main())
