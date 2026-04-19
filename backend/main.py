import sys
import os
import torch
import soundfile as sf
import tempfile
from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional

from config import MATCHA_DIR, VN_CHECKPOINT, EN_CHECKPOINT, SAMPLE_RATE, DATABASE_URL
from database import engine, Base
from routers import books, posts, groups, users
from seed import seed_db
import models
from piper_handler import PiperHandler, VieNeuHandler, F5TTSHandler, PIPER_MODELS

# Add Matcha-TTS to path so we can import its modules
sys.path.append(MATCHA_DIR)

try:
    from matcha.models.matcha_tts import MatchaTTS
    from matcha.hifigan.models import Generator as HiFiGAN
    from matcha.hifigan.config import v1
    from matcha.hifigan.env import AttrDict
    from matcha.hifigan.denoiser import Denoiser
    from matcha.text import text_to_sequence
    from matcha.utils.utils import intersperse, get_user_data_dir, assert_model_downloaded
    HAS_MATCHA = True
except ImportError as e:
    print(f"Error importing matcha modules: {e}")
    print("Matcha-TTS will be unavailable. Use other models (Piper, VieNeu, F5).")
    HAS_MATCHA = False

app = FastAPI(title="Matcha-TTS Text-to-Sound API")

# Add CORS middleware to allow the HTML file to make requests
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include Routers
app.include_router(books.router)
app.include_router(posts.router)
app.include_router(groups.router)
app.include_router(users.router)

# Global variables to hold models
models = {
    "vn": None,
    "en": None
}
# New handlers
piper_handlers = {"medium": None, "x_low": None}
vieneu_handler = None
f5_handler = None

vocoder = None
denoiser = None
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

def load_matcha(checkpoint_path, device):
    print(f"Loading Matcha from {checkpoint_path} to {device}", flush=True)
    if not os.path.exists(checkpoint_path):
        # Allow matcha cli to handle download if it's a standard one, but for custom like VN:
        print(f"Warning: Checkpoint not found at {checkpoint_path}")
        return None
        
    try:
        checkpoint = torch.load(checkpoint_path, map_location=device, weights_only=False)
        model = MatchaTTS(**checkpoint['hyper_parameters'])
        model.load_state_dict(checkpoint['state_dict'])
        model.eval()
        model.to(device)
        return model
    except Exception as e:
        print(f"Failed to load model from {checkpoint_path}: {e}")
        return None

def load_vocoder(device):
    print("Loading Vocoder...", flush=True)
    save_dir = get_user_data_dir()
    vocoder_name = "hifigan_T2_v1" # Or hifigan_univ_v1 based on your training
    url = "https://github.com/shivammehta25/Matcha-TTS-checkpoints/releases/download/v1.0/generator_v1"
    vocoder_path = save_dir / vocoder_name
    assert_model_downloaded(vocoder_path, url)
    
    h = AttrDict(v1)
    hifigan = HiFiGAN(h).to(device)
    try:
        # Some hifigan models have 'generator' key, some just direct state dict
        checkpoint = torch.load(vocoder_path, map_location=device, weights_only=False)
        if "generator" in checkpoint:
            hifigan.load_state_dict(checkpoint["generator"])
        else:
            hifigan.load_state_dict(checkpoint)
    except Exception as e:
        print(f"Vocoder load error: {e}")
        
    hifigan.eval()
    hifigan.remove_weight_norm()
    d = Denoiser(hifigan, mode="zeros")
    return hifigan, d

@app.on_event("startup")
async def startup_event():
    global models, vocoder, denoiser, vieneu_handler, f5_handler
    print(f"Starting API on device: {device}")
    
    # Initialize Database Tables
    Base.metadata.create_all(bind=engine)
    
    # Auto-seed if empty
    print("Checking if database needs seeding...")
    seed_db()
    
    # Load Vietnamese model if Matcha is available
    if HAS_MATCHA:
        models["vn"] = load_matcha(VN_CHECKPOINT, device)
    
    # Optional: Load English model if needed, otherwise skip to save memory
    # models["en"] = load_matcha(EN_CHECKPOINT, device)
    
    # Load Vocoder if Matcha is available
    if HAS_MATCHA:
        vocoder, denoiser = load_vocoder(device)
    else:
        print("Skipping Matcha Vocoder load.")
    
    # Initialize New Handlers
    print("Initializing New Model Handlers...")
    piper_handlers["medium"] = PiperHandler(PIPER_MODELS["medium"]["model"], PIPER_MODELS["medium"]["config"])
    piper_handlers["x_low"] = PiperHandler(PIPER_MODELS["x_low"]["model"], PIPER_MODELS["x_low"]["config"])
    
    vieneu_handler = VieNeuHandler("Model_API/vieneu/VieNeu-TTS-0_3B-Q4_0.gguf")
    # we don't load yet as llama-cpp-python might be missing
    
    f5_handler = F5TTSHandler(
        "Model_API/f5-tts/vietnamese-tts/model_1290000.pt",
        "Model_API/f5-tts/vietnamese-tts/vocab.txt"
    )

class SynthesisRequest(BaseModel):
    text: str
    language: str = "vn" # 'vn' or 'en'
    speed: Optional[float] = 1.0
    model_type: Optional[str] = "matcha" # 'matcha', 'piper', 'vieneu', 'f5'
    model_name: Optional[str] = "default" # 'medium', 'x_low', 'zalopay'

def process_text(text, device, language):
    cleaner = "basic_cleaners_ngngngan" if language == "vn" else "english_cleaners2"
    
    try:
        seq = text_to_sequence(text, [cleaner])[0]
        x = torch.tensor(intersperse(seq, 0), dtype=torch.long, device=device)[None]
        x_lengths = torch.tensor([x.shape[-1]], dtype=torch.long, device=device)
        return x, x_lengths
    except Exception as e:
        print(f"Error processing text: {e}")
        raise e

@app.get("/health")
def health_check():
    return {
        "status": "online",
        "models_loaded": {
            "vn": models["vn"] is not None,
            "en": models["en"] is not None
        },
        "device": str(device)
    }

@app.post("/api/v1/synthesize")
@torch.inference_mode()
def synthesize(request: SynthesisRequest, background_tasks: BackgroundTasks):
    lang = request.language.lower()
    model_type = request.model_type.lower()
    
    if model_type == "matcha":
        return synthesize_matcha(request, background_tasks)
    elif model_type == "piper":
        return synthesize_piper(request, background_tasks)
    elif model_type == "vieneu":
        return synthesize_vieneu(request, background_tasks)
    elif model_type == "f5":
        return synthesize_f5(request, background_tasks)
    else:
        raise HTTPException(status_code=400, detail=f"Unsupported model type: {model_type}")

def synthesize_piper(request: SynthesisRequest, background_tasks: BackgroundTasks):
    name = request.model_name if request.model_name in piper_handlers else "medium"
    handler = piper_handlers[name]
    
    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".wav")
    temp_filename = temp_file.name
    temp_file.close()
    
    if handler is None:
        raise HTTPException(status_code=503, detail=f"Piper model '{name}' is not initialized or available.")
        
    success = handler.synthesize(request.text, temp_filename, request.speed)
    if not success:
        raise HTTPException(status_code=500, detail="Piper synthesis failed (check server logs)")
    
    background_tasks.add_task(os.remove, temp_filename)
    return FileResponse(path=temp_filename, media_type="audio/wav", filename="piper_output.wav")

def synthesize_vieneu(request: SynthesisRequest, background_tasks: BackgroundTasks):
    if vieneu_handler is None:
        raise HTTPException(status_code=503, detail="VieNeu model handler is not initialized.")
        
    if vieneu_handler.llm is None:
        if not vieneu_handler.load():
            raise HTTPException(status_code=503, detail="VieNeu handler (llama-cpp-python) could not be loaded")
            
    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".wav")
    temp_filename = temp_file.name
    temp_file.close()
    
    success = vieneu_handler.synthesize(request.text, temp_filename)
    if not success:
        raise HTTPException(status_code=500, detail="VieNeu synthesis failed")
        
    background_tasks.add_task(os.remove, temp_filename)
    return FileResponse(path=temp_filename, media_type="audio/wav", filename="vieneu_output.wav")

def synthesize_f5(request: SynthesisRequest, background_tasks: BackgroundTasks):
    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".wav")
    temp_filename = temp_file.name
    temp_file.close()
    
    if f5_handler is None:
        raise HTTPException(status_code=503, detail="F5-TTS model handler is not initialized.")
        
    success = f5_handler.synthesize(request.text, temp_filename)
    if not success:
        raise HTTPException(status_code=500, detail="F5-TTS synthesis failed")
        
    background_tasks.add_task(os.remove, temp_filename)
    return FileResponse(path=temp_filename, media_type="audio/wav", filename="f5_output.wav")

def synthesize_matcha(request: SynthesisRequest, background_tasks: BackgroundTasks):
    lang = request.language.lower()
    
    if not HAS_MATCHA:
        raise HTTPException(status_code=503, detail="Matcha-TTS engine is not installed/available on this server.")
        
    if lang not in models or models[lang] is None:
        raise HTTPException(status_code=503, detail=f"Matcha model for language '{lang}' is not loaded or checkpoint was not found.")
    
    model = models[lang]
    text = request.text.strip()
    
    if not text:
        raise HTTPException(status_code=400, detail="Text cannot be empty.")
        
    print(f"Synthesizing [{lang}]: {text}")
    
    try:
        x, x_lengths = process_text(text, device, lang)
        
        output = model.synthesise(
            x,
            x_lengths,
            n_timesteps=10,
            temperature=0.667,
            spks=None,
            length_scale=1.0 / request.speed if request.speed > 0 else 1.0
        )
        
        mel = output["mel"]
        
        # Check for NaN in Mel
        if torch.isnan(mel).any():
            print("Error: Mel spectrogram contains NaN values!")
            raise Exception("Mel spectrogram contains NaN values")

        audio = vocoder(mel).clamp(-1, 1)
        
        # Check for NaN in Audio
        if torch.isnan(audio).any():
            print("Error: Vocoder output contains NaN values!")
            audio = torch.nan_to_num(audio) # Fallback to avoid crash, but issue remains
            
        audio = denoiser(audio.squeeze(), strength=0.00025).cpu().squeeze()
        
        # Ensure 1D shape [T] for soundfile
        audio = audio.flatten() 
        
        # Final NaN check before saving
        if torch.isnan(audio).any():
             audio = torch.nan_to_num(audio)
             print("Warning: Audio contained NaN after denoising")

        print(f"Audio shape: {audio.shape}, dtype: {audio.dtype}")
        
        # Normalize with better safety
        abs_audio = torch.abs(audio)
        max_val = abs_audio.max().item()
        mean_val = abs_audio.mean().item()
        
        print(f"Stats - Max: {max_val:.4f}, Mean: {mean_val:.4f}")

        if max_val > 1e-7:
            audio = audio / max_val * 0.95 # Slight headroom
            print(f"Normalized audio: peak was {max_val:.4f}")
        else:
            print("Warning: Audio is effectively silent (max_val too small)")
            # Create a tiny bit of noise or 1 second of silence to avoid player issues
            if audio.shape[0] == 0:
                 audio = torch.zeros(SAMPLE_RATE)
        
        # Save to temp file
        temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".wav")
        temp_filename = temp_file.name
        temp_file.close()
        
        sf.write(temp_filename, audio.numpy(), SAMPLE_RATE)
        
        # Add cleanup task
        background_tasks.add_task(os.remove, temp_filename)
        
        return FileResponse(
            path=temp_filename, 
            media_type="audio/wav", 
            filename="output.wav"
        )
        
    except Exception as e:
        print(f"Synthesis failed: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
