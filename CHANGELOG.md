# Changelog

All notable changes to Square Garden are documented in this file.

## [1.5.1] - 2026-05-11 (Build 9)

### New Features
- **Skill upgrade from Settings** — Players can upgrade their skill level anytime (Casual to Standard or Pro, Standard to Pro) without losing progress or unlocked worlds. Downgrade requires a full progress reset.
- **Randomized goal sets** — Each level now has 4 goal sets. Replaying a level may give different goals each time for variety. Tutorial levels (1-3) always use original goals.

### Gameplay
- **Difficulty-specific tile behaviors** — Skill levels now have distinct rules for completed goals:
  - Casual: can swap through completed goals (breaks them), tiles from completed goals count toward new goals over multiple moves.
  - Standard: swaps through completed goals are blocked, but tiles from completed goals can still count toward new goals.
  - Pro: swaps through completed goals are blocked, and completed goal tiles can only be shared when two goals complete simultaneously from one swap.
- **Casual move multiplier** — Changed from 1.5x to 1.25x for better balance.

### Bug Fixes
- Fixed win clapping sound looping indefinitely instead of playing once.
- Fixed Challenge Lab (World 11) appearing when hitting Play after completing all 90 levels — world now capped at 10.

### UI/UX
- Updated How to Play with skill upgrade info, detailed borders & tile sharing rules per skill level, and randomized goal set mention.

## [1.5.0] - 2026-05-10 (Build 8)

### New Features
- **4 Challenge Game Modes** — Special reward events triggered by exceptional play on World 5+. Challenges cost no lives and award bonus stars + one of every power-up token on completion.
  - **Blitz Garden** — 60-second time attack! Clear as many goals as possible. Combo multipliers increase every 3 goals (2x, 3x, 4x...). Trigger: 8 consecutive progressive level wins on World 5+.
  - **Overgrown Garden** — Massive 9x9 board with 8 goals, frozen tiles, and 16 moves. Get 3 tries with increasing multipliers (1x, 2x, 3x). Choose to retry (forfeit stars) or keep your score. 2x win bonus! Trigger: Complete all 9 levels in a world (once per world, World 5+).
  - **Shifting Sands** — Every 3 swaps, uncompleted tiles scramble! Stars earned are doubled (2x). Trigger: Win 5 consecutive levels without using any power-ups on World 5+.
  - **Memory Garden** — Tiles are hidden! They reveal briefly at start, then only near where you swap. Stars earned are tripled (3x). Trigger: Every perfect game on World 5+ (immediate).

### Audio
- **Win clapping/cheers sounds** — Perfect games play extended applause; regular wins play random cheers or clapping alongside celebration music.
- **Fail sound effects** — 4 sampled fail sounds played at random on game loss, replacing the procedural sad trombone.

### Gameplay
- **Improved board generation** — Complex levels (5+ goals, 8+ width) now attempt up to 300 board generation retries with larger goals placed first, greatly reducing unsolvable boards.
- **Pre-met goal detection** — Goals already formed on the initial board are now immediately detected and marked complete.

### Bug Fixes
- Fixed Overgrown Garden triggering incorrectly on already-completed worlds — now only triggers when the current win actually completes the world.
- Fixed duplicate goals appearing in challenge boards (same color + pattern type).
- Fixed screen freeze during Overgrown board generation — now shows loading indicator while generating on background thread.

## [1.4.0] - 2026-05-09 (Build 7)

### New Features
- **Celebratory music on wins** — Every win plays a random segment from the celebration track (~8 sec with smooth fade-out). Perfect games get their own dedicated segment that loops until Next Level is pressed.
- **Background intro music** — Looping music on the Home screen, respects the music toggle in Settings.
- **Sad trombone on game loss** — Procedural "wah wah wah wahhh" descending brass sound effect when a game is lost.
- **9 goal-completion sound effects** — 5 sampled congratulatory clips plus 4 new procedural celebration patterns (chime cascade, horn stab, sparkle arpeggio, tubular bell), played at random on each goal completion.
- **5 scramble sound effects** — Distinct procedural audio patterns (digital glitch, modem stutter, warped vinyl, buzz saw, data corruption) played at random during board scramble.

### Audio
- **Brass celebration fanfares** — Win sounds replaced with rich procedural brass section synthesis (6.5s–11s) with multiple voices, timpani, and cymbal crashes.
- **Music toggle respected everywhere** — MusicManager now observes the music enabled setting globally; toggling music off immediately stops all playback (intro and win music).
- **Pre-computed celebration audio** — Heavy brass synthesis now runs on a background thread at startup, eliminating the ~10 second delay before the win overlay appeared.

### UI/UX
- **Celebration overlays rain down** — Confetti, balloons, and stars now fall from the top of the screen in the foreground (rendered above the win overlay), with doubled sizes and mixed scale variation.
- **Solid black grid on completed goals** — Completed goal cells now display a solid black border and grid lines for clear visibility across all themes.

### Bug Fixes
- Fixed Show Solution not completing all moves — solver now uses the full original move budget instead of the difficulty-adjusted count.
- Fixed background music continuing to play when music toggle was turned off on the Home screen.

## [1.3.1] - 2026-05-08 (Build 6)

### New Features
- **Token tiles for all power-ups** — Shuffle, Passthrough, and Unfreeze token tiles now appear randomly on World 4+ boards (~25% chance each, independently). Capture them by completing goals that contain them. A single tile can hold multiple token types.
- **Leaderboard score sync** — Tapping the Leaderboards button now submits your current total stars and highest level, so existing progress appears on the board immediately.

### UI/UX
- **White dotted border on completed goals** — Completed goal cells now show a white dotted outline on top of the green/cyan border, making them clearly distinguishable from frozen tiles.
- **Scrollable win overlay** — Win celebration screen with many token awards (e.g. perfect game) is now scrollable so buttons are always reachable on mobile phones.
- **Header text no longer hidden by PlayerBadge** — Added padding on HomeScreen greeting and LevelSelectScreen world title to prevent overlap with the top-right PlayerBadge on mobile.

### Bug Fixes
- Fixed Google Play Games leaderboard not opening (switched to `startActivityForResult` for the leaderboard intent)
- Fixed leaderboard sign-in requiring a second tap to open (now auto-opens after sign-in)

## [1.3.0] - 2026-05-08 (Build 5)

### New Features
- **Redo tile power-up** — Special tiles appear randomly on World 4+ boards (~25% chance). Capture one as part of a completed goal to earn a redo token. Redo tokens let you restart a level without penalty.
- **Perfect game award** — Complete all goals in the same number of moves as goals (World 5+) to earn 2x stars and +1 of every token (shuffle, passthrough, unfreeze, redo).
- **Leaderboard opt-in toggle** — Choose whether to submit scores to Google Play Games leaderboards (default off). Toggle in Profile screen.
- **Visual goal grid** — Goals displayed as a graphical grid of mini embossed tile shapes instead of text descriptions. Grid layout adapts to 2-7 goals — fewer goals means bigger shapes.
- **Bas-relief avatars** — Cartoony, sculpted avatar display with beveled medallion, specular highlights, and subtle breathing animation.
- **Passthrough through frozen tiles** — Passthrough power-up now jumps over frozen tiles as well as completed goal cells.

### UI/UX
- **Vivid beveled tiles** — Richer, more saturated tile colors with stronger 3D embossing (thicker bevel, drop shadow, specular highlight).
- **Horizontal PlayerBadge** — All player info (level, stars, lives, trophies) in a single compact row instead of a vertical stack.
- **Redo button visibility** — Redo button now uses themed secondary container colors, visible in all themes including dark mode.
- **Dark theme difficulty labels** — All difficulty labels (Easy through Extremely Hard) now use lighter colors in dark themes for readability.
- **Perfect game count** — Trophy icon with count displayed in PlayerBadge and HomeScreen star chip.
- **Back button** on How to Play page.

### Bug Fixes
- Fixed redo not resetting moves to full amount when using a redo token
- Fixed cooldown bypass — players can no longer access game screens during the 5-minute cooldown by minimizing the timer overlay
- Removed duplicate avatar on HomeScreen that was hidden behind the PlayerBadge

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
