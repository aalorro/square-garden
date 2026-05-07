# Changelog

All notable changes to Square Garden are documented in this file.

## [1.2.0] - 2026-05-07 (Build 4)

### New Features
- **Shuffle power-up** — Randomize the board mid-game. Frozen tiles and completed goals stay in place. Earned when unlocking a new world.
- **Passthrough power-up** — Next swap through a goal border won't break it. Visual cyan shield on protected borders. Earned every 7 levels completed.
- **Unfreeze power-up** — Tap a frozen tile to thaw it. Earned every 5 consecutive wins on World 3+.
- **Minimizable cooldown overlay** — "No lives left" timer can be minimized to a floating chip so you can watch solution replays.
- **Dynamic win messages** — Randomized congratulatory headlines and star-tier subtitles on level completion.
- **Token award celebrations** — Animated cards with spring bounce-in when earning power-up tokens.
- **Favorite levels** — Star marker on levels for easy replay access, visible in-game and on level select.
- **Google Play Games leaderboards** — 36 leaderboards (Total Stars, Highest Level, Per-World Stars) separated by skill level (Casual/Standard/Pro).

### Gameplay Changes
- Redesigned levels 55-90 (Worlds 7-10) for improved solvability — removed impossible patterns (LINE 7 on 8x8, excessive CROSSes), minimum Pro MPG ~2.4
- Passthrough only activatable when completed goals exist on the board
- Audio feedback when passthrough token is consumed
- Pro difficulty thresholds tightened for more meaningful star multipliers
- World unlock thresholds lowered to align with natural progression

### UI/UX
- Compact GoalPanel on phones and 7" tablets — smaller fonts, tighter spacing for more board space
- Full-width game board on phones and 7" tablets for easier tile interaction
- Compact PlayerBadge on phones — smaller avatar and reduced font sizes
- Bottom action bar with 5 circular buttons: Hint, Shuffle, Passthrough, Unfreeze, Redo
- Token count badges on power-up buttons with active state highlighting

### Bug Fixes
- Fixed gesture handler silently dropping taps when state changed during touch (pointerInput key stability)
- Fixed levels with unsolvable patterns on 8x8 and 9x9 grids
- Fixed star display crash when difficulty multipliers pushed stars above 3

## [1.1.0] - 2026-05-06 (Build 3)

### New Features
- Real-time game difficulty rating (Easy / Medium / Hard / Very Hard / Extremely Hard) based on board randomization, move pressure, goal complexity, and board constraints
- Star multiplier per game difficulty: Easy 0.75x, Medium 1x, Hard 1.25x, Very Hard 1.5x, Extremely Hard 2x
- Shapes explainer animation screen after first profile creation — demos all 7 goal patterns with animated tile pop-in
- Life restored celebration splash with confetti and fanfare music
- "Don't show again" checkbox for shapes explainer

### Gameplay Changes
- Skill level (Casual / Standard / Pro) is now locked after profile creation — can only change by resetting progress
- Worlds below starting skill level are inaccessible (Pro can't access Worlds 1-2, Standard can't access World 1)
- World unlock thresholds scale by skill: Casual 1x, Standard 2x, Pro 3x
- Life restoration requires 3 consecutive wins within 5 levels of highest completed level
- Removed in-game "wins to restore life" notification bubble — rule explained in How to Play

### UI/UX
- Difficulty rating displayed with color coding in GoalPanel (green/blue/orange/red/purple)
- Skill picker shows as read-only when editing profile, with note to reset in Settings
- Reset progress now navigates to profile setup to re-choose skill level
- Renamed "Difficulty" setting to "Skill" (Casual / Standard / Pro)
- Star count-up animation capped at 5 seconds max
- Removed "Menu" text from game page back arrow
- Reduced level name font size for mobile screens

## [1.0.2] - 2026-05-05 (Build 2)

### Improvements
- Skill-based starting worlds — experienced players skip easy content
- Progressive difficulty ramp in World 1 and World 2 levels
- Orange color restricted to World 5+ (no longer appears in early worlds)
- Goal completion shown as green checkmark next to struck-through text
- GoalPanel left-justified for natural reading
- Theme picker uses 3x2 grid layout for mobile readability
- "Try Again" / "Menu" buttons sized to prevent text wrapping
- Level names displayed under stars in world page
- Responsive layout fixes for mobile screens
- "How to Play" screen includes tips on earning lives back
- Board generation restricted to level-appropriate colors only

## [1.0.0] - 2026-05-05 (First Google Play Release)

### Features
- 90 handcrafted levels across 10 beautifully themed worlds
- 5 tile colors: Red, Blue, Yellow, Green, and Orange (introduced in World 5)
- 3 goal types: Line, Square, Shape (L/T/Cross)
- 3 skill levels: Casual (more moves, start World 1), Standard (balanced, start World 2), Pro (fewer moves, start World 3)
- Skill-based starting worlds — experienced players skip easy content
- Life system with skill-gated recovery and win streak bonuses
- Drag-to-swap gameplay with smooth animations
- Embossed tile rendering with shadow/highlight/sheen layers and unique motifs
- Hint system highlighting best-move quadrant
- Star trail animations and confetti celebrations on wins
- 6 color themes: Light, Dark, Summer, Winter, Fall, Spring
- User profiles with emoji avatars, username customization (15 char alphanumeric limit)
- World unlock system based on cumulative stars
- Frozen tiles (World 4+) and void cells (World 5+) for strategic depth
- Tutorial on first 3 levels
- "How to Play" screen with tips on earning lives back
- DataStore-based progress persistence
- No ads, no in-app purchases

### Level Design
- World 1: Progressive difficulty — levels ramp from single goals to multi-goal challenges with increasing line lengths
- World 2: Larger 6x6 boards with shapes, squares, and 3-4 goals per level
- World 3: Complex 7x7 boards with 3-4 goals including crosses
- World 4: Frozen tiles, voids, L/T/Cross shapes
- World 5: Orange color debut, void cells create irregular boards
- Worlds 5-10: Increasing complexity with combined mechanics

### UI/UX
- Goal completion shown as green checkmark next to struck-through text
- Level names displayed under stars in world page
- Theme picker uses 3x2 grid layout for mobile readability
- Responsive layouts tested on mobile, 7" tablet, and 10" tablet
- "Try Again" / "Menu" buttons sized to prevent text wrapping
- GoalPanel left-justified for natural reading
- Board generation restricted to level-appropriate colors only
