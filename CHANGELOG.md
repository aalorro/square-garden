# Changelog

All notable changes to Square Garden are documented in this file.

## [1.9.1] - 2026-06-12 (Build 25)

### New Features
- **Undo power-up** — New token that reverts the last swap, restoring board state, moves, and completed goals. Available for Pro, Pro+, and Master Mode players on World 6+. Spawns at ~12.5% probability (half of other tokens). Awarded on perfect games, challenge wins, and Master Mode challenge wins.

### Fixes
- **Avatar upload on phones** — Fixed crop dialog buttons being cut off on mobile phones.
- **Leaderboard avatar** — Custom uploaded avatars now display correctly in the leaderboard.
- **Username wrapping** — Reduced welcome screen username font on phones to prevent 15-character names from wrapping.

## [1.9.0] - 2026-06-11 (Build 24)

### New Features
- **Challenge info popup** — An info icon next to "CHALLENGE ROUND" opens a popup showing the challenge name and description, dismissable via tap-outside or close button.
- **Custom Master Mode win music** — Master Mode wins now play randomized clips from a dedicated victory melody with smooth fade-outs.
- **Custom game complete music** — Beating level 126 now plays a unique looping pixel melody instead of the generic celebration track.

## [1.8.5] - 2026-06-10 (Build 23)

### New Features
- **4 Master Mode-exclusive challenge types** — Frozen Wave (tiles freeze every 2 moves), Rotation Garden (board rotates 90° every 3 moves), Mirror Garden (every swap is mirrored), and Decay Garden (completed goals expire after 5 moves). These challenge rounds trigger during Master Mode runs.
- **In-app update detection** — The app now checks Google Play for available updates and prompts with a non-blocking download. A restart Snackbar appears when the download completes.

### Fixes
- **Master Mode challenge navigation** — "Next Game" after a Master Mode challenge round now correctly returns to Master Mode instead of navigating to the regular world.
- **Master Mode challenge loss** — Losing a Master Mode challenge now returns to the Master Mode hub instead of the Home screen.

## [1.8.4] - 2026-06-08 (Build 22)

### New Features
- **Save & restore game state** — Game progress is now saved after every swap. Quitting mid-game (force-close, task switch, or "Clear All") resumes the exact same board on relaunch — no more restarting fresh to dodge a life loss.

### Improvements
- **Diagonal swap detection** — Fixed unreliable diagonal swipes where cardinal moves fired instead. An angle-based intent guard now waits for clear diagonal input before committing.
- **X and Y shapes in Master Mode** — Master Mode goal generation now includes X_SHAPE and Y_SHAPE patterns alongside the existing hard shapes.
- **Master Mode tier progression** — After 25+ games, Warming Up and Steady tiers are removed; the easiest tier becomes Heating Up.
- **Menu button on win/loss overlays** — Both regular world and Master Mode win cards now show a Menu button alongside Next Game. Loss dialogs also have a working Menu button.
- **Tablet font tuning** — Level select tile fonts on 10"+ tablets reduced for better readability.

### Fixes
- **End Run button in Master Mode** — Fixed "End Run" not working after a win or loss (back stack was empty after saved game restore).
- **Menu button navigation** — Fixed Menu button on win card navigating to the next level instead of the Home screen.
- **Loss dialog Menu button** — Fixed Menu button on out-of-moves and challenge loss dialogs not navigating anywhere.
- **Master Mode leaderboard** — Fixed leaderboard submission path and cancellation handling.
- **PlayerBadge dropdown** — Home and Master Mode menu items now accessible during active gameplay.

## [1.8.3] - 2026-06-07 (Build 21)

### New Features
- **Master Mode** — Infinite roguelike puzzle mode unlocked after beating all 126 levels. Five difficulty tiers (Warming Up to Brutal) with streak multipliers up to 5x. Separate leaderboard and persistent stats.

### Improvements
- **Guaranteed token spawns** — Worlds 11–14 now always spawn at least one of each power-up token type on the board.
- **Reduced goals on levels 124–126** — Endgame levels are challenging but no longer overwhelming.

### Fixes
- **Board generation stalling** — Fixed consecutive games (especially on 9×9 boards in Worlds 9–10) causing the app to hang. The solver and board generator now properly cancel when navigating away, preventing thread pool exhaustion.
- **Show Solution timeout** — The "Show Solution" feature now has a 10-second timeout and adaptive beam width, preventing indefinite hangs on complex boards.
- **Badge overlap** — Fixed mastery badge overlapping avatar on certain screen sizes.

## [1.8.2] - 2026-06-04 (Build 20)

### Improvements
- **World unlock thresholds rebalanced** — Casual uses steeper flat thresholds for smoother progression; Standard and Pro keep legacy scaling. Pro+ thresholds now continue the Pro curve (W11=850, W12=990, W13=1140, W14=1300).
- **"You are here" indicator** — World Select screen shows a green badge on the world containing your next unplayed level.
- **Encouraging leaderboard messages** — Replaced generic "not in top 50" text with 8 randomized encouraging messages, shown only on your own skill tab.

### Fixes
- **Redo power-up performance** — Fixed device slowdown after repeated Redo usage. Solver and board generation coroutines now cancel properly instead of stacking, preventing CPU saturation on large boards.

## [1.8.1] - 2026-06-03 (Build 19)

### Fixes
- **Leaderboard cross-device sync** — Other players' scores now appear correctly. Fixed Firebase persistence caching stale data instead of fetching from the server.
- **Leaderboard auto-refresh** — Scores refresh automatically when entering the leaderboard screen, no manual retry needed.
- **Leaderboard auto-retry** — If the first fetch fails (connection warming up), retries once automatically before showing an error.
- **Leaderboard chip labels** — "Casual" and "Standard" filter chips no longer wrap text on mobile phones.
- **Level select card sizing** — Restored original font sizes for phones and 8" tablets; reduced sizes apply only to 10"+ tablets to prevent label overflow.

## [1.8.0] - 2026-06-03 (Build 18)

### New Features
- **Global leaderboards** — Opt in from your Profile to share scores on global leaderboards powered by Firebase. See your rank among players of the same skill level. Zero sign-in friction with anonymous authentication.
- **Solver-based difficulty labels** — Game difficulty is now calibrated by the solver's optimal solution length instead of heuristics. Labels accurately reflect how many moves are actually needed.

### Improvements
- **Smarter solutions (iterative deepening)** — The solver now tries the shortest possible solution first, producing much more realistic move sequences.
- **Two-phase solver** — A quick wide-beam probe catches easy/medium boards instantly; the full solver handles harder boards.
- **World unlock thresholds rebalanced** — Strong players no longer unlock worlds 2+ ahead of their current progress. Thresholds scale progressively through later worlds.

### Fixes
- **Profile reset** — Resetting progress now fully clears difficulty, starting level, and all profile state.
- **Pro+ world visibility** — Non-Pro+ players no longer see worlds 11–14 in the world menu.

## [1.7.3] - 2026-06-02 (Build 17)

### New Features
- **Haptic feedback** — Tactile vibration on goal completion, power-up token capture, and winning a level for a more satisfying feel.
- **Expanded congratulatory messages** — More variety in win celebration text.

### Improvements
- **Smarter solutions (pair-removal)** — HintSolver now removes redundant move pairs (swap-and-swap-back patterns) for even cleaner solutions.
- **Lose dialog button sizing** — Reduced button font size on phones to prevent text wrapping.

## [1.7.2] - 2026-05-31 (Build 16)

### New Features
- **Pro+ upgrade celebration** — Completing level 90 as a Pro player triggers a dramatic celebration overlay with confetti, balloons, music, and stats summary. Choose to upgrade to Pro+ or stay in Pro.
- **Post-90 replay mode** — Pro players who decline the Pro+ upgrade get randomized games from World 5–10 (levels 37–90) instead of sequential progression.
- **Redo confirmation dialog** — Using the Redo power-up now asks "Are you sure?" before refreshing the board.

### Improvements
- **"Next Game" button** — Win overlay and life restored splash now consistently say "Next Game" instead of "Next Level".
- **Life restored splash** — Always shows both "Menu" and "Next Game" buttons.

## [1.7.1] - 2026-05-30 (Build 15)

### Gameplay
- **All Pro-level games now have 4 goals** — eliminated all 3-goal levels from World 3+ for a more engaging Pro experience.
- **Level 19 rebalanced** — Pro players start World 3 with a proper 4-goal warmup (Line/Square mix) rated Easy instead of a boring 3-goal Hard.
- **Play button goes directly to next level** — tapping Play on the Home screen launches your next unplayed level instead of the world menu.

### Improvements
- **Smarter solutions** — HintSolver now compresses solutions by removing redundant moves, producing cleaner and shorter solutions.
- **Level number on game board** — level name shows the number in parentheses (auto-hidden on phones if it would overflow).
- **"Up Next" label on LevelSelect** — the next unplayed level card is clearly marked.
- **Tablet-optimized LevelSelect cards** — level number, stars, game name, and "Up Next" label scale up for tablet screens.

## [1.7.0] - 2026-05-29 (Build 14)

### New Features
- **Game Complete celebration** — Beating level 126 triggers a dramatic full-screen overlay with looping music, continuous confetti/balloons/stars, stats summary, and a "Save Mastery Badge" option. Music loops until the player presses a button.
- **Mastery badge** — Completing the game earns a golden ring with crown motif on the player's avatar, visible everywhere (Home, Profile, PlayerBadge).
- **Exportable mastery badge** — A print-quality 2048x2048 PNG certificate with the player's name, difficulty, stars, and completion date. Saveable to gallery and shareable.
- **Lifetime Stats screen** — Accessible from Home (after first game played). Shows total stars, games played, perfect games, total swaps, tokens used, challenges completed, and difficulty completions.
- **Favorites modal** — A dialog on the Home screen listing all favorited levels with level number, name, world, and star rating. Tap to play, unfavorite inline.
- **Cascading token trail effect** — Won tokens animate in a staggered cascade on the win screen.

### Gameplay
- **Back button blocked mid-game** — Android back button is disabled during active gameplay to prevent accidental exits.
- **Extended cooldown** — Rest period extended from 5 minutes to 20 minutes.

### Changes
- **Leaderboards disabled** — Google Play Games leaderboards temporarily hidden pending a better provider.

## [1.6.0] - 2026-05-28 (Build 13)

### Gameplay
- **Smart Shuffle** — The Shuffle power-up now rearranges remaining tiles with a mild bias toward progress, giving stuck players a second chance without handing them a gimme (~20% win rate after shuffle).
- **Token capture sound** — Capturing a power-up token (Shuffle, Passthrough, Unfreeze, Redo, Diagonal) now plays a chime sound effect.
- **Show Solution reliability** — The "Show Solution" button now works consistently, including after using the Shuffle power-up. Wider beam search (200) for better solution coverage.

### Fixes
- **Board generation hangs** — Added 3-second deadline to all board generation paths. Replaced linear goal placement with recursive backtracking. Boards that can't be generated in time gracefully fall back to random placement with async solving.
- **Pro+ unsolvable boards** — Fixed a bug where multiple same-color goals could overlap cells, requiring more tiles than existed on the board (e.g. 3 purple goals needing 15 tiles with only 12 on board).
- **Overgrown retry** — Fixed diagonal tokens vanishing after retrying an Overgrown challenge. Fixed retry using stale goals from the previous board instead of the newly generated level.
- **Show Solution after shuffle** — Fixed the button appearing clickable but doing nothing after a shuffle, caused by the precomputed solution being cleared.
- **Win button label** — The congratulatory splash button now correctly says "Back to Game" since it navigates to the next level.

## [1.5.4] - 2026-05-28 (Build 12)

### New Features
- **Pro+ skill tier** — A fourth skill level above Pro: 0.65× moves, 4× stars, starts at World 11 (level 91). Upgrade from Settings as with Pro.
- **4 new Pro+-exclusive worlds (Worlds 11–14, levels 91–126)** — Nebula Verge, Quantum Lattice, Singularity Spire, and Infinity Prism. Locked to Pro+ skill.
- **5th power-up: Diagonal Movement** (Pro+ / World 11+) — One-shot swap with any of the 8 neighbours (orthogonal or diagonal), skipping frozen tiles. Spawns on World 11+ boards (~25%) and is awarded with the other tokens on a Perfect Game on World 11+.
- **Two new shape goals** — X-shape and Y-shape, plus a new Violet tile colour for Pro+ play.

### Gameplay
- **Completed goals stay put** — A swap that accidentally re-forms the same goal pattern elsewhere no longer shifts the highlighted cells. Already-completed goals are locked in place.
- **Better "Back to Game" navigation** — After a challenge round (Blitz, Overgrown, Shifting, Memory) the "Back to Game" button now lands you on the next level past your highest completed level — your actual progression frontier — instead of dropping you at the menu.

### UI
- **Compact icon-only win celebration** — Awarded power-up tokens on the win splash are now shown as a single row of icon chips. Tap a chip to reveal its label; tap again or elsewhere to dismiss.
- **Compact mid-game capture popups** — Same icon-chip treatment when you capture a power-up tile during play.
- **Corner-to-corner X overlay on completed-goal tiles** — A black X across each completed tile (in addition to the border) makes completed goals much easier to read on 8×8 and 9×9 boards.

### Fixes
- **Play button** now respects Pro+ worlds 11–14 instead of capping at World 10.
- **Diagonal swap drag** — The board's drag handler now emits diagonal targets when Diagonal Movement is armed (previously only orthogonal drags reached the engine).
- **Pro+ level difficulty tuning** — Per-world line-length floors and tougher Pro+ goal randomization so Pro+ levels actually exceed World 10 in difficulty.
- **Tutorial replay** — Pro+ hint legality and one-shot tutorials repaired.
- **Previous-world navigation** clamped to the player's visibility floor.
- **Hidden placeholder board** during level load; deepened red tile colour for better contrast.

## [1.5.3] - 2026-05-26 (Build 11)

### Audio
- **New welcome music** — Replaced the Home screen intro with a new "Parade" loop, now using gapless `setNextMediaPlayer` chaining (two MediaPlayer instances alternating) for a truly seamless loop with no audible cut between iterations.
- **Randomized huge-win celebratory music** — Perfect-game wins and challenge completions now play a randomly chosen celebratory clip selected from the most climactic sections of two new tracks (Puzzle and Bitcrush), each with a clean fade-out. Clapping/cheers sound effects are unchanged.

## [1.5.2] - 2026-05-12 (Build 10)

### New Features
- **Collapsible Player Badge** — Swipe the avatar left/right to collapse or expand the badge. Tap the avatar for the Settings/Exit menu.
- **Next World button** — A "Next World" button at the bottom of each level select screen lets you jump directly to the next world without going back to the world menu.
- **Leaderboards on Home screen** — Moved Leaderboards button from Settings to the Welcome page for quicker access.

### Gameplay
- **Tutorial levels now have 2 goals** — Levels 1 and 2 each have 2 goals instead of 1, introducing multi-goal play from the start.
- **Casual minimum 3 goals** — After tutorials, Casual players always get at least 3 goals per level for a richer experience.

### Improvements
- **Leaderboard refresh** — Scores are now submitted immediately to Google Play servers (using `submitScoreImmediate`) before opening the leaderboard, ensuring you see the latest data.
- **Memory challenge reveal** — In Memory challenge, the completed board is fully revealed for 3 seconds before the win celebration plays.
- **Solution replay hand indicator** — A hand emoji follows the tile during animated solution replays, with a simple beep on goal completion and clapping at the end.
- **Board generation loading** — A loading indicator now shows when any board takes time to generate, not just Overgrown challenge boards.
- **Extended win clapping** — Clapping sound plays twice for a fuller celebration.
- **Completed goal borders** — Changed from white dotted outlines to solid black borders for clearer visibility.

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
