<div align="center">
  <a href="read-images/images1.png"><img src="read-images/images1.png" width="80" height="120" /></a>
  <a href="read-images/images2.png"><img src="read-images/images2.png" width="80" height="120" /></a>
  <a href="read-images/images3.png"><img src="read-images/images3.png" width="80" height="120" /></a>
  <a href="read-images/images4.png"><img src="read-images/images4.png" width="80" height="120" /></a>
  <a href="read-images/images5.png"><img src="read-images/images5.png" width="80" height="120" /></a>
  <a href="read-images/images6.png"><img src="read-images/images6.png" width="80" height="120" /></a>
  <a href="read-images/images7.png"><img src="read-images/images7.png" width="80" height="120" /></a>
</div>
# 🌟 My Focus App: The Comprehensive Technical & User Documentation

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![Language](https://img.shields.io/badge/language-Kotlin-purple.svg)
![UI](https://img.shields.io/badge/UI-Jetpack_Compose-blue.svg)
![Architecture](https://img.shields.io/badge/architecture-MVVM-red.svg)

Welcome to the definitive guide for **My Focus App**, a holistic productivity and mindfulness suite designed for the modern Android ecosystem. This document serves as a full-scale technical manual, user guide, and architectural breakdown, spanning every detail of the application's lifecycle, from its UI layers to its persistent storage engines.

---

## 📖 Table of Contents

1.  [🚀 Executive Vision](#-executive-vision)
2.  [🏗 High-Level Architecture](#-high-level-architecture)
    *   [MVVM & UDF](#mvvm--udf)
    *   [Concurrency & Coroutines](#concurrency--coroutines)
3.  [🧠 The Brain: TaskViewModel.kt](#-the-brain-taskviewmodelkt)
    *   [State Management](#state-management)
    *   [Business Logic Flow](#business-logic-flow)
    *   [Money Data Management](#money-data-management)
4.  [💸 Money Tracking: MoneyTrackingScreen.kt Breakdown](#-money-tracking-moneytrackingscreenkt-breakdown)
5.  [⏱ Focus Engine: Timer & Background Service](#-focus-engine-timer--background-service)
    *   [TimerService.kt Implementation](#timerservicekt-implementation)
    *   [Foreground Execution Strategy](#foreground-execution-strategy)
6.  [🎵 Audio Suite: Media3 & PlaybackService](#-audio-suite-media3--playbackservice)
    *   [PlaybackService.kt Deep Dive](#playbackservicekt-deep-dive)
    *   [Dynamic Background Logic](#dynamic-background-logic)
7.  [📚 E-Book Engine: Parsing & Rendering](#-e-book-engine-parsing--rendering)
    *   [Docx & HTML Parsers](#docx--html-parsers)
    *   [Theming & Eye-Care](#theming--eye-care)
8.  [✅ Task Management: To-Do & Persistence](#-task-management-to-do--persistence)
    *   [Room Database Schema](#room-database-schema)
    *   [Gamification & Confetti](#gamification--confetti)
9.  [⚙️ Persistence Layer: Settings & DataStore](#-persistence-layer-settings--datastore)
    *   [SettingsManager.kt](#settingsmanagerkt)
10. [🎨 UI/UX: Design Language & Compose](#-uiux-design-language--compose)
11. [📂 File Manifest & Project Structure](#-file-manifest--project-structure)
12. [🛠 Developer Appendix: Code Snippets & Logic](#-developer-appendix-code-snippets--logic)
13. [🚀 Installation & Build Guide](#-installation--build-guide)
14. [🛡 Privacy, Security & Compliance](#-privacy-security--compliance)
15. [📈 Performance Optimization & Benchmarking](#-performance-optimization--benchmarking)
16. [❓ Frequently Asked Questions (FAQ)](#-frequently-asked-questions-faq)
17. [🗺 Future Roadmap & Version History](#-future-roadmap--version-history)
18. [📜 Licensing & Credits](#-licensing--credits)

---

## 🚀 Executive Vision

**My Focus App** is engineered to be a sanctuary in your pocket. In an era where "Attention is Currency," we aim to provide a zero-distraction environment that merges productivity tools with relaxation suites. The app is built on the philosophy that focus isn't just about "doing more," but about "being more" in the moment.

### Core Objectives:
- **Consolidation**: Eliminating context-switching by providing Music, Reading, Timing, and Budgeting in one app.
- **Privacy**: 100% offline-first. No cloud tracking, no account required.
- **Immersion**: Using Material Design 3 and smooth animations to reduce cognitive load.
- **Financial Wellness**: Providing dynamic visual feedback to help users stay within their spending goals.

---

## ✨ Key Features

### 💸 Smart Money Tracking
- **Visual Budgeting**: Circular and linear progress bars that change color based on spending thresholds (Green < 50%, Orange 50-80%, Red > 80%).
- **Blinking Alerts**: Dynamic "Pulse" animations when you exceed your monthly budget.
- **GitHub-Style Safety**: Critical deletions require manual text confirmation (typing the entry description or the word "RESET") to prevent accidental data loss.
- **Polished UX**: Beautifully crafted dialogs with "Noob-friendly" step-by-step instructions for developers.

### ⏱ Deep Work Timer
- **Foreground Service**: Keeps your timer running even if the app is closed.
- **Persistent Notifications**: Control your focus session from the lock screen.

### 🎵 Immersive Music
- **Media3 Integration**: Seamless audio playback with background support.
- **Nature & Lo-Fi tracks**: Pre-loaded focus sounds with dynamic GIF backgrounds.

### 📚 Distraction-Free Reading
- **Docx/HTML Support**: Read your favorite books directly in the app.
- **Eye-Care Mode**: Sepia themes and adjustable font sizes for long reading sessions.

---

## 🏗 High-Level Architecture

The application follows the **Modern Android Architecture (MAD)** guidelines, ensuring scalability, testability, and maintainability.

### MVVM & UDF
- **Model**: Represents the data source. We use **Room** for relational data and **DataStore** for preferences.
- **View**: A declarative UI built with **Jetpack Compose**. It observes StateFlow from the ViewModel.
- **ViewModel**: Acts as the bridge, exposing state and handling events.

### Unidirectional Data Flow (UDF)
1.  **User Action**: Taps "Start Timer".
2.  **Event**: `viewModel.toggleTask(id)` is called.
3.  **Process**: ViewModel interacts with `TimerService` and `TaskDao`.
4.  **State Update**: Room emits a new List through `Flow`.
5.  **UI Reflection**: The UI re-composes with the "Running" state.

---

## 🧠 The Brain: TaskViewModel.kt

The `TaskViewModel` is the central hub of the application. It manages the lifecycle of tasks, todos, settings, and music downloads.

### State Management
We utilize `StateFlow` to provide reactive updates to the UI.
```kotlin
val tasks: StateFlow<List<Task>> = taskDao.getAllTasks()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

val isDarkMode: StateFlow<Boolean> = settingsManager.darkModeFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
```

### Business Logic Flow: Task Toggling
When a user toggles a task, the ViewModel calculates the remaining time and decides whether to start or stop the `TimerService`.
- If starting: It sends an Intent with `ACTION_START`.
- If stopping: It sends `ACTION_STOP` and triggers haptic feedback.

### Money Data Management
The ViewModel acts as the guardian for financial data.
- **`resetMoneyData()`**: A high-privilege function that performs a clean wipe of all money entries and resets the budget limit to its default ($1,000).
- **Decoupled Logic**: Budget limits are stored separately from transaction history, ensuring that changing your monthly goal doesn't accidentally delete your past receipts.

---

## 💸 Money Tracking: MoneyTrackingScreen.kt Breakdown

If you are new to coding, this screen might look complex, but it's just a set of instructions! We've added "Line Numbers" below so you can find exactly where the magic happens in the code.

### 🗺️ The Layout (Where things are)
- **LINE 69**: `MoneyTrackingScreen` is the "Main Blueprint". Think of this as the foundation of a house; everything on this page is built inside here.
- **LINE 71-79**: **The Listeners**. These lines "listen" to the Brain (ViewModel). When you add an expense, these lines hear it and tell the screen to update immediately.
- **LINE 93-108**: **The Header**. This is the bar at the top with the title. It has a "Back" arrow and a (+) button to set your budget limit.
- **LINE 117-120**: **The Floating Button**. That big blue (+) button at the bottom? This is the code that makes it appear and opens the "Add Expense" box when clicked.

### 🎨 The Visuals (Colors & Math)
- **LINE 133-140**: **The Choice**. This code checks your settings to see if you want to see the "Big Spending Circle" or just a simple card with a progress bar.
- **LINE 151-163**: **Over-Budget Warning**. If you spend too much, this code turns the text **RED** and makes the progress bar **BLINK**. It's like a visual alarm!
- **LINE 241-279**: **Circular Money Progress**. This is a special tool that draws the circle. It calculates the math so the circle is Green (Safe), Orange (Warning), or Red (Danger) based on your budget.

### 📜 The History (Your List)
- **LINE 179-188**: **The Lazy List**. Instead of loading 1,000 items at once, this smart list only loads the entries you can actually see on the screen, saving your phone's battery.
- **LINE 280-312**: **Money Entry Item**. This describes what a single receipt looks like in your list (the name, date, and price).

### 🛡️ The Safety Nets (Settings & Reset)
- **LINE 340-424**: **Set Limit Dialog**. This is the popup where you change your monthly budget goal.
- **LINE 351-385**: **THE ULTIMATE SAFETY NET**. If you try to delete all your data, we make you type the word **"RESET"** in all caps. This prevents accidents—just like how GitHub protects important code!
- **LINE 401-415**: **The Reset Button**. We gave this button a nice border and a soft red color so you can find it easily, but it looks different because it's a "Danger" action.

---

## ⏱ Focus Engine: Timer & Background Service

Focus requires persistence. If the user leaves the app, the timer must continue.

### TimerService.kt Implementation
The `TimerService` is a **Foreground Service** that ensures the system doesn't kill the timer logic.
- **Foreground Notification**: Shows the live remaining time using `NotificationCompat`.
- **Tick Engine**: Uses a Coroutine with `delay(1000)` to update the notification every second.
- **API 34 Compliance**: Implements `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` for Android 14+ compatibility.

### Logic Analysis:
```kotlin
private fun startTimerUpdates() {
    timerJob?.cancel()
    timerJob = serviceScope.launch {
        while (isActive) {
            val elapsed = System.currentTimeMillis() - startTimeMillis
            val remaining = (initialRemainingMillis - elapsed).coerceAtLeast(0L)
            updateNotification(remaining)
            if (remaining <= 0) break
            delay(1000)
        }
    }
}
```

---

## 🎵 Audio Suite: Media3 & PlaybackService

The music engine is powered by **Android Media3 (ExoPlayer)**, providing a robust, low-latency audio experience.

### PlaybackService.kt Deep Dive
This service manages the `MediaSession`, allowing the app to be controlled via the Lock Screen, Notification Drawer, and Bluetooth devices.
- **Auto-Looping**: Custom logic ensures that focus tracks repeat seamlessly.
- **MediaController**: The UI connects to this service via a `MediaController` to sync playback state.

### Dynamic Backgrounds
Based on the track's category (e.g., "Nature", "Lo-Fi"), the `MusicScreen` dynamically loads corresponding GIFs or SVGs using the **Coil** library, creating an immersive atmosphere.

---

## 📚 E-Book Engine: Parsing & Rendering

The E-book suite is designed for "Deep Reading" sessions.

### Docx & HTML Parsers
The app includes a custom engine to read complex document formats without external dependencies.
- **Docx**: Unzips the file and parses `word/document.xml`.
- **HTML**: Uses Regex and basic XML parsing to extract clean text while preserving specific formatting like colors for spiritual texts.

### Eye-Care & Theming
A dedicated "Reading Mode" transforms the UI:
- **Sepia Theme**: Soft cream background (`#F4ECD8`) and deep brown text.
- **Dynamic Scaling**: The `ebookFontSize` is managed via DataStore, allowing users to adjust text size on the fly without losing their position.

---

## ✅ Task Management: To-Do & Persistence

The To-Do system is designed for "Quick Wins."

### Room Database Schema
- **Task Table**: Stores `id`, `name`, `initialTime`, `remainingTime`, and `characterImageName`.
- **Todo Table**: Stores `id`, `text`, and `isCompleted`.
- **Migration Logic**: Version 4 includes support for dynamic character images, with a `MIGRATION_2_3` strategy to prevent data loss.

### Gamification & Confetti
To reward completion, the `TodoScreen` features a custom **Particle System**.
- **`FallingAnimation`**: Generates random particles (confetti) that fall across the screen when a task is checked.
- **Physics**: Random X-offsets, rotation speeds, and fall durations are calculated to make the animation feel organic.

---

## ⚙️ Persistence Layer: Settings & DataStore

We use **Jetpack DataStore** for all non-relational settings. Unlike SharedPreferences, DataStore is built on Coroutines and Flow, ensuring UI consistency.

### SettingsManager.kt
```kotlin
class SettingsManager(context: Context) {
    private val dataStore = context.dataStore
    val darkModeFlow: Flow<Boolean> = dataStore.data.map { it[DARK_MODE] ?: false }
    // ... other settings for vibration, sound, font size
}
```

---

## 🎨 UI/UX: Design Language & Compose

The app is a showcase of **Material Design 3**.
- **Surfaces**: Using the `Surface` component for elevation and tonal coloring.
- **Typography**: Utilizing `MaterialTheme.typography` for consistent hierarchy across Reading and Task lists.
- **Navigation**: Implemented using `androidx.navigation.compose`, allowing for type-safe transitions between 7 major screens.

---

## 📂 File Manifest & Project Structure

### Root Package: `com.example.myapplication`
- `MainActivity.kt`: Entry point and Navigation Host.
- `TaskViewModel.kt`: Central State Manager.
- `Database.kt`: Room Persistence Config.
- `Models.kt`: Entity Definitions.
- `TimerService.kt`: Background Timing Engine.
- `PlaybackService.kt`: Background Audio Engine.
- `SettingsManager.kt`: Preference Management.

### UI Package: `com.example.myapplication.pages`
- `TimerScreen.kt`: Circular focus timer.
- `TodoScreen.kt`: Gamified task list.
- `MoneyTrackingScreen.kt`: Dynamic budget tracker with visual alerts.
- `EbookScreen.kt`: Document reader.
- `MusicScreen.kt`: Now playing interface.
- `OnlineMusicScreen.kt`: Download center.
- `DownloadedMusicScreen.kt`: Local storage manager.
- `SettingsScreen.kt`: Global preferences.

---

## 🛠 Developer Appendix: Logic Deep Dives

### Character Selection Logic
The app loads characters from `assets/characters.json`. This allows for infinite expansion of "Focus Buddies" without changing code.

### Music Download Logic
In `OnlineMusicScreen.kt`, we use a buffered stream to download tracks:
1.  Connect to URL.
2.  Open output stream to `filesDir/music`.
3.  Read in 8KB chunks while updating a `progressMap`.
4.  Handle errors and partial downloads gracefully.

### GitHub-Style Reset Confirmation
To prevent accidental data loss, the "Reset All Data" action requires a manual string match:
1.  User clicks "Reset".
2.  A dialog appears with a disabled "Confirm" button.
3.  User must type exactly `"RESET"` (case-sensitive) into an `OutlinedTextField`.
4.  The button enables only when `userInput == "RESET"`.
5.  This pattern significantly reduces "Fat Finger" errors in critical data operations.

---

## 🚀 Installation & Build Guide

1.  **Clone**: `git clone <repo-url>`
2.  **Open**: Import into Android Studio (Ladybug or newer).
3.  **Sync**: Ensure Gradle Sync completes successfully.
4.  **Run**: Deploy to a device running Android 8.0 (Oreo) or higher.

---

## 🛡 Privacy, Security & Compliance

- **No Internet Required**: All core features work offline. Internet is only used for music downloads.
- **No Permissions**: No access to Location, Contacts, or Microphone.
- **Data Encapsulation**: Database is stored in the app's internal sandbox.

---

## 📈 Performance Optimization

- **Lazy Loading**: `LazyColumn` is used for all lists to ensure O(1) memory usage regardless of list size.
- **Composition Local**: We use `LocalContext` and `LocalConfiguration` to optimize resource access.
- **State Pruning**: ViewModel state is only active when screens are subscribed, reducing battery drain.

---

## ❓ Frequently Asked Questions (FAQ)

**Q: Can I add my own books?**
A: Yes. Place `.docx` or `.htm` files in the assets folder and update `book.json`.

**Q: Why a Foreground Service?**
A: To ensure the timer isn't killed when you lock your screen, which is critical for focus apps.

---

## 🗺 Future Roadmap

- [ ] **Cloud Backup**: Optional encrypted sync.
- [ ] **Stats Dashboard**: Weekly focus charts.
- [ ] **Interactive Characters**: Animation states based on focus progress.

---

## 📜 Licensing & Credits

- **Author**: Gongchampou Kamei
- **License**: MIT
- **Special Thanks**: Jetpack Compose Community, Media3 Team.

---

*(This document represents a comprehensive line conceptual breakdown of the My, G Apps project)*
