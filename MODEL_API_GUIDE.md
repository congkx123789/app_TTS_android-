# Vietnamese TTS Model API Guide 🇻🇳

This guide explains how to use the newly added Vietnamese TTS models in this project. We have integrated several high-quality, open-source models optimized for different use cases.

## 📁 Directory Structure

All models are stored in the `Model_API/` directory:
- `Model_API/piper/`: Optimized ONNX models (vais1000-medium and vivos-x_low).
- `Model_API/vieneu/`: Modern GGUF models for on-device execution.
- `Model_API/f5-tts/`: State-of-the-art expressive models (ZaloPay fine-tune).

## 🚀 Distributed APIs (New)

We now provide independent API servers for each model as requested.

### 1. Piper TTS API
- **Location**: `Model_API/piper/api_piper.py`
- **Port**: `8001`
- **Run**: `python api_piper.py`
- **Endpoint**: `POST /synthesize`
- **Request**: `{"text": "...", "model_name": "medium", "speed": 1.0}`

### 2. F5-TTS API
- **Location**: `Model_API/f5-tts/api_f5.py`
- **Port**: `8002`
- **Run**: `python api_f5.py`
- **Endpoint**: `POST /synthesize`
- **Request**: `{"text": "..."}`

### 3. VieNeu-TTS API
- **Location**: `Model_API/vieneu/api_vieneu.py`
- **Port**: `8003`
- **Run**: `python api_vieneu.py`
- **Endpoint**: `POST /synthesize`
- **Request**: `{"text": "..."}`

### 4. Matcha-TTS API
- **Location**: `Model_AI_Text_To_sound/Matcha-TTS/api_matcha.py`
- **Port**: `8004`
- **Run**: `python api_matcha.py`
- **Endpoint**: `POST /synthesize`
- **Request**: `{"text": "...", "language": "vn"}`

## 🏗️ Running the System

1. **Activate Environment**: Ensure you are in the `miniconda` environment.
2. **Start Desired API**: Navigate to the model folder and run its `api_*.py` script.
3. **Unified Backend**: The main `backend/main.py` (Port 8000) still provides a unified interface if needed, but these separate APIs allow for independent scaling and usage.
