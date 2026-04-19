import os
import sys

# Define base paths
MATCHA_DIR = "/home/alida/Documents/Cursor/Model_AI_Text_To_sound/Matcha-TTS"

# Checkpoints
VN_CHECKPOINT = os.path.join(MATCHA_DIR, "checkpoints_vn", "matcha_vn_ngngngan.pt")
# For English, we'll try to use a VCTK or LJSpeech checkpoint if available, or fallback to something else.
# Alternatively, point to the last checkpoint of your training for VN if preferred.
EN_CHECKPOINT = os.path.join(MATCHA_DIR, "matcha_ljspeech.ckpt") # Will be downloaded automatically by matcha if not exist, or we can use another one

# Audio properties
SAMPLE_RATE = 22050

# Database
DATABASE_URL = "sqlite:///./app.db"
