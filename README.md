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

| | Moves | Stars | Starting World | Pass Through Goals | Tile Sharing |
|---|---|---|---|---|---|
| **Casual** | 1.25x | 1x | World 1 | Yes (breaks goal) | Multi-move |
| **Standard** | 1.0x | 2x | World 2 | No (blocked) | Multi-move |
| **Pro** | 0.7x | 3x | World 3 | No (locked) | One-move only |

- **Pass Through Goals**: Casual can swap through completed goal cells (but the goal breaks). Standard and Pro block swaps touching completed goals.
- **Tile Sharing**: Casual and Standard allow completed goal tiles to count toward new goals over multiple moves. Pro only allows tile sharing when two goals complete simultaneously from one swap.
- **Passthrough Power-Up**: Overrides blocking on all difficulties — jumps over completed goal cells and frozen tiles.

Players start at a world matching their skill level — no grinding through easy content. Skill is locked after profile creation; reset progress in Settings to change.

### World Unlock Stars

| World | Name | Casual (×1) | Standard (×2) | Pro (×3) |
|-------|------|-------------|---------------|----------|
| 1 | Seedling Garden | 0 | 0 | 0 |
| 2 | Blooming Meadow | 8 | 16 | 24 |
| 3 | Ancient Grove | 20 | 40 | 60 |
| 4 | Crystal Cavern | 35 | 70 | 105 |
| 5 | Shattered Isles | 55 | 110 | 165 |
| 6 | Void Fortress | 80 | 160 | 240 |
| 7 | Molten Core | 110 | 220 | 330 |
| 8 | Starfall Summit | 145 | 290 | 435 |
| 9 | Abyssal Depths | 185 | 370 | 555 |
| 10 | Prism Citadel | 230 | 460 | 690 |

### Power-Ups
- **Shuffle** — Randomize the board when stuck (completed goals stay put)
- **Passthrough** — Next swap jumps over completed goal cells and frozen tiles
- **Unfreeze** — Tap a frozen tile to thaw it
- **Redo** — Special tiles appear on World 4+ boards (~25% chance). Capture one in a goal to earn a redo token for a free level restart
- **Perfect Game** — Complete all goals in minimal moves (World 5+) for 2x stars and +1 of every token

### Challenge Modes (World 5+)

Special reward events triggered by exceptional play. Challenges cost no lives and award bonus stars + power-up tokens on completion.

| Challenge | Mechanic | Star Bonus | Trigger |
|-----------|----------|------------|---------|
| **Blitz Garden** | 60-second time attack, combo multipliers (2x/3x/4x...) | Combo-based | 8 consecutive progressive wins |
| **Overgrown Garden** | 9x9 board, 8 goals, 3 tries with increasing multipliers | 2x win bonus | Complete all 9 levels in a world (once per world) |
| **Shifting Sands** | Tiles scramble every 3 swaps | 2x | 5 consecutive wins without power-ups |
| **Memory Garden** | Tiles hidden, revealed near swaps only | 3x | Every perfect game (immediate) |

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
- **Audio**: Procedural PCM via AudioTrack + sampled clips via MediaPlayer
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
  model/       - Tile, Board, Goal, Level, GameState, PlayerProgress, ChallengeMode
  logic/       - BoardEngine, PatternMatcher, HintSolver, LevelLoader, ChallengeGenerator
  viewmodel/   - GameViewModel
  ui/
    theme/     - 6 themes with Material3 ColorScheme
    navigation/- Screen routes
    screens/   - Splash, Home, WorldSelect, LevelSelect, Game, Settings, Profile
    components/- GameBoardCanvas, GoalPanel, MoveCounter, StarDisplay, etc.
  data/        - ProgressRepository, SettingsRepository, ProfileRepository, PlayGamesManager
  audio/       - AudioManager, SoundGenerator, MusicManager
```
