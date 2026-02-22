# Newsarize 📰🤖

> **Newsarize** is a privacy-first, purely on-device Android application that fetches daily news from RSS feeds and summarizes them locally utilizing the massive power of the Snapdragon 8 Elite Hexagon NPU & GPU. 

![Newsarize Interface](docs/assets/screenshot.png) *(Placeholder if you want to add screenshots later)*

Newsarize was built to combat information overload without sending your private reading habits to the cloud. It downloads news streams via Ktor, cleans them, and leverages Google's **MediaPipe LLM Inference** library to run Google's **Gemma 2B** quantized models directly on your smartphone's hardware.

## ✨ Key Features
- **100% Local Inference:** No API keys, no server costs, and no data leaves your device. Everything runs through the native `tasks-genai` hardware acceleration.
- **Smart Background Queue:** Newsarize uses a continuous FIFO coroutine daemon that intelligently digests unsummarized articles in the background, prioritizing the ones you are currently looking at on the screen `pubDate DESC`.
- **Database Integrity:** Robust local caching powered by Android Room, complete with unique feed constraints to prevent duplicate entries and AI-amnesia.
- **Direct Model Sideloading:** Since LLM model files are huge (~1.2 - 2.5 GB), Newsarize expects you to sideload the model. It automatically extracts `gemma-2b-it-gpu-int4.bin` or `.tar.gz` archives and safely tucks them into the protected app sandbox.

## 🚀 Quick Start
Check out the [Installation Guide](https://florian-projects.github.io/Newsarize/installation) on our documentation site for detailed setup instructions and where to download the compatible Gemma `.bin` models!

## 📚 Documentation
Detailed documentation is hosted on GitHub Pages (generated via MkDocs Material):
👉 [Newsarize Documentation](https://florian-projects.github.io/Newsarize/)

---
*Built with modern Kotlin, Jetpack Compose, and Google MediaPipe.*
