# Master Mode Design Spec

## Overview
Master Mode is an infinite roguelike puzzle mode unlocked after beating level 126. It provides endgame replayability with procedurally generated boards of varying size/difficulty, streak-based progression, and random challenge events.

## Entry Requirements
- `UserProfile.masteryBadgeEarned == true` (set after beating level 126)
- Accessible via golden "Master Mode" button on HomeScreen
- Uses shared life pool with main game (cooldown applies)

## Sentinel Values
- **Level ID**: `-999` (`GameViewModel.MASTER_MODE_SIGNAL`) triggers Master Mode
- **World**: `-1` (`MasterLevelGenerator.MASTER_WORLD`) for all master mode levels
- **Per-game IDs**: `-100 - gamesPlayed` (e.g., -100, -101, -102...)

## Tier System

| Tier | Board Sizes | Goals | Colors | Frozen | Moves Mult | Base Stars |
|------|-------------|-------|--------|--------|------------|------------|
| Warming Up | 5x5, 5x6 | 3-4 | 3-4 | 0-2 | 1.0x | 2 |
| Steady | 5x5-6x6 | 3-5 | 3-5 | 1-4 | 0.9x | 3 |
| Heating Up | 6x6-7x7 | 4-6 | 4-5 | 2-6 | 0.8x | 5 |
| Intense | 7x7-8x8 | 5-8 | 4-6 | 4-10 | 0.7x | 8 |
| Brutal | 8x8-9x9 | 6-10 | 5-6 | 6-14 | 0.6x | 12 |

**Move budget**: Tier `moveMultiplier` is the sole factor. Skill-level move multiplier does NOT stack.

### Tier Selection Weights (by gamesPlayed)
- 0-2: 60% Warming Up, 30% Steady, 10% Heating Up
- 3-7: 25% WU, 35% Steady, 25% HU, 10% Intense, 5% Brutal
- 8-14: 10% WU, 20% Steady, 30% HU, 25% Intense, 15% Brutal
- 15+: 5% WU, 15% Steady, 25% HU, 30% Intense, 25% Brutal

## Streak Multiplier

| Streak | Multiplier | Formula |
|--------|-----------|---------|
| 0-2 | 1.0x | `1.0 + floor(streak/3) * 0.5` |
| 3-5 | 1.5x | capped at 5.0x |
| 6-8 | 2.0x | |
| 9-11 | 2.5x | |
| 24+ | 5.0x (max) | |

On loss: streak resets to 0, multiplier resets to 1.0x. Accumulated stars are kept.

## Star Calculation
```
finalStars = tierBase * gameDiffMultiplier * streakMultiplier * skillMultiplier
```

Example: Pro+ player, 9-win streak, Extremely Hard Brutal board = 12 * 2.0 * 2.5 * 4 = 240 stars

## Challenge Rounds
- Trigger chance: ~10-25% after 5+ games, biased by streak
- Challenge probability: `baseChance = 0.10 + (currentStreak * 0.02)`, max 0.25
- Type weights: Blitz 35%, Shifting 35%, Memory 15%, Overgrown 15%
- Challenge rounds cost no lives (standard challenge rules)

## Navigation Flow
```
Home -> [Master Mode button] -> MasterModeScreen (hub)
  -> "Enter the Garden" -> GameScreen(-999)
    -> WIN -> MasterModeSummaryOverlay -> "Next Game" (new -999) or "End Run" (hub)
    -> LOSE -> LoseDialog -> "Retry" (new -999) or "End Run" (hub)
    -> COOLDOWN -> auto pop-back to hub
```

## Data Persistence
Separate DataStore (`master_mode`) via `MasterModeRepository`:

### All-time stats
- `total_master_stars`, `total_master_games`, `total_master_wins`
- `best_streak`, `master_challenges_completed`

### Session stats (reset on new session)
- `session_games_played`, `session_games_won`, `session_stars`
- `current_streak` (persists across sessions), `session_start`

## Firebase Structure
```
leaderboards/
  total_stars/       # existing, unchanged
  master_mode/       # new
    {uid}/
      name, emoji, score (total master stars), streak (best), ts
```

## Token Spawning
- All power-up tokens available (Shuffle, Passthrough, Unfreeze, Redo, Diagonal)
- Same ~25% spawn rate as regular levels
- No world restriction for any token type

## Files
### New
- `model/MasterModeState.kt` - MasterTier enum, MasterModeState data class
- `logic/MasterLevelGenerator.kt` - Stateless level generator
- `data/MasterModeRepository.kt` - DataStore persistence
- `ui/screens/MasterModeScreen.kt` - Hub/lobby screen
- `ui/components/MasterModeSummaryOverlay.kt` - Win overlay

### Modified
- `model/GameState.kt` - Added `masterModeState`, `masterTier`, `isMasterMode`
- `ui/navigation/Screen.kt` - Added `Screen.MasterMode`
- `ui/navigation/NavGraph.kt` - Added MasterMode route
- `ui/screens/HomeScreen.kt` - Added Master Mode button
- `viewmodel/GameViewModel.kt` - Master Mode init/win/loss/token branches
- `ui/screens/GameScreen.kt` - Tier label, overlays, loss dialog, cooldown
- `data/LeaderboardRepository.kt` - Master Mode submit/fetch
- `ui/screens/LeaderboardScreen.kt` - Master tab
- `ui/screens/StatsScreen.kt` - Master Mode stats section
- `ui/screens/SettingsScreen.kt` - Reset includes master mode data
