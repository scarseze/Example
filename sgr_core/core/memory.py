import sqlite3
import json
import os
from datetime import datetime
from typing import List, Dict, Any, Optional
from core.state import Message

class PersistentMemory:
    def __init__(self, db_path: str = "memory.db"):
        self.db_path = db_path
        self._init_db()

    def _init_db(self):
        """Initialize SQLite tables if they don't exist."""
        conn = sqlite3.connect(self.db_path)
        c = conn.cursor()
        
        # User Table
        c.execute('''
            CREATE TABLE IF NOT EXISTS users (
                user_id TEXT PRIMARY KEY,
                name TEXT,
                preferences_json TEXT,
                created_at TIMESTAMP
            )
        ''')
        
        # History Table
        c.execute('''
            CREATE TABLE IF NOT EXISTS history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT,
                role TEXT,
                content TEXT,
                timestamp TIMESTAMP,
                FOREIGN KEY(user_id) REFERENCES users(user_id)
            )
        ''')
        
        conn.commit()
        conn.close()

    def add_message(self, user_id: str, role: str, content: str):
        """Save a message to history."""
        conn = sqlite3.connect(self.db_path)
        c = conn.cursor()
        c.execute('''
            INSERT INTO history (user_id, role, content, timestamp)
            VALUES (?, ?, ?, ?)
        ''', (user_id, role, content, datetime.now().isoformat()))
        conn.commit()
        conn.close()

    def get_history(self, user_id: str, limit: int = 20) -> List[Message]:
        """Retrieve recent conversation history."""
        conn = sqlite3.connect(self.db_path)
        c = conn.cursor()
        c.execute('''
            SELECT role, content, timestamp 
            FROM history 
            WHERE user_id = ? 
            ORDER BY timestamp DESC 
            LIMIT ?
        ''', (user_id, limit))
        rows = c.fetchall()
        conn.close()
        
        messages = []
        for r in reversed(rows): # Return chronological order
            # Handle string timestamp from SQLite
            ts = r[2]
            if isinstance(ts, str):
                try:
                    ts = datetime.fromisoformat(ts)
                except:
                    ts = datetime.now()
            
            messages.append(Message(role=r[0], content=r[1], timestamp=ts))
            
        return messages

    def get_user_preferences(self, user_id: str) -> Dict[str, Any]:
        """Get user preferences (or create default)."""
        conn = sqlite3.connect(self.db_path)
        c = conn.cursor()
        c.execute('SELECT preferences_json FROM users WHERE user_id = ?', (user_id,))
        row = c.fetchone()
        
        if row and row[0]:
            return json.loads(row[0])
            
        # If user doesn't exist, create simple default
        default_prefs = {"language": "ru"}
        self.update_user_preferences(user_id, default_prefs)
        return default_prefs

    def update_user_preferences(self, user_id: str, prefs: Dict[str, Any]):
        """Upsert user preferences."""
        conn = sqlite3.connect(self.db_path)
        c = conn.cursor()
        
        # Check if exists
        c.execute('SELECT user_id FROM users WHERE user_id = ?', (user_id,))
        exists = c.fetchone()
        
        prefs_str = json.dumps(prefs, ensure_ascii=False)
        
        if exists:
            c.execute('UPDATE users SET preferences_json = ? WHERE user_id = ?', (prefs_str, user_id))
        else:
            c.execute('''
                VALUES (?, ?, ?, ?)
            ''', (user_id, "User", prefs_str, datetime.now().isoformat()))
            
        conn.commit()
        conn.close()
