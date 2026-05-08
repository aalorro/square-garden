# Square Garden

A tile-swap puzzle game for Android built with Kotlin and Jetpack Compose.

## Gameplay

Slide tiles to swap them with adjacent neighbors and form color patterns to complete level goals. Use fewer moves to earn more stars.

### Goal Types
- **Line** - Form a row/column of N same-colored tiles
- **Square** - Form a 2x2 block of same-colored tiles
- **Shape** - Form an L, T, or Cross shape of same-colored tiles

### Worlds

| World | Name | Levels | Board | Features |
|-------|------|--------|-------|----------|
| 1 | Seedling Garden | 1-9 | 5x5 | Tutorial, basic goals |
| 2 | Blooming Meadow | 10-18 | 6x6 | Multi-goal levels, shapes |
| 3 | Ancient Grove | 19-27 | 7x7 | Complex shapes, 3-4 goals |
| 4 | Crystal Cavern | 28-36 | 7x7 | Frozen tiles (immovable) |
| 5 | Shattered Isles | 37-45 | 7x7 | Void cells (irregular boards) |
| 6 | Void Fortress | 46-54 | 8x8 | Frozen tiles + void cells |
| 7 | Molten Core | 55-63 | — | — |
| 8 | Starfall Summit | 64-72 | — | — |
| 9 | Abyssal Depths | 73-81 | — | — |
| 10 | Prism Citadel | 82-90 | — | — |

### Obstacles
- **Frozen Tiles** - Cannot be swapped but their color counts toward patterns. Shown with an ice overlay.
- **Void Cells** - Empty spaces on the board. Creates irregular board shapes. Lines cannot cross voids.

### Difficulty Modes

| | Moves | Stars | Starting World | Border Behavior |
|---|---|---|---|---|
| **Easy** | 1.5x | 1x | World 1 | Cosmetic |
| **Medium** | 1.0x | 2x | World 2 | Breaks on cross |
| **Hard** | 0.7x | 3x | World 3 | Locks tiles |

Players start at a world matching their skill level — no grinding through easy content. Skill is locked after profile creation; reset progress in Settings to change.

### Power-Ups
- **Shuffle** — Randomize the board when stuck (completed goals stay put)
- **Passthrough** — Next swap jumps over completed goal cells and frozen tiles
- **Unfreeze** — Tap a frozen tile to thaw it
- **Redo** — Special tiles appear on World 4+ boards (~25% chance). Capture one in a goal to earn a redo token for a free level restart
- **Perfect Game** — Complete all goals in minimal moves (World 5+) for 2x stars and +1 of every token

### Features
- Difficulty-based starting worlds (skip easy content if you're experienced)
- Drag-to-swap with animated sliding
- Vivid beveled tile rendering with embossed 3D effect and unique motifs per color
- Visual goal grid — goals shown as mini tile shapes instead of text
- Cartoony bas-relief avatar medallions with breathing animation
- Hint system (highlights quadrant containing best move)
- 6 color themes (Light, Dark, Summer, Winter, Fall, Spring)
- User profiles with emoji avatars
- Star trail animations on win
- Life system with difficulty-gated recovery
- Win streak tracking
- Google Play Games leaderboards (opt-in)

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Compose Canvas
- **Architecture**: MVVM (ViewModel + StateFlow)
- **Storage**: DataStore Preferences
- **Navigation**: Navigation Compose
- **Audio**: Procedural PCM via AudioTrack
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35

## Building

```bash
./gradlew assembleDebug
```

Requires Android Studio with JBR (JetBrains Runtime) and Android SDK installed.

## Project Structure

```
com.squaregarden/
  model/       - Tile, Board, Goal, Level, GameState, PlayerProgress
  logic/       - BoardEngine, PatternMatcher, HintSolver, LevelLoader
  viewmodel/   - GameViewModel
  ui/
    theme/     - 6 themes with Material3 ColorScheme
    navigation/- Screen routes
    screens/   - Splash, Home, WorldSelect, LevelSelect, Game, Settings, Profile
    components/- GameBoardCanvas, GoalPanel, MoveCounter, StarDisplay, etc.
  data/        - ProgressRepository, SettingsRepository, ProfileRepository
  audio/       - AudioManager
```
