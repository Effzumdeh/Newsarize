# Installation

Newsarize relies on heavily quantized local LLMs to function without an internet connection. Follow these steps to get started.

## 1. Get the App
Clone the repository and build the APK via Android Studio:
```bash
git clone https://github.com/florian-projects/Newsarize.git
cd Newsarize
./gradlew assembleDebug
```
*Alternatively, download the `app-debug.apk` directly from the Releases page on GitHub.*

## 2. Obtain the Gemma Model
Due to Google's licensing and file size limits (Android 16 restricts App data folders), you must provide the LLM model manually.
1. Visit Kaggle or HuggingFace and search for the `gemma-2b-it` model.
2. Download the `gpu-int8` or `gpu-int4` variant.
3. Save the `.tar.gz` archive, or a raw `.bin`, `.task`, or `.tflite` file directly into your Android device's `Downloads` folder.

## 3. Link the Model
1. Open Newsarize on your phone.
2. Navigate to the **Settings** (Gear Icon).
3. Tap **"Select Downloaded Model File"**.
4. The app uses the `ContentResolver` to silently extract the archive (if necessary) and copy the massive 2 GB model file into its protected internal sandbox.
5. Once imported, you are ready to go!
