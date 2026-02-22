# Usage & Features

The Newsarize interface is divided into a few key areas designed for daily information consumption. 

## The Daily Briefing
Newsarize opens directly to the **Daily Briefing Screen**. 
It checks your database, downloads the latest articles from your subscribed XML sources (via Ktor HTTP Requests), and generates a specialized list of cards.

### Smart Popcorn Queue
The magic is under the hood:
1. You drag down to refresh.
2. The UI paints the latest raw headlines instantly.
3. Our silent background `Coroutines` daemon (`NewsViewModel`) spins up the imported AI model and picks off the exact articles *currently visible* on your screen (`ORDER BY pubDate DESC`).
4. Instead of freezing your application while the device processes, the AI Summaries dynamically **plop** into existence piece-by-piece underneath each headline.

## Managing Subscriptions
1. Open the **Settings Screen**.
2. Tap on "Manage RSS Feeds".
3. Here you can paste custom RSS endpoints (`.xml` or atom formats). 
4. Pre-configured feeds include `Tagesschau` and `Heise`.
5. Any deleted feed instantly purges unrelated cached articles from your local memory.

## AI Debugging
The backend automatically falls back to an error summary string if the AI model throws an out-of-bounds error or encounters a `GPU` architecture fault. If a model inference fails, delete the `.bin` or `.tar.gz` model from settings, force close the app, and import a fresh Gemma download.
