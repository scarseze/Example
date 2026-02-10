from ics import Calendar, Event
import logging
from datetime import datetime, timedelta
import os

logger = logging.getLogger(__name__)

class IcsGenerator:
    def __init__(self):
        pass

    def create_ics(self, summary: str, start_time: str, end_time: str, description: str = "") -> str:
        """
        Creates an .ics file for the event.
        Times must be 'YYYY-MM-DD HH:MM:SS'.
        Returns the absolute path to the generated file.
        """
        try:
            # Parse strings to datetime
            dt_start = datetime.strptime(start_time, "%Y-%m-%d %H:%M:%S")
            dt_end = datetime.strptime(end_time, "%Y-%m-%d %H:%M:%S")

            c = Calendar()
            e = Event()
            e.name = summary
            e.begin = dt_start
            e.end = dt_end
            e.description = description
            
            c.events.add(e)

            # Create filename based on summary (sanitized)
            safe_summary = "".join([c for c in summary if c.isalnum() or c in (' ', '-', '_')]).strip()
            filename = f"{safe_summary}.ics"
            
            # Save to current directory
            with open(filename, 'w', encoding='utf-8') as f:
                f.writelines(c.serialize_iter())
            
            abs_path = os.path.abspath(filename)
            logger.info(f"ICS file created at: {abs_path}")
            return abs_path

        except Exception as e:
            logger.error(f"Failed to create ICS: {e}")
            return None
