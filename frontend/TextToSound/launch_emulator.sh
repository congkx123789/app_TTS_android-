#!/bin/bash

export ADB="$HOME/Android/Sdk/platform-tools/adb"
export EMULATOR_CMD="$HOME/Android/Sdk/emulator/emulator"
export AVD_NAME="Pixel_8"

echo "==========================================="
echo "📱 ANDROID EMULATOR LAUNCHER 📱"
echo "==========================================="
echo ""

# Check if a device is connected
if ! $ADB devices | grep -q 'emulator'; then
    echo "▶️  No emulator found. Starting ($AVD_NAME)..."
    export ANDROID_AVD_HOME=~/.android/avd
    
    # Launch emulator in background
    nohup $EMULATOR_CMD -avd $AVD_NAME -no-snapshot-load > /tmp/emulator.log 2>&1 &
    
    echo "⏳ Waiting for emulator to connect..."
    $ADB wait-for-device
    echo "✅ Emulator connected!"
    
    echo "⏳ Waiting for Android to boot..."
    while [ "$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
        sleep 2
    done
    echo "✅ Emulator boot completed!"
    echo ""
else
    echo "✅ Emulator is already running!"
    echo ""
fi

echo "==========================================="
echo "🎯 NEXT STEPS FOR BUILDING:"
echo "==========================================="
echo "Because you are using the Snap version of Android Studio on Linux,"
echo "compiling from the terminal fails due to snap security restrictions."
echo ""
echo "To rebuild and install your app:"
echo "1. Open Android Studio."
echo "2. Make sure the 'Medium Phone API 36 (Mobile)' is selected at the top."
echo "3. Click the bright green ▶ RUN button (or press Shift+F10)."
echo ""
echo "Android Studio will compile the app and automatically open it on the"
echo "emulator that was just started by this script!"
echo "==========================================="
