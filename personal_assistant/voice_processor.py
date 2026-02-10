import os
import logging
from groq import AsyncGroq
from dotenv import load_dotenv

# Load env variables
load_dotenv()

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class VoiceProcessor:
    def __init__(self):
        api_key = os.getenv("GROQ_API_KEY")
        if not api_key:
            logger.error("GROQ_API_KEY is missing in .env")
            raise ValueError("GROQ_API_KEY is required")
        
        self.client = AsyncGroq(api_key=api_key)
        self.model = "whisper-large-v3-turbo"  # Or "distil-whisper-large-v3-en"
        logger.info(f"VoiceProcessor initialized with model: {self.model}")

    async def transcribe(self, file_path: str) -> str:
        """
        Transcribes an audio file using Groq's Whisper API.
        """
        if not os.path.exists(file_path):
            logger.error(f"File not found: {file_path}")
            return ""

        try:
            logger.info(f"Sending {file_path} to Groq for transcription...")
            
            with open(file_path, "rb") as file:
                transcription = await self.client.audio.transcriptions.create(
                    file=(os.path.basename(file_path), file.read()),
                    model=self.model,
                    # Optional: prompt="Please transcribe this voice message in Russian.",
                    temperature=0.0
                )
            
            text = transcription.text
            logger.info(f"Transcription successful: {text[:50]}...")
            return text

        except Exception as e:
            logger.error(f"Error during transcription: {e}")
            return ""

# Test execution
if __name__ == "__main__":
    import asyncio
    # Create a dummy or record a file to test locally
    async def main():
        try:
            vp = VoiceProcessor()
            # print(await vp.transcribe("test_voice.ogg"))
        except Exception as e:
            print(f"Init failed: {e}")
    
    asyncio.run(main())
