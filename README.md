# Safe Square 🎲🛡️

**Safe Square** is a modern, polished local two-player strategy dice race game built for Android using **100% Jetpack Compose**. Players compete to race their three tokens clockwise around a 28-space shared track, utilizing strategically placed **Safe Squares** to avoid captures before heading down their private home lane.

---

## 🌟 Key Features

- **🎮 Local 2-Player Gameplay**: Turn-based offline multiplayer on a single device with intuitive turn indicators, die roll animations, and move highlights.
- **🛡️ Strategic Safe Squares**: 6 designated protected squares on the board (including player entries) where tokens are completely immune to captures.
- **💥 Capture Mechanic**: Land on an opponent's token on any standard track space to send it back to START.
- **🎨 Custom Board Themes**: Choose between **Classic** (warm wood & ivory) and **Midnight** (dark neon contrast) board aesthetics, with full support for System Light and Dark modes.
- **🔊 Procedural Audio & Haptics**: Realistic sound effects synthesized in memory at launch (no external WAV/MP3 assets required) paired with haptic feedback.
- **📊 Persistent Statistics & Settings**: Track games played, win counts for Player 1 & 2, and maximum captures per match, stored safely via Jetpack DataStore.
- **🧠 Pure Kotlin Rules Engine**: Game logic is completely decoupled from UI and framework dependencies, backed by comprehensive unit tests.

---

## 🎯 Game Rules & Mechanics

| Rule Phase | Description |
|---|---|
| **Entry (Exit Roll)** | A player must roll a **6** to bring a token out of START onto their designated entry square. Rolling a **6** also awards an extra roll! |
| **Movement** | Move a token forward by the exact number of spaces shown on the die. Tokens can pass over other tokens on the track. |
| **Safe Squares** | Designated with a shield icon. Tokens on a Safe Square **cannot be captured**. Only **one** token can occupy a Safe Square at a time—if occupied, no other token can land on it. |
| **Capturing** | Landing directly on an opponent's token on a normal track space captures it and sends it back to START. |
| **Blocking** | You cannot land on your own token or on an occupied Safe Square. |
| **Home Lane** | After completing 1 full lap (28 spaces), tokens enter their private 3-space Home Lane. |
| **Winning** | Moving into **HOME** requires an exact die roll (no overshooting). The first player to bring all **3 tokens HOME** wins! |

---

## 🛠️ Tech Stack & Architecture

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 components and custom `Canvas` drawing for board rendering, die animations, and token glyphs.
- **Architecture**: Modern Android Architecture using `ViewModel`, `StateFlow`, and Unidirectional Data Flow (UDF).
- **Navigation**: `androidx.navigation.compose` for seamless transitions between Splash, Home, Game, Results, Settings, How to Play, and About screens.
- **Storage**: `DataStore Preferences` for storing game statistics and user configurations (theme, fast movement, legal move highlights, sound, haptics).
- **Audio Engine**: Custom procedural WAV synthesizer using Android `SoundPool` that generates sound clips on first launch without increasing APK size.
- **Testing**: JUnit unit tests covering the pure Kotlin game rules (`SafeSquareRulesTest`).

---

## 📂 Project Structure

```
SafeSquare/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/safesquare/
│   │   │   │   ├── data/             # SettingsRepository & DataStore Models
│   │   │   │   ├── feedback/         # SoundEngine (Procedural Sfx) & Haptics
│   │   │   │   ├── game/             # SafeSquareRules, BoardLayout & Game Models
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/   # Canvas GameBoard, Drawing Utilities & UI Components
│   │   │   │   │   ├── game/         # GameScreen & GameViewModel
│   │   │   │   │   ├── screens/      # HomeScreen, ResultScreen, SettingsScreen, etc.
│   │   │   │   │   └── theme/        # Material3 Theme & Custom Board Palettes
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── SafeSquareApplication.kt
│   │   └── test/                    # Unit tests for SafeSquareRules
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Ladybug or newer recommended.
- **JDK**: Java 11 or higher.
- **Android SDK**:
  - Minimum SDK: `24` (Android 7.0 Nougat)
  - Target SDK: `37`

### Build & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/Safe-Square.git
   cd Safe-Square
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle project files.
4. Select an Android Emulator or physical device and click **Run** (`Shift + F10`).

### Running Tests

To run the unit test suite for the game rules engine:
```bash
./gradlew test
```

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
