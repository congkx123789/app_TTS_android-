import sys
import os
from pathlib import Path

# Get project root (parent of backend folder)
ROOT_DIR = Path(__file__).resolve().parent.parent

# Mocking config for standalone test
MATCHA_DIR_DEFAULT = ROOT_DIR.parent / "Model_AI_Text_To_sound" / "Matcha-TTS"
MATCHA_DIR = os.getenv("MATCHA_DIR", str(MATCHA_DIR_DEFAULT))

if not os.path.exists(MATCHA_DIR):
    # Fallback to current sibling if not found in Model_AI
    alt_matcha = ROOT_DIR.parent / "Matcha-TTS"
    if alt_matcha.exists():
        MATCHA_DIR = str(alt_matcha)

VN_CHECKPOINT = os.path.join(MATCHA_DIR, "checkpoints_vn", "matcha_vn_ngngngan.pt")
SAMPLE_RATE = 22050

sys.path.append(str(MATCHA_DIR))

from matcha.models.matcha_tts import MatchaTTS
from matcha.hifigan.models import Generator as HiFiGAN
from matcha.hifigan.config import v1
from matcha.hifigan.env import AttrDict
from matcha.hifigan.denoiser import Denoiser
from matcha.text import text_to_sequence
from matcha.utils.utils import intersperse, get_user_data_dir, assert_model_downloaded

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

def load_models():
    print(f"Loading model from {VN_CHECKPOINT}...")
    checkpoint = torch.load(VN_CHECKPOINT, map_location=device, weights_only=False)
    model = MatchaTTS(**checkpoint['hyper_parameters'])
    model.load_state_dict(checkpoint['state_dict'])
    model.eval().to(device)

    print("Loading Vocoder...")
    save_dir = get_user_data_dir()
    vocoder_name = "hifigan_T2_v1"
    vocoder_path = save_dir / vocoder_name
    
    h = AttrDict(v1)
    hifigan = HiFiGAN(h).to(device)
    checkpoint = torch.load(vocoder_path, map_location=device, weights_only=False)
    if "generator" in checkpoint:
        hifigan.load_state_dict(checkpoint["generator"])
    else:
        hifigan.load_state_dict(checkpoint)
    hifigan.eval()
    hifigan.remove_weight_norm()
    denoiser = Denoiser(hifigan, mode="zeros")
    return model, hifigan, denoiser

def synthesize(model, vocoder, denoiser, text):
    print(f"Synthesizing: {text}")
    cleaner = "basic_cleaners_ngngngan"
    seq = text_to_sequence(text, [cleaner])[0]
    x = torch.tensor(intersperse(seq, 0), dtype=torch.long, device=device)[None]
    x_lengths = torch.tensor([x.shape[-1]], dtype=torch.long, device=device)
    
    with torch.inference_mode():
        output = model.synthesise(x, x_lengths, n_timesteps=10, temperature=0.667, spks=None, length_scale=1.0)
        mel = output["mel"]
        
        # Check for NaN in Mel
        if torch.isnan(mel).any():
            print("Error: Mel spectrogram contains NaN values!")
            return

        audio = vocoder(mel).clamp(-1, 1)
        
        # Check for NaN in Audio
        if torch.isnan(audio).any():
            print("Error: Vocoder output contains NaN values!")
            audio = torch.nan_to_num(audio)
            
        audio = denoiser(audio.squeeze(), strength=0.00025).cpu().squeeze()
        audio = audio.flatten()
        
    # Check for NaN after denoising
    if torch.isnan(audio).any():
         audio = torch.nan_to_num(audio)
         print("Warning: Audio contained NaN after denoising")

    abs_audio = torch.abs(audio)
    max_val = abs_audio.max().item()
    mean_val = abs_audio.mean().item()
    
    print(f"Stats - Max: {max_val:.4f}, Mean: {mean_val:.4f}")

    if max_val > 1e-7:
        audio = audio / max_val * 0.95
        print(f"Normalized audio: peak was {max_val:.4f}")
    else:
        print("Warning: Audio is effectively silent!")
        if audio.shape[0] == 0:
            audio = torch.zeros(SAMPLE_RATE)
    
    sf.write("debug_output.wav", audio.numpy(), SAMPLE_RATE)
    print("Saved to debug_output.wav")

if __name__ == "__main__":
    model, vocoder, denoiser = load_models()
    synthesize(model, vocoder, denoiser, "Xin chào, đây là bài kiểm tra âm thanh.")
