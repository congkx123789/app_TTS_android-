#!/bin/bash
# Support relative paths
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Set PYTHONPATH to include Matcha-TTS (expected in sibling or parent dir)
if [ -z "$MATCHA_DIR" ]; then
    MATCHA_DIR="$PROJECT_ROOT/../Model_AI_Text_To_sound/Matcha-TTS"
fi

export PYTHONPATH=$PYTHONPATH:$MATCHA_DIR

# Start server using the python in current environment
python -m uvicorn main:app --host 0.0.0.0 --port 8000
