# Changelog

All notable changes to Square Garden are documented in this file.

## [1.3.0] - 2026-05-05

### Added
- **Difficulty-based starting worlds** — Easy starts at World 1, Medium at World 2, Hard at World 3
  - Experienced players jump straight into challenging content without grinding through easy levels
  - `Difficulty` enum gains `startingLevel` and `startingWorld` properties
  - `PlayerProgress.highestUnlockedLevel()` accepts a floor parameter to ensure starting levels are always accessible
- Worlds at or below the starting world bypass star-gating requirements
- Play button now navigates directly to the player's current world (based on progress and difficulty)
- Difficulty picker in Profile screen shows which world each difficulty starts at

### Changed
- **Full project rename** from PatternGarden to SquareGarden
  - Package: `com.patterngarden` → `com.squaregarden`
  - Application class: `PatternGardenApp` → `SquareGardenApp`
  - Theme/Typography/NavGraph identifiers renamed accordingly
  - `applicationId`, `namespace`, `rootProject.name` all updated
- HomeScreen Play button goes to LevelSelect (current world) instead of WorldSelect
- Added CLAUDE.md project guide

### Notes
- Changing difficulty never revokes earned progress — uses `maxOf(earned, startingLevel)`
- Players can adjust difficulty anytime in their profile

## [1.2.0] - 2026-05-04

### Added
- **3 new worlds** (24 levels total, levels 26-49)
  - World 4: Crystal Cavern - introduces frozen/immovable tiles
  - World 5: Shattered Isles - introduces void cells (irregular board shapes)
  - World 6: Void Fortress - combines both frozen tiles and void cells on 8x8 boards
- **Frozen tiles** - tiles that cannot be swapped but whose color still counts toward pattern goals, rendered with ice overlay
- **Void cells** - empty board positions that create irregular board shapes; lines and patterns cannot cross voids
- Frozen/void awareness in pattern matching, hint solver, and board generation
- Vertical scroll on World Select screen to accommodate 6 worlds

### Changed
- "Next Level" button now works up to level 49 (was 25)
- HintSolver skips frozen and void cells when searching for swaps
- Drag gestures blocked on frozen and void cells

## [1.1.0] - 2026-05-03

### Added
- **Difficulty modes** (Easy/Medium/Hard) with move multipliers and star multipliers
- **Life system** with difficulty-gated recovery (must win at same or higher difficulty to restore lives)
- **Win streak tracking** for life restoration
- **Games played counter** on player badge
- **Life restore notification** - shows wins needed to restore a life (10s or touch to dismiss)
- **Last won level highlight** on Level Select screen with primary border
- **Level name labels** on Level Select grid squares
- **User profiles** with emoji avatars, username, year of birth, theme picker
- **6 color themes**: Light, Dark, Summer, Winter, Fall, Spring
- **Level badge** showing highest unlocked level on all screens
- **Star trail animation** on win with dramatic arc
- **Confetti overlay** scaled to stars earned
- Tutorial on levels 1-3

### Fixed
- L-shape detection now uses 8 orientations (4 rotations + mirror/flip) instead of 4
- Star display shows actual stars earned (was capped at 3)
- Star counts consistent across Home, World Select, and player badge (cumulative total)
- One-time migration to seed cumulative star total from per-level bests
- Back button disabled after first move to prevent mid-game exit, re-enabled on win/loss

### Changed
- World 2 goals increased to 2-4 per level (was 1-4)
- World 3 goals increased to 3-4 per level (was 2-4)
- Welcome greeting font increased to 40sp
- Back buttons styled with visible oval borders (1.5dp primary) and 28sp font across all screens
- Level grid changed from 4 to 3 columns with top spacing to avoid badge overlap

## [1.0.0] - 2026-05-01

### Added
- Initial release with 25 handcrafted levels across 3 worlds
- Drag-to-swap gameplay with animated sliding
- Embossed tile rendering with shadow/highlight/sheen layers and unique motifs
- 3 goal types: Line, Square, Shape (L/T/Cross)
- 4 tile colors: Red, Blue, Yellow, Green
- Star rating system (1-3 stars based on remaining moves)
- Hint system highlighting best-move quadrant
- World unlock system based on cumulative stars
- DataStore-based progress persistence
