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
| 11 | Nebula Verge | 91-99 | — | **Pro+ only**, Diagonal Movement token debuts |
| 12 | Quantum Lattice | 100-108 | — | Pro+ only |
| 13 | Singularity Spire | 109-117 | — | Pro+ only |
| 14 | Infinity Prism | 118-126 | — | Pro+ only |

### Obstacles
- **Frozen Tiles** - Cannot be swapped but their color counts toward patterns. Shown with an ice overlay.
- **Void Cells** - Empty spaces on the board. Creates irregular board shapes. Lines cannot cross voids.

### Difficulty Modes

| | Moves | Stars | Starting World | Pass Through Goals | Tile Sharing |
|---|---|---|---|---|---|
| **Casual** | 1.25x | 1x | World 1 | Yes (breaks goal) | Multi-move |
| **Standard** | 1.0x | 2x | World 2 | No (blocked) | Multi-move |
| **Pro** | 0.7x | 3x | World 3 | No (locked) | One-move only |
| **Pro+** | 0.65x | 4x | World 11 | No (locked) | One-move only |

- **Pass Through Goals**: Casual can swap through completed goal cells (but the goal breaks). Standard, Pro, and Pro+ block swaps touching completed goals.
- **Tile Sharing**: Casual and Standard allow completed goal tiles to count toward new goals over multiple moves. Pro and Pro+ only allow tile sharing when two goals complete simultaneously from one swap.
- **Passthrough Power-Up**: Overrides blocking on all difficulties — jumps over completed goal cells and frozen tiles.
- **Pro+ exclusive worlds**: Worlds 11–14 (Nebula Verge → Infinity Prism, levels 91–126) are reserved for Pro+ players. The Diagonal Movement power-up is gated to these worlds.
- **Pro+ exclusive power-up**: Diagonal Movement tokens spawn only on World 11+ boards and are awarded as part of the Perfect Game bonus only on World 11+.

Players start at a world matching their skill level — no grinding through easy content. Players can upgrade their skill anytime in Settings (but not downgrade) — progress and unlocked worlds are preserved. Reset progress in Settings to change to a lower skill. Upgrading to Pro+ unlocks Worlds 11–14 once enough stars have been earned. Completing all 90 levels as Pro triggers a celebration with the option to upgrade to Pro+ or stay in Pro with endless randomized replay.

### World Unlock Stars

| World | Name | Casual (×1) | Standard (×2) | Pro (×3) | Pro+ (×4) |
|-------|------|-------------|---------------|----------|-----------|
| 1 | Seedling Garden | 0 | 0 | 0 | 0 |
| 2 | Blooming Meadow | 7 | 14 | 21 | 28 |
| 3 | Ancient Grove | 14 | 28 | 42 | 56 |
| 4 | Crystal Cavern | 18 | 36 | 54 | 72 |
| 5 | Shattered Isles | 42 | 84 | 126 | 168 |
| 6 | Void Fortress | 68 | 136 | 204 | 272 |
| 7 | Molten Core | 98 | 196 | 294 | 392 |
| 8 | Starfall Summit | 132 | 264 | 396 | 528 |
| 9 | Abyssal Depths | 172 | 344 | 516 | 688 |
| 10 | Prism Citadel | 240 | 480 | 720 | 960 |
| 11 | Nebula Verge | — | — | — | 1120 |
| 12 | Quantum Lattice | — | — | — | 56 |
| 13 | Singularity Spire | — | — | — | 116 |
| 14 | Infinity Prism | — | — | — | 172 |

### Power-Ups
- **Shuffle** — Rearranges remaining tiles with a mild bias toward progress when stuck (completed goals stay put)
- **Passthrough** — Next swap jumps over completed goal cells and frozen tiles
- **Unfreeze** — Tap a frozen tile to thaw it
- **Redo** — Special tiles appear on World 4+ boards (~25% chance). Capture one in a goal to earn a redo token for a free level restart
- **Diagonal Movement** (Pro+ / World 11+) — One-shot swap with any of the 8 neighbors (orthogonal or diagonal), skipping frozen tiles. Tokens spawn on World 11+ boards (~25% chance) and can be captured the same way as other tokens
- **Perfect Game** — Complete all goals in minimal moves (World 5+) for 2x stars and +1 of every token (Diagonal token also awarded on World 11+)

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
- Skill upgrade anytime from Settings (upgrade only, no downgrade)
- Randomized goal sets per level — replaying a level gives different goals each time
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
- Pro+ upgrade celebration after completing all 90 Pro levels
- Global leaderboards via Firebase (opt-in)
- Master Mode — infinite roguelike puzzles unlocked after beating all 126 levels, with 5 difficulty tiers and streak multipliers

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
  model/       - Tile, Board, Goal, Level, GameState, PlayerProgress, ChallengeMode, MasterModeState
  logic/       - BoardEngine, PatternMatcher, HintSolver, LevelLoader, ChallengeGenerator, MasterLevelGenerator
  viewmodel/   - GameViewModel
  ui/
    theme/     - 6 themes with Material3 ColorScheme
    navigation/- Screen routes
    screens/   - Splash, Home, WorldSelect, LevelSelect, Game, Settings, Profile, Stats, Leaderboard, MasterMode
    components/- GameBoardCanvas, GoalPanel, PlayerBadge, GameCompleteOverlay, MasterModeSummaryOverlay, etc.
  data/        - ProgressRepository, SettingsRepository, ProfileRepository, LeaderboardRepository, MasterModeRepository
  audio/       - AudioManager, SoundGenerator, MusicManager
```
