# Application Architecture

Newsarize is built to be a resilient, offline-first application relying on modern Jetpack Compose. 

## Clean Architecture
### 1. Networking (`RssService.kt`)
Powered by `Ktor HttpClient`, the service executes GET requests securely against RSS endpoints and relies on an internal fast XML `pullParser` to detangle the mess of custom namespaces, parsing `pubDate` using multi-locale strategies to determine strict timeline relevancy.
### 2. The Database (`AppDatabase.kt`)
Newsarize wields `Android Room` SQL. 
- RSS Article URLs are marked with strict uniqueness `indices = [Index(value = ["link"], unique = true)]`.
- This ensures that duplicate network fetches naturally bounce off the database boundary rather than overwriting existing articles and destroying precious, heavily-calculated AI summaries.
### 3. Background LLM Daemon (`NewsViewModel.kt`)
All Heavy-Lifting sits in `startInferenceWorker()`. 
A `while(true)` background coroutine waits peacefully for the model verification flag. Once authorized, it executes a single database query `getNextUnsummarizedArticle()`, restricting the load to a single `Gemma` instance, completely detaching the inference cycle from the fragile Android Main UI Thread.
### 4. Hardware Inference (`MediaPipeAiService.kt`)
- We dropped `ONNX` logic in favor of Google's state-of-the-art `tasks-genai:0.10.27` unified API.
- The wrapper allocates strict `KV-Cache` token restraints (`setMaxTokens(300)`) preventing Snapdragon GPUs from choking out of VRAM ("Out of Context").
- Advanced grammatical instructions enforce standard High German compliance avoiding LLM hallucination inside INT8 limitations.
