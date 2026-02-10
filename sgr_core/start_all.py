import subprocess
import sys
import os
import time
import signal

def main():
    print("🚀 Starting SGR Unified System...")
    print("1. API Server (for VS Code)")
    print("2. Telegram Bot")
    
    # Paths
    current_dir = os.path.dirname(os.path.abspath(__file__))
    server_cmd = [sys.executable, "main.py", "--server"]
    telegram_cmd = [sys.executable, "interfaces/telegram_bot.py"]
    
    # Environment
    env = os.environ.copy()
    env["PYTHONUNBUFFERED"] = "1"
    # Ensure localhost is not proxied
    # Ensure localhost is not proxied
    # env["NO_PROXY"] = "localhost,127.0.0.1"

    processes = []

    try:
        # Start API Server
        print("\n[1/2] Launching API Server...")
        server_proc = subprocess.Popen(server_cmd, cwd=current_dir, env=env)
        processes.append(server_proc)
        
        # Start Telegram Bot
        print("[2/2] Launching Telegram Bot...")
        tg_proc = subprocess.Popen(telegram_cmd, cwd=current_dir, env=env)
        processes.append(tg_proc)

        # Optional: Max Messenger (Uncomment to use)
        # print("[OPTIONAL] Launching Max Messenger...")
        # max_cmd = [sys.executable, "core/interfaces/max_bot.py"] # or main.py --max
        # max_proc = subprocess.Popen(max_cmd, cwd=current_dir, env=env)
        # processes.append(max_proc)

        # Optional: Local Ollama (Uncomment to use)
        # print("[OPTIONAL] Launching Ollama...")
        # ollama_cmd = ["ollama", "serve"] 
        # ollama_proc = subprocess.Popen(ollama_cmd, cwd=current_dir, env=env)
        # processes.append(ollama_proc)
        
        print("\n✅ All systems go! Press Ctrl+C to stop everything.\n")
        
        # Monitor Loop
        while True:
            time.sleep(1)
            # Check if any process died
            if server_proc.poll() is not None:
                print(f"❌ API Server died with code {server_proc.returncode}")
                break
            if tg_proc.poll() is not None:
                print(f"❌ Telegram Bot died with code {tg_proc.returncode}")
                break
                
    except KeyboardInterrupt:
        print("\n🛑 Stopping all services...")
    finally:
        for p in processes:
            if p.poll() is None:
                p.terminate()
                try:
                    p.wait(timeout=5)
                except:
                    p.kill()
        print("Done.")

if __name__ == "__main__":
    main()
