# Newsarize 📰🤖

> **Newsarize** is a privacy-first, purely on-device Android application that fetches daily news from RSS feeds and summarizes them locally utilizing the massive power of the Snapdragon 8 Elite Hexagon NPU & GPU. 

![Newsarize Interface](docs/assets/screenshot.png) *(Placeholder if you want to add screenshots later)*

Newsarize was built to combat information overload without sending your private reading habits to the cloud. It downloads news streams via Ktor, cleans them, and leverages Google's **MediaPipe LLM Inference** library to run Google's **Gemma 2B** quantized models directly on your smartphone's hardware.

## ✨ Key Features
- **100% Local Inference:** No API keys, no server costs, and no data leaves your device. Everything runs through the native `tasks-genai` hardware acceleration.
- **Edge-to-Edge Material Design 3:** Enjoy a beautiful, modern Android 16 inspired interface featuring `ElevatedCards`, BottomSheets, swipe-to-dismiss interactions, and true immersion underneath system navigation bars.
- **Responsive Architecture**: Intelligent UI coroutine throttling and background `Dispatchers.IO` handling ensure the Android interface never freezes—even when unpacking massive 2GB models or generating heavy AI loops.
- **Universal XML Unmarshaller:** Newsarize natively digests both standard RSS 2.0 and complex Atom structures (like Heise & Caschys Blog), reliably capturing deep ISO-8601 timestamps, extracting `<enclosure>` URLs, and fetching in-line `<img src>` tags straight from HTML payloads to display elegant article thumbnails natively using **Coil**.
- **Smart Background Queue & Filtering:** A continuous FIFO coroutine daemon intelligently digests unsummarized articles in the background, carefully throttling GPU/CPU usage avoiding native crashes, infinite loops, and handling missing tag configurations smoothly. The UI features segmented chips to instantly filter lists by "Alle", "Ungelesen", or "Gelesen", alongside specific RSS provider dropdowns. Filter changes instantly snap back to the top of the feed.
- **Modern SPA WebView Native Scrolling:** Reading complex Javascript-heavy modern sites (like `heise.de`) works flawlessly via an embedded `DOMStorage` enabled Compose WebView.
- **Interactive UI Feedback:** Tapping the refresh button fetches new background content seamlessly while providing instant Snackbar feedback ("X neue Artikel gefunden") and cleanly auto-scrolling to the top to show the latest news.
- **Zero-Auto-Init & Hardware Fallback:** Newsarize features a robust startup architecture. The AI engine is decoupled from the main app launch to prevent native GPU driver race conditions. Users have manual control to start/stop the engine, and the app automatically falls back to CPU if the GPU context remains locked.
- **SQLite Projection Hardening:** To avoid `CursorWindow` overflows with massive HTML payloads, Newsarize uses lean UI projections and ID-based background fetching, ensuring a smooth and un-crashable experience even with thousands of archived articles. A strict context-limit guard also prevents the underlying MediaPipe engine from crashing on ultra-long RSS descriptions (e.g. Gamestar feeds).
- **Database Integrity & Cascading:** Robust local caching powered by Android Room v3. Deleting an RSS Feed from your settings immediately executes a `ForeignKey.CASCADE` delete natively, wiping out all bloated AI article data securely while preserving clean caches.
- **Direct Model Sideloading:** Since LLM model files are huge (~1.2 - 2.5 GB), Newsarize expects you to sideload the model. It automatically extracts Kaggle `gemma-2b-it-gpu-int4` archives effortlessly supporting `.bin`, `.tflite`, and `.task` file extensions natively!

## 🚀 Quick Start
Check out the [Installation Guide](https://florian-projects.github.io/Newsarize/installation) on our documentation site for detailed setup instructions and where to download the compatible Gemma `.bin` models!

## 📚 Documentation
Detailed documentation is hosted on GitHub Pages (generated via MkDocs Material):
👉 [Newsarize Documentation](https://florian-projects.github.io/Newsarize/)

---
*Built with modern Kotlin, Jetpack Compose, and Google MediaPipe.*
