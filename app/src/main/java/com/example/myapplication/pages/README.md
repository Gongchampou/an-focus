# App Pages Documentation

This directory contains the core UI screens of the application. Each screen is built using **Jetpack Compose** and follows a modular design.

## 📱 Available Screens

### 1. [EbookScreen.kt](./EbookScreen.kt)
**Purpose**: The central hub for the digital library and reading experience.
- **Features**:
  - **Grid Library**: Displays books from `book.json` using `LazyVerticalGrid`.
  - **Category Filtering**: Allows users to switch between different book categories.
  - **Integrated Reader**: A full-screen overlay for reading book content with adjustable font sizes.
  - **SVG Support**: Handles inline SVG strings for book covers.
- **Why it exists**: To provide a clean, accessible way for users to browse and read text-heavy content like stories and prayers.

### 2. [MusicScreen.kt](./MusicScreen.kt)
**Purpose**: The main audio playback interface.
- **Features**:
  - **Media3 Integration**: Connects to a `MediaController` for robust background playback.
  - **Dynamic Playlist**: Loads tracks from a JSON source.
  - **Playback Controls**: Play/Pause, Skip, and Seek functionality.
  - **Visualizations**: Support for GIF/SVG album art and basic animations.
- **Why it exists**: To manage the playback of songs, chants, or guided prayers.

### 3. [OnlineMusicScreen.kt](./OnlineMusicScreen.kt)
**Purpose**: Interface for streaming audio from remote servers.
- **Features**:
  - **Network Loading**: Fetches track lists from online APIs.
  - **Streaming**: Uses `Media3` to stream audio without local storage.

### 4. [DownloadedMusicScreen.kt](./DownloadedMusicScreen.kt)
**Purpose**: Offline access to audio content.
- **Features**:
  - **Local Storage Management**: Lists files stored in the app's internal/external storage.
  - **Offline Playback**: Ensures music works without an internet connection.

### 5. [TimerScreen.kt](./TimerScreen.kt)
**Purpose**: A tool for timed activities (e.g., meditation, timed prayer).
- **Features**:
  - **Count-down Logic**: Interactive timer with start/pause/reset.
  - **Visual Progress**: Circular progress indicators.

### 6. [TodoScreen.kt](./TodoScreen.kt)
**Purpose**: A productivity tool for managing religious or personal tasks.
- **Features**:
  - **Task List**: CRUD operations for tasks.
  - **Persistence**: Likely backed by Room database for saving state.

### 7. [SettingsScreen.kt](./SettingsScreen.kt)
**Purpose**: Application configuration.
- **Features**:
  - **Theme Toggles**: Light/Dark mode.
  - **Font Preferences**: Global font scaling.
  - **About Section**: App version and developer info.

---

## 🛠 Instructions for Development

1. **Adding a New Screen**:
   - Create a new `.kt` file in this directory.
   - Use the `@Composable` annotation.
   - Register the new screen in your main navigation host (usually in `MainActivity.kt` or a `NavHost`).

2. **State Management**:
   - Prefer `rememberSaveable` for simple UI state.
   - Use `ViewModel` for complex business logic (e.g., fetching data from JSON/API).

3. **Styling**:
   - Always use `MaterialTheme.colorScheme` and `MaterialTheme.typography` to ensure consistency with the app's theme.
