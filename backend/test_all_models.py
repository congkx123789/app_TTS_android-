import os
import sys
import time

# Add current dir to path
sys.path.append(os.getcwd())

from piper_handler import PiperHandler, VieNeuHandler, F5TTSHandler, MatchaHandler, PIPER_MODELS

def test_piper():
    print("\n--- Testing Piper ---")
    model_info = PIPER_MODELS["medium"]
    handler = PiperHandler(model_info["model"], model_info["config"])
    output_path = "test_piper.wav"
    text = "Xin chào, đây là bài kiểm tra giọng nói từ Piper."
    success = handler.synthesize(text, output_path)
    if success and os.path.exists(output_path):
        print(f"SUCCESS: Piper generated {output_path} ({os.path.getsize(output_path)} bytes)")
    else:
        print("FAILED: Piper synthesis failed")

def test_vieneu():
    print("\n--- Testing VieNeu ---")
    model_path = "Model_API/vieneu/VieNeu-TTS-0_3B-Q4_0.gguf"
    handler = VieNeuHandler(model_path)
    # The SDK usually loads its own model or uses the provided path depending on implementation
    output_path = "test_vieneu.wav"
    text = "Xin chào, đây là bài kiểm tra giọng nói từ VieNeu."
    success = handler.synthesize(text, output_path)
    if success and os.path.exists(output_path):
        print(f"SUCCESS: VieNeu generated {output_path} ({os.path.getsize(output_path)} bytes)")
    else:
        print("FAILED: VieNeu synthesis failed")

def test_matcha():
    print("\n--- Testing Matcha-TTS ---")
    handler = MatchaHandler()
    output_path = "test_matcha.wav"
    text = "Xin chào, đây là bài kiểm tra giọng nói từ Matcha T T S."
    success = handler.synthesize(text, output_path)
    if success and os.path.exists(output_path):
        print(f"SUCCESS: Matcha-TTS generated {output_path} ({os.path.getsize(output_path)} bytes)")
    else:
        print("FAILED: Matcha-TTS synthesis failed")

if __name__ == "__main__":
    test_piper()
    test_matcha()
    test_vieneu()
    test_f5tts()
