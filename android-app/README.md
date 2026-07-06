# Jetro AI — MT5 Live Dashboard Android App

A premium Android app for monitoring your MT5 live trading account in real-time.  
Connects to the Flask server (`server.py`) running on your Windows PC.

---

## 📱 Features

| Screen | Description |
|--------|-------------|
| **Dashboard** | Account equity, balance, 6 metric cards (win rate, profit factor, Sharpe, drawdown, recovery factor), open positions preview |
| **Open Positions** | Live list of all open trades with SL/TP details, floating P&L |
| **History** | Closed trade log with WIN/LOSS/BUY/SELL filter chips |
| **Analytics** | Equity growth curve chart, daily P&L bar chart, symbol distribution bars |
| **Advisory** | Live signal & broadcast feed from the advisory system |
| **Settings** | Server URL configuration, connection status, setup guide |

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Hedgehog or newer) — [Download here](https://developer.android.com/studio)
- Java 8+
- Android phone (Android 8.0+)

### Build & Install

1. Open **Android Studio**
2. Click **Open** and select this folder: `c:\mt5\android-app\`
3. Wait for Gradle sync to complete (first time may take 2-3 minutes)
4. Connect your Android phone via USB and enable **USB Debugging**
5. Click **Run ▶** — the app will be built and installed

### Or build APK manually
```
cd c:\mt5\android-app
gradlew assembleDebug
```
APK will be at: `app\build\outputs\apk\debug\app-debug.apk`

---

## 🔌 Connecting to Your Server

1. Start `server.py` on your Windows PC
2. Find your PC's local IP:
   - Open CMD → type `ipconfig`
   - Look for `IPv4 Address` (e.g. `192.168.1.100`)
3. In the app → **Settings** → enter: `http://192.168.1.100:5000`
4. Both devices must be on the **same WiFi network**

> For remote access, use [ngrok](https://ngrok.com) or Cloudflare Tunnel and enter the HTTPS URL.

---

## 🎨 Design

- **Theme**: Deep Obsidian dark (#060913 background)
- **Accents**: Cyan · Violet · Emerald · Amber
- **Auto-refresh**: Every 5 seconds
- **No external chart libraries** — charts drawn with Compose Canvas

---

## 📁 Project Structure

```
android-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/jetro/mt5dashboard/
│   │   │   ├── MainActivity.kt           ← Entry point + navigation
│   │   │   ├── api/
│   │   │   │   ├── ApiModels.kt          ← Data classes matching Flask JSON
│   │   │   │   ├── MT5ApiService.kt      ← Retrofit API interface
│   │   │   │   └── RetrofitClient.kt     ← HTTP client singleton
│   │   │   ├── viewmodel/
│   │   │   │   └── MT5ViewModel.kt       ← State + auto-refresh logic
│   │   │   ├── ui/
│   │   │   │   ├── theme/                ← Colors, Typography, Theme
│   │   │   │   ├── components/           ← MetricCard, TradeRow, Charts
│   │   │   │   └── screens/              ← All 6 screens
│   │   │   └── navigation/NavGraph.kt
│   │   ├── AndroidManifest.xml
│   │   └── res/
│   └── build.gradle
└── build.gradle
```
