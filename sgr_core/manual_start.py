import sys
import subprocess
import os

print("🚀 Starting SGR Core Server MANUALLY...")
print("If this window closes instantly, run this script from PowerShell/CMD directly.")

# Force unbuffered output
env = os.environ.copy()
env["PYTHONUNBUFFERED"] = "1"

# Run main.py --server directly
cmd = [sys.executable, "main.py", "--server"]
print(f"Executing: {' '.join(cmd)}")

try:
    # Use current script directory as CWD
    current_dir = os.path.dirname(os.path.abspath(__file__))
    subprocess.run(cmd, cwd=current_dir, env=env, check=True)
except subprocess.CalledProcessError as e:
    print(f"\n❌ Server crashed with exit code {e.returncode}")
except KeyboardInterrupt:
    print("\n🛑 Stopped by user")
except Exception as e:
    print(f"\n❌ Error: {e}")

input("\nPress Enter to close window...")
