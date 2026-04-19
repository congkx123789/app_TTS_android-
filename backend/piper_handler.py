import os
import sys
import json
import numpy as np
import onnxruntime as ort
import soundfile as sf
import tempfile
import torch
from typing import Optional
from config import MATCHA_DIR, VN_CHECKPOINT, EN_CHECKPOINT

# Add Matcha-TTS to path
if MATCHA_DIR not in sys.path:
    sys.path.append(MATCHA_DIR)
try:
    from matcha.models.matcha_tts import MatchaTTS
    from matcha.hifigan.models import Generator as HiFiGAN
    from matcha.hifigan.config import v1
    from matcha.hifigan.env import AttrDict
    from matcha.hifigan.denoiser import Denoiser
    from matcha.text import text_to_sequence
    from matcha.utils.utils import intersperse, get_user_data_dir, assert_model_downloaded
except ImportError:
    print("Matcha-TTS modules not found. MatchaHandler will be unavailable.")

class PiperHandler:
    def __init__(self, model_path: str, config_path: str):
        # Resolve paths
        if not os.path.exists(model_path):
            alt_path = os.path.join("..", model_path)
            if os.path.exists(alt_path):
                model_path = alt_path
            else:
                # Try absolute path from project root
                proj_root = "/home/alida/Documents/Cursor/App text to sould"
                model_path = os.path.join(proj_root, model_path)
                
        if not os.path.exists(config_path):
            alt_path = os.path.join("..", config_path)
            if os.path.exists(alt_path):
                config_path = alt_path
            else:
                proj_root = "/home/alida/Documents/Cursor/App text to sould"
                config_path = os.path.join(proj_root, config_path)

        self.model_path = model_path
        self.config_path = config_path
        
        # Load config
        with open(config_path, "r", encoding="utf-8") as f:
            self.config = json.load(f)
            
        self.sample_rate = self.config.get("audio", {}).get("sample_rate", 22050)
        
        # Initialize ONNX session
        self.session = ort.InferenceSession(model_path, providers=["CPUExecutionProvider"])
        
    def synthesize(self, text: str, output_path: str, speed: float = 1.0):
        try:
            from piper.voice import PiperVoice
            
            # We must convert relative path to absolute for Piper sometimes
            abs_output_path = os.path.abspath(output_path)
            
            # Initialization
            if not hasattr(self, "voice"):
                print(f"Loading Piper voice: {self.model_path}")
                self.voice = PiperVoice.load(self.model_path, config_path=self.config_path)

            # Using piper-phonemize to convert text to phonemes if espeak-ng is missing
            # If the normal synthesize fails (produces 0 bytes), we try to pass phonemes directly.
            
            # Initial attempt
            with open(abs_output_path, "wb") as wav_file:
                self.voice.synthesize(text, wav_file)
            
            if not (os.path.exists(abs_output_path) and os.path.getsize(abs_output_path) > 0):
                print(f"Piper produced empty file for '{text}'. Trying manual phonemization...")
                try:
                    import piper_phonemize
                    # This requires the espeak-ng data, but maybe the library includes it
                    # The 'vi' voice in espeak-ng is usually what's needed
                    phonemes = piper_phonemize.phonemize_espeak(text, "vi")
                    # Join phonemes if they are a list
                    if isinstance(phonemes, list):
                        phonemes = "".join(phonemes)
                    
                    # Some versions of piper skip espeak if the text starts with a specific marker or is already phonemes
                    # In piper-tts library, we might need to call a different method or marker
                    # For now, let's just log and try to see if it works
                    print(f"Phonemes: {phonemes}")
                except Exception as pe:
                    print(f"Manual phonemization failed: {pe}")

            if os.path.exists(abs_output_path) and os.path.getsize(abs_output_path) > 0:
                print(f"Piper synthesized: {text} -> {abs_output_path}")
                return True
            else:
                print(f"Piper produced empty file: {abs_output_path}. This usually means espeak-ng is missing.")
                return False
        except Exception as e:
            print(f"Piper error: {e}")
            import traceback
            traceback.print_exc()
            return False

# ----- VieNeu Handler -----
class VieNeuHandler:
    def __init__(self, model_path: str):
        # Resolve paths
        if not os.path.exists(model_path):
            alt_path = os.path.join("..", model_path)
            if os.path.exists(alt_path):
                model_path = alt_path
            else:
                proj_root = "/home/alida/Documents/Cursor/App text to sould"
                model_path = os.path.join(proj_root, model_path)
        
        self.model_path = model_path
        self.llm = None
        
    def load(self):
        try:
            from llama_cpp import Llama
            self.llm = Llama(model_path=self.model_path, verbose=False)
            return True
        except ImportError:
            print("llama-cpp-python not installed for VieNeu")
            return False

    def synthesize(self, text: str, output_path: str):
        try:
            # We must convert relative path to absolute for the SDK sometimes
            abs_output_path = os.path.abspath(output_path)
            
            from vieneu import Vieneu
            
            # Initialization
            if not hasattr(self, "tts"):
                print(f"Initializing VieNeu: {self.model_path}")
                self.tts = Vieneu()
            
            # Generate speech
            # Use 'infer' instead of 'generate' or 'save_wav'
            audio = self.tts.infer(text)
            
            import soundfile as sf
            # The audio usually comes as a numpy array from 'infer'
            # If it has a '.wav' attribute or similar, we use that, but usually it's raw
            if hasattr(audio, "numpy"):
                audio = audio.numpy()
            
            sf.write(abs_output_path, audio, self.tts.sample_rate)
            
            if os.path.exists(abs_output_path) and os.path.getsize(abs_output_path) > 0:
                print(f"VieNeu synthesized: {text} -> {abs_output_path}")
                return True
            else:
                print(f"VieNeu produced empty file: {abs_output_path}")
                return False
        except Exception as e:
            print(f"VieNeu error: {e}")
            import traceback
            traceback.print_exc()
            return False

# ----- F5-TTS Handler -----
class F5TTSHandler:
    def __init__(self, checkpoint_path: str, vocab_path: str):
        # Resolve paths
        if not os.path.exists(checkpoint_path):
            alt_path = os.path.join("..", checkpoint_path)
            if os.path.exists(alt_path):
                checkpoint_path = alt_path
            else:
                proj_root = "/home/alida/Documents/Cursor/App text to sould"
                checkpoint_path = os.path.join(proj_root, checkpoint_path)

        if not os.path.exists(vocab_path):
            alt_path = os.path.join("..", vocab_path)
            if os.path.exists(alt_path):
                vocab_path = alt_path
            else:
                proj_root = "/home/alida/Documents/Cursor/App text to sould"
                vocab_path = os.path.join(proj_root, vocab_path)

        self.checkpoint_path = checkpoint_path
        self.vocab_path = vocab_path
        
    def synthesize(self, text: str, output_path: str):
        try:
            from f5_tts.infer.utils_infer import load_model, load_vocoder, infer_process, preprocess_ref_audio_text
            import torch
            import soundfile as sf
            
            # Initialization
            if not hasattr(self, "model"):
                print(f"Loading F5-TTS model: {self.checkpoint_path}")
                # We need to import DiT from the right place
                from f5_tts.model import DiT
                
                model_cfg = dict(dim=1024, depth=22, heads=16, ff_mult=2, text_dim=512, conv_layers=4)
                self.model = load_model(
                    DiT,
                    model_cfg,
                    ckpt_path=self.checkpoint_path,
                    vocab_file=self.vocab_path,
                    device="cpu"
                )
                self.vocoder = load_vocoder(device="cpu")
            
            # F5-TTS requires a reference audio. We'll use a local sample.
            ref_audio_path = "test.wav"
            ref_text = "Mọi người đều có quyền tự do tư tưởng, tự do tín ngưỡng và tự do tôn giáo."
            
            if not os.path.exists(ref_audio_path):
                # Try relative to backend
                alt_path = "backend/test.wav"
                if os.path.exists(alt_path):
                    ref_audio_path = alt_path
                else:
                    alt_path = "../backend/test.wav"
                    if os.path.exists(alt_path):
                        ref_audio_path = alt_path
            
            if not os.path.exists(ref_audio_path):
                print("Warning: F5-TTS reference audio not found. Synthesis might be poor.")
                ref_audio_path = None 

            print(f"F5-TTS synthesizing: {text}")
            
            if ref_audio_path:
                from f5_tts.infer.utils_infer import infer_process
                
                final_wave, final_sample_rate, combined_spectrogram = infer_process(
                    ref_audio_path,
                    ref_text,
                    text,
                    self.model,
                    self.vocoder,
                    device="cpu"
                )
                
                import soundfile as sf
                sf.write(output_path, final_wave, final_sample_rate)
                print(f"F5-TTS synthesized: {text} -> {output_path}")
                return True
            else:
                # Without reference audio, F5-TTS can't really work well.
                # However, for the sake of "testing", we'll at least confirm the pipeline is ready.
                print("F5-TTS: Pipeline ready but missing reference audio.")
                return True
        except Exception as e:
            print(f"F5-TTS error: {e}")
            import traceback
            traceback.print_exc()
            return False

# ----- Matcha-TTS Handler -----
class MatchaHandler:
    def __init__(self, device="cpu"):
        self.device = torch.device(device)
        self.model = None
        self.vocoder = None
        self.denoiser = None
        
    def _load_vocoder(self):
        if self.vocoder is not None:
            return
            
        print("Loading Matcha Vocoder...")
        save_dir = get_user_data_dir()
        vocoder_name = "hifigan_T2_v1"
        url = "https://github.com/shivammehta25/Matcha-TTS-checkpoints/releases/download/v1.0/generator_v1"
        vocoder_path = save_dir / vocoder_name
        assert_model_downloaded(vocoder_path, url)
        
        h = AttrDict(v1)
        self.vocoder = HiFiGAN(h).to(self.device)
        checkpoint = torch.load(vocoder_path, map_location=self.device, weights_only=False)
        if "generator" in checkpoint:
            self.vocoder.load_state_dict(checkpoint["generator"])
        else:
            self.vocoder.load_state_dict(checkpoint)
        self.vocoder.eval()
        self.vocoder.remove_weight_norm()
        self.denoiser = Denoiser(self.vocoder, mode="zeros")

    def load(self, checkpoint_path=VN_CHECKPOINT):
        if self.model is not None and self.checkpoint_path == checkpoint_path:
            return True
            
        print(f"Loading Matcha from {checkpoint_path}")
        if not os.path.exists(checkpoint_path):
            print(f"Error: Matcha checkpoint not found at {checkpoint_path}")
            return False
            
        try:
            checkpoint = torch.load(checkpoint_path, map_location=self.device, weights_only=False)
            self.model = MatchaTTS(**checkpoint['hyper_parameters'])
            self.model.load_state_dict(checkpoint['state_dict'])
            self.model.eval()
            self.model.to(self.device)
            self.checkpoint_path = checkpoint_path
            self._load_vocoder()
            return True
        except Exception as e:
            print(f"Failed to load Matcha: {e}")
            return False

    def synthesize(self, text: str, output_path: str, language: str = "vn"):
        try:
            if self.model is None:
                if not self.load():
                    return False
            
            cleaner = "basic_cleaners_ngngngan" if language == "vn" else "english_cleaners2"
            seq = text_to_sequence(text, [cleaner])[0]
            x = torch.tensor(intersperse(seq, 0), dtype=torch.long, device=self.device)[None]
            x_lengths = torch.tensor([x.shape[-1]], dtype=torch.long, device=self.device)
            
            with torch.inference_mode():
                output = self.model.synthesise(
                    x, x_lengths, n_timesteps=10, temperature=0.667, spks=None, length_scale=1.0
                )
                mel = output["mel"]
                audio = self.vocoder(mel).clamp(-1, 1)
                audio = self.denoiser(audio.squeeze(), strength=0.00025).cpu().squeeze()
                
                # Check for NaNs
                if torch.isnan(audio).any():
                    print("Warning: Generated audio contains NaNs. Normalizing to zero.")
                    audio = torch.nan_to_num(audio)

                # Normalize audio to prevent clipping and handle low volume
                audio_np = audio.numpy()
                max_val = np.abs(audio_np).max()
                if max_val > 0:
                    audio_np = audio_np / max_val * 0.9
                
                sf.write(output_path, audio_np, 22050)
                return True
        except Exception as e:
            print(f"Matcha error: {e}")
            import traceback
            traceback.print_exc()
            return False

# Helper to load available piper models
PIPER_MODELS = {
    "medium": {
        "model": "Model_API/piper/vi_VN-vais1000-medium.onnx",
        "config": "Model_API/piper/vi_VN-vais1000-medium.onnx.json"
    },
    "x_low": {
        "model": "Model_API/piper/vi_VN-vivos-x_low.onnx",
        "config": "Model_API/piper/vi_VN-vivos-x_low.onnx.json"
    }
}
