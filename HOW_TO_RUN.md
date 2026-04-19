# Text-to-Sound Project Setup Guide 🚀

This guide explains how to spin up the entire application stack: the FastAPI Backend, the SQLite Database, and the Android Native Client using an Emulator.

---

## ⚡ 0. Initial Setup (Environment)

If you have **nothing** installed yet, perform these steps first:

1. **Install Python & Java**:
   ```bash
   sudo apt update
   sudo apt install python3 python3-pip openjdk-17-jdk wget
   ```
2. **Install Android SDK & Studio**:
   - Download **Android Studio** from the official website or install via Snap: `sudo snap install android-studio --classic`.
   - Open Android Studio and follow the Setup Wizard to install the **Android SDK**, **Platform Tools**, and **Build Tools**.
3. **Configure Paths**: Add these to your `~/.bashrc` or `~/.zshrc`:
   ```bash
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/emulator
   export PATH=$PATH:$ANDROID_HOME/platform-tools
   ```

---

## 🏗️ 1. Project Structure

The repository is organized following a modern full-stack mobile architecture:

```text
.
├── backend/                # FastAPI Backend, TTS logic, and SQLite DB
│   ├── app.db              # The database (auto-seeded)
│   ├── main.py             # Server entry point
│   ├── models.py           # Database schemas
│   └── requirements.txt    # Python dependencies
├── frontend/
│   ├── setup_android.sh    # Quick-setup helper (creates folders/configs)
│   └── TextToSound/        # Native Android App (Kotlin/Compose)
│       ├── app/            # Source code, UI, and ViewModels
│       ├── launch_emulator.sh # Automated emulator & boot checker
│       └── build.gradle.kts# Build configuration
├── *.epub                  # Sample local Ebooks for testing
└── HOW_TO_RUN.md           # This setup guide
```

---

## 🏗️ 2. Setup from Scratch ("From Zero")

If you are starting on a fresh machine, follow these steps in order:

### Step 2.1: System Requirements
Make sure you have the following installed:
- **Python 3.9+**: For the backend server.
- **JDK 17**: Required by Android Gradle (verify with `java -version`).
- **Android SDK**: Usually installed via Android Studio at `~/Android/Sdk`.

### Step 2.2: Backend Installation
1. Navigate to the backend folder:
   ```bash
   cd backend
   ```
2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Start the server:
   ```bash
   python3 main.py
   ```
   > **Note**: This will automatically create `app.db` and seed it if it's your first time!

### Step 2.3: Android App Setup & Launch
1. **Initialize Project Folders**: If the project structure is missing, you can run the setup script (Note: I have already initialized this for you):
   ```bash
   bash frontend/setup_android.sh
   ```
2. **Launch the Emulator**: Run this command in your terminal to open the virtual device:
   ```bash
   ~/Android/Sdk/emulator/emulator -avd Pixel_8 &
   ```
   *Alternative: Use the automated boot checker:*
   ```bash
   bash frontend/TextToSound/launch_emulator.sh
   ```
3. **Build & Install**:
   - **Open Android Studio**: Select the `/frontend/TextToSound` folder.
   - **Snap Restrictions**: If using Snap-based Android Studio, terminal builds (`./gradlew`) may fail. **Click the green ▶ Play button inside Android Studio** to build/install.

---

## 📱 3. Running the Application Stack

1. **Terminal 1**: Start Backend (`cd backend && python3 main.py`).
2. **Terminal 2**: Start Emulator (`~/Android/Sdk/emulator/emulator -avd Pixel_8 &`).
3. **Android Studio**: Click standard ▶ **Run** button to deploy the app.

---

## 🏗️ 4. Key Component Structure

- **BookViewModel.kt**: Business logic, audio caching, and reading progress.
- **AudiobookPlayerScreen.kt**: Full-screen player UI with descriptions.
- **FloatingMiniPlayer.kt**: Draggable, ultra-compact pill overlay (verified!).
- **AppDatabase.kt**: Room/SQLite database for offline storage.

---

## 🛠️ Troubleshooting

- **No Device Found**: Ensure `adb devices` lists your emulator. If not, restart the emulator.
- **Library is Empty**: verify the backend is running at `http://localhost:8000`. The app uses `10.0.2.2:8000` for emulator-to-host communication.
- **Audio Lag**: The first play might lag as models load; subsequent plays are cached.
- **Snap Errors**: If `./gradlew` fails on Linux, use the Android Studio GUI for all builds.
