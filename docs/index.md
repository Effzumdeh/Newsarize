# Welcome to Newsarize

**Privacy-First, On-Device News Summarization for Android**

Newsarize tackles the daily flood of information by retrieving standard RSS feeds (like Tagesschau or Heise) and intelligently condensing them down into bite-sized highlights—entirely on your smartphone.

## 🔒 Privacy-First Philosophy
We believe your reading habits belong to you. Newsarize uses **Zero Cloud APIs**. It downloads the raw news algorithms directly to your device and processes everything locally using Google's MediaPipe GPU frameworks. No text is ever sent to an external server for summarization. The app even runs perfectly in Airplane mode!

## ✨ Key Features
* **Hardware Accelerated Inference:** Optimized for modern Hexagon NPU & GPU chips (e.g., Snapdragon 8 Elite), natively running INT4/INT8 quantized models.
* **Smart UI Queuing:** A background worker silently picks off the newest articles you are currently looking at and summarizes them in milliseconds while keeping your UI utterly butter-smooth. You can also filter articles by "Alle", "Ungelesen", or "Gelesen".
* **Direct Archive Imports:** Sideload heavy 2.5 GB `.tar.gz` models directly into the app sandbox. The extraction happens on-the-fly (`.bin`, `.task`, `.tflite` natively supported).
* **Persistent Cache:** Android `Room V3` guarantees your AI summaries are cached safely. `ForeignKey.CASCADE` natively deletes all associated data efficiently if you ever unfollow a feed.
* **Universal XML Engine:** Hybrid Atom & RSS 2.0 unmarshaller reliably pulls feeds from almost any blog regardless of formatting complexities (`<item>` vs `<entry>`).
