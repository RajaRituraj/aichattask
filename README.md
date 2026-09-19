# AI Chat + Tasks 🚀

A real-time messaging application with integrated AI features. Built with a modern Android stack (Kotlin, Jetpack Compose, Clean Architecture) and a Node.js backend powered by Gemini 2.0 Flash.

## 🌟 Overview

This app allows users to chat in real-time and leverage AI to be more productive. The AI can summarize long conversations, answer questions about the chat history, and automatically extract tasks/todos from messages.

### Features
1. **Real-time Chat**: Connect with others instantly via Socket.IO.
2. **AI Summarization**: Get a quick, real-time streamed summary of the current conversation.
3. **Ask the AI**: Ask questions like "Who is handling the API?" and get answers based on the chat history.
4. **Task Extraction**: The AI automatically reads the chat and pulls out action items (Tasks), ownership, and due dates.

---

## 🏗️ Architecture & Tech Stack

This project follows **Clean Architecture** principles and the **MVVM (Model-View-ViewModel)** pattern. It is split into two main parts: the Android App and the Backend.

### Android App (`/android`)
- **UI Toolkit**: Jetpack Compose
- **Architecture**: Clean Architecture (Domain -> Data -> Presentation)
- **Dependency Injection**: Dagger Hilt (`2.60.1`)
- **Networking/Real-time**: Retrofit 2 & Socket.IO Client
- **Local Database**: Room Database (for caching messages and tasks)
- **Navigation**: Navigation3 API

### Backend (`/backend`)
- **Runtime**: Node.js
- **Framework**: Express.js
- **Real-time**: Socket.IO (handles chat rooms and message broadcasting)
- **AI Integration**: `@google/genai` (Gemini 2.0 Flash for summaries, Q&A, and task extraction)

---

## 🔄 How the Data Flows (For Junior Devs)

Understanding how data moves through this app is the key to understanding the codebase.

### 1. Sending a Chat Message
1. **Presentation Layer**: User types a message in `ChatScreen` and clicks send. The `ChatViewModel` receives this intent.
2. **Domain Layer**: The ViewModel calls the `SendMessageUseCase`.
3. **Data Layer**: The UseCase calls `ChatRepositoryImpl`, which uses the `SocketService` to emit a `send_message` event to the Node.js backend.
4. **Backend**: The Node.js server receives the message, attaches a timestamp, and broadcasts it to everyone in the room via Socket.IO.
5. **Back to Android**: The `SocketService` receives the incoming broadcast, saves it to the local **Room Database**, and updates the `StateFlow`. The UI automatically recomposes to show the new message.

### 2. Using the AI (e.g., Task Extraction)
1. **Presentation Layer**: User opens the AI Panel (`AiPanelSheet`) and selects "Tasks". `AiPanelViewModel` handles this.
2. **Domain Layer**: The ViewModel calls `ExtractTasksUseCase`, passing the recent chat history.
3. **Data Layer**: `AiRepositoryImpl` makes an HTTP POST request via **Retrofit** to the backend's `/ai/extract-tasks` endpoint.
4. **Backend**: Node.js takes the chat history, constructs a strict prompt, and sends it to the **Gemini 2.0 Flash** model, instructing it to return a JSON array of tasks.
5. **Back to Android**: The JSON response is parsed into Kotlin data classes (`Task` model) and displayed in the UI.

---

## 🛠️ Setup Instructions

### 1. Setup the Backend
1. Open a terminal and navigate to the `backend` folder:
   ```bash
   cd backend
   ```
2. Install the dependencies:
   ```bash
   npm install
   ```
3. Get a free Gemini API Key from [Google AI Studio](https://aistudio.google.com/apikey).
4. Create a `.env` file in the `backend` folder and add your key:
   ```env
   GEMINI_API_KEY=AIzaSy_your_api_key_here
   PORT=3000
   ```
5. Start the server:
   ```bash
   npm run dev
   ```

### 2. Setup the Android App
1. Open **Android Studio**.
2. Select **File > Open** and choose the `android` folder inside this repository.
3. Let Gradle sync and download all dependencies (this may take a few minutes).
4. *Important*: Ensure your Android emulator or physical device is running.
5. Click the **Run (▶️)** button in Android Studio to install and launch the app.

*(Note: The app expects the backend to be running on `http://10.0.2.2:3000` which is the default localhost alias for Android emulators. If testing on a physical device, update the BASE_URL in `NetworkModule.kt` to your computer's local IP address).*

---

## 🤝 Contributing
Feel free to fork this project, open issues, or submit Pull Requests! Ensure you follow the Clean Architecture structure when adding new features.
