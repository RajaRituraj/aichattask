# AIChat + Tasks

A real-time chat app with AI superpowers. Built with **Kotlin + Jetpack Compose** (Clean Architecture, MVVM, Hilt, Room, Socket.IO) and a **Node.js** backend.

## Project Structure

```
AIChat_Task/
├── android/      ← Android project (Jetpack Compose)
└── backend/      ← Node.js + Express + Socket.IO + Gemini AI
```

## Quick Start

### 1. Backend Setup

```bash
cd backend
cp .env.example .env
# Edit .env and add your Gemini API key:
#   GEMINI_API_KEY=AIza...
npm install
npm run dev     # Starts on http://localhost:3000
```

Get a free Gemini API key at: https://aistudio.google.com/apikey

### 2. Android Setup

Open `android/` in **Android Studio** and run on emulator or device.

> **Note:** The backend URL is hardcoded to `http://10.0.2.2:3000` (Android emulator → localhost). If running on a **real device**, update `NetworkModule.kt` and `ChatViewModel.kt` to use your machine's local IP (e.g. `http://192.168.x.x:3000`).

## Features

| Feature | Status |
|---|---|
| Real-time chat via Socket.IO | ✅ |
| Message history (Room DB) | ✅ |
| Connection status + offline banner | ✅ |
| Typing indicators | ✅ |
| Online user list | ✅ |
| AI Summary (streaming) | ✅ |
| Ask about chat (Q&A + message reference) | ✅ |
| Extract tasks from conversation | ✅ |
| Task list screen (filter, toggle, delete) | ✅ |

## Architecture

```
Presentation  →  ViewModel (StateFlow)  →  Composables
Domain        →  Use Cases              →  Repository Interfaces
Data          →  Repository Impls       →  Room + Socket.IO + Retrofit
DI            →  Hilt Modules
```

## Tech Stack

- **Android**: Kotlin, Jetpack Compose, Material3, Hilt, Room, Retrofit, Socket.IO client
- **Backend**: Node.js, Express, Socket.IO, Gemini 2.0 Flash (via SSE streaming)
- **Navigation**: Navigation3 (type-safe serializable routes)
