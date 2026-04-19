import os
from pathlib import Path

# Get project root (parent of backend folder)
ROOT_DIR = Path(__file__).resolve().parent.parent

# Define base paths - Supporting relative paths for portability
# Matcha-TTS is usually a sibling to the current project or inside a Model_AI folder
# We check a few common locations
MATCHA_DIR_DEFAULT = ROOT_DIR.parent / "Model_AI_Text_To_sound" / "Matcha-TTS"
MATCHA_DIR = os.getenv("MATCHA_DIR", str(MATCHA_DIR_DEFAULT))

if not os.path.exists(MATCHA_DIR):
    # Fallback to current sibling if not found in Model_AI
    alt_matcha = ROOT_DIR.parent / "Matcha-TTS"
    if alt_matcha.exists():
        MATCHA_DIR = str(alt_matcha)

# Checkpoints
VN_CHECKPOINT = os.path.join(MATCHA_DIR, "checkpoints_vn", "matcha_vn_ngngngan.pt")
EN_CHECKPOINT = os.path.join(MATCHA_DIR, "matcha_ljspeech.ckpt")

# Audio properties
SAMPLE_RATE = 22050

# Database
DATABASE_URL = "sqlite:///./app.db"
