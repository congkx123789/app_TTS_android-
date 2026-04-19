import os
import subprocess
import sys
from pathlib import Path

def run_command(command, description):
    print(f"--- {description} ---")
    try:
        subprocess.check_call(command, shell=True)
    except subprocess.CalledProcessError as e:
        print(f"Error during {description}: {e}")
        return False
    return True

def setup_environment():
    # Create virtual environment if it doesn't exist
    if not os.path.exists("venv"):
        if not run_command("python3 -m venv venv", "Creating virtual environment"):
            return False
    
    # Install requirements
    pip_path = os.path.join("venv", "bin", "pip") if os.name != "nt" else os.path.join("venv", "Scripts", "pip")
    if not run_command(f"{pip_path} install -r backend/requirements.txt", "Installing backend dependencies"):
        return False
    
    # Install setup-specific dependencies
    if not run_command(f"{pip_path} install huggingface_hub python-dotenv", "Installing setup dependencies"):
        return False
    
    return True

def download_models(token):
    from huggingface_hub import hf_hub_download, snapshot_download
    
    # Define models to download
    # 1. F5-TTS
    f5_dir = Path("Model_API/f5-tts/vietnamese-tts")
    f5_dir.mkdir(parents=True, exist_ok=True)
    print(f"Downloading F5-TTS models to {f5_dir}...")
    snapshot_download(
        repo_id="zalopay/vietnamese-tts",
        local_dir=str(f5_dir),
        token=token,
        ignore_patterns=[".git*", "*.md"]
    )

    # 2. Piper Models (placeholders if not on HF)
    # If the user has these on HF, we can add them here.
    
    print("Model download complete.")

def create_dotenv(token):
    env_content = f"""# Hugging Face Token
HF_TOKEN={token}

# Project Paths
MATCHA_DIR=../Model_AI_Text_To_sound/Matcha-TTS
"""
    with open("backend/.env", "w") as f:
        f.write(env_content)
    print("Created backend/.env file.")

def main():
    print("=== Trudio Project Setup Tool ===")
    
    if len(sys.argv) > 1:
        token = sys.argv[1]
    else:
        token = input("Enter your Hugging Face token: ").strip()
    
    if not token:
        print("Error: Hugging Face token is required.")
        return
        print("Environment setup failed.")
        return

    # Use the python in venv to run the downloader
    python_path = os.path.join("venv", "bin", "python") if os.name != "nt" else os.path.join("venv", "Scripts", "python")
    
    # We'll run the downloader as a separate step to ensure dependencies are loaded
    create_dotenv(token)
    
    # Optional: Download models (User can trigger this)
    # download_models(token)

    print("\nSetup successful!")
    print("To start the server:")
    print("1. source venv/bin/activate")
    print("2. cd backend && bash start_server.sh")

if __name__ == "__main__":
    main()
