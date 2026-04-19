#!/bin/bash
export PYTHONPATH=$PYTHONPATH:/home/alida/Documents/Cursor/Model_AI_Text_To_sound/Matcha-TTS
# Use the matcha conda environment explicitly to ensure torch is available
/home/alida/miniconda3/envs/matcha/bin/python -m uvicorn main:app --host 0.0.0.0 --port 8000
