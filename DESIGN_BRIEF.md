# Square Garden - Design Brief

## Game Overview

**Square Garden** is a calm, colorful tile-swap puzzle game for Android. Players drag tiles on a grid to swap adjacent tiles, forming color patterns (lines, squares, shapes) to complete level goals within a limited number of moves. The tone is relaxed and botanical — think "zen garden meets candy puzzle."

Tagline: *"A calm puzzle game"*

---

## Visual Identity

### Color Palette

**Brand Colors (botanical garden theme):**
- Sage Green: `#8FBC8F` (primary)
- Dark Sage: `#5F8A5F`
- Cream: `#FFF8F0` (background)
- Warm Brown: `#8B7355` (secondary text)
- Soft White: `#FAF8F5` (card surfaces)
- Deep Forest: `#2E4A2E` (headings)
- Light Sage: `#B5D5B5`

**Tile Colors (5 vivid colors):**
| Color  | Main      | Light (highlight) | Dark (shadow) | Motif              |
|--------|-----------|--------------------|--------------|--------------------|
| Red    | `#EF5350` | `#FF8A80`          | `#C62828`    | 6-petal flower      |
| Blue   | `#42A5F5` | `#90CAF9`          | `#1565C0`    | Water droplet       |
| Yellow | `#FFCA28` | `#FFE082`          | `#F9A825`    | Sun with 8 rays     |
| Green  | `#66BB6A` | `#A5D6A7`          | `#2E7D32`    | Leaf shape          |
| Orange | `#FF9800` | `#FFCC80`          | `#E65100`    | Citrus slice        |

### Typography
- Game title: Bold + Light split ("**Square**" bold / "Garden" light), 38-42sp
- Headings: Material3 headlineMedium
- Body: Material3 bodyLarge
- UI buttons: 16-28sp, SemiBold

### Shape Language
- Rounded corners everywhere (12-28dp radius)
- Pill-shaped buttons (RoundedCornerShape 50%)
- Cards with 18-24dp rounded corners
- Soft, no sharp edges — everything feels organic

---

## App Logo / Icon

The current logo is a vector adaptive icon:
- **Foreground**: A 2x2 grid of 4 colored tiles (Red top-left, Blue top-right, Green bottom-left, Yellow bottom-right) with embossed look (shadow edges + highlight sheens). A small garden sprout with two leaves and a bud grows from the center intersection of the 4 tiles.
- **Background**: Warm cream `#E8E0D4`
- **Style**: Flat with subtle depth (embossed tile edges), clean and recognizable at small sizes

**Design direction for logo redesign/polish:**
- The 2x2 tile grid represents the core gameplay mechanic (colored squares)
- The garden sprout represents the "Garden" half of the name and the botanical theme
- Should work as both a round Android adaptive icon and a square app badge
- Needs to be legible at 48x48dp (notification) up to 512x512 (Play Store)

---

## Screens & UI Layout

### 1. Splash Screen
- Centered on cream background
- "Square" (bold, sage green) + "Garden" (light, warm brown) stacked vertically, 42sp
- Below: "A calm puzzle game" in muted text
- Fade-in + scale-up animation (0.8 -> 1.0 over 800ms)
- Auto-navigates to Home or Profile Setup after ~1.5s

### 2. Profile Setup Screen (first launch)
- Username text field
- Emoji avatar picker grid (50 options, see Avatars section)
- Year of birth selector
- Theme picker (6 themes)
- Difficulty selector (Easy / Medium / Hard)
- "Start Playing" button

### 3. Home Screen
- Top-left: "Welcome, {username}!" greeting (40sp)
- Center: "Square" / "Garden" title stack (38sp)
- Below title: Total star count in gold
- Large "Play" pill button (sage green, 220x56dp)
- "Settings" outlined pill button below

### 4. World Select Screen
- Header: "Worlds" + star count
- Scrollable column of world banner cards (120dp tall each)
- Each card has a unique **Canvas-drawn illustrated banner** as background
- Text overlay (left side): World name (20sp bold white) + level range (14sp)
- Locked worlds show star requirement in gold; disabled worlds show "Too easy" in pink
- Dark gradient scrim on left side for text readability
- "Back" pill button at bottom

### 5. Level Select Screen
- Header: World name (32sp bold, in world's theme color)
- 3x3 grid of level cards (9 levels per world), square aspect ratio, filling the page
- Each card is colored with the world's theme (light variant for unlocked, full color for last-won)
- Each card shows: level number (40sp bold), level name (16sp semibold), star rating (20sp gold stars)
- All text uses world theme colors for consistency
- Locked levels at 40% opacity
- Last-won level highlighted with bold theme border (3dp)
- "Back" pill button at bottom (themed border color)

**World Theme Colors for Level Tiles:**
| World | Tile Color | Light Variant | Text Color | Star Color |
|-------|-----------|---------------|-----------|-----------|
| 1 Seedling Garden | `#81C784` | `#A5D6A7` | `#1B5E20` | `#FFCA28` |
| 2 Blooming Meadow | `#64B5F6` | `#90CAF9` | `#0D47A1` | `#FFCA28` |
| 3 Ancient Grove | `#A1887F` | `#BCAAA4` | `#3E2723` | `#FFD54F` |
| 4 Crystal Cavern | `#4FC3F7` | `#81D4FA` | `#01579B` | `#FFE082` |
| 5 Shattered Isles | `#CE93D8` | `#E1BEE7` | `#4A148C` | `#FFD740` |
| 6 Void Fortress | `#90A4AE` | `#B0BEC5` | `#263238` | `#FFE57F` |
| 7 Molten Core | `#FF8A65` | `#FFAB91` | `#BF360C` | `#FFD600` |
| 8 Starfall Summit | `#B39DDB` | `#D1C4E9` | `#311B92` | `#FFE082` |
| 9 Abyssal Depths | `#4DB6AC` | `#80CBC4` | `#004D40` | `#FFD740` |
| 10 Prism Citadel | `#F48FB1` | `#F8BBD0` | `#880E4F` | `#FFE082` |

### 6. Game Screen (main gameplay)
- **Top bar**: "Back" pill button (left), level name (center)
- **Goal panel**: "Goals" label + list of goals with color dots, descriptions, strikethrough + "Done" when completed
- **Move counter**: "Moves: **X** / Y" with difficulty label below; turns red at 3 or fewer moves
- **Game board** (center, fills remaining space): Canvas-rendered grid of embossed tiles
- **Bottom bar**: "Hint" button (primary container) + "Restart" outlined button
- **Win overlay**: Fullscreen scrim + card with "Congratulations!", star count, pulsing star display, confetti, star trail animation, "Menu" + "Next Level" buttons
- **Lose dialog**: Modal card with "Out of Moves" in red, encouragement text, "Show Solution" button (if available), "Menu" + "Try Again" buttons
- **Solution replay**: Fullscreen overlay with animated board replay, move counter, goal tracking
- **Tutorial overlay**: Modal card with instructional text + "Got it!" button (levels 1-3 only)

---

## Game Board Visual Design

### Tile Rendering (embossed 3D look)
Each tile is a rounded rectangle with 4 layers:
1. **Shadow edge** (bottom-right): Dark variant at 55% opacity, offset slightly down-right
2. **Main body**: Vivid tile color, full rounded rect
3. **Highlight edge** (top-left): Light variant at 60% opacity, inset from top-left
4. **Inner sheen**: White at 15% opacity, covers top ~35% of tile — gives a glossy highlight

### Tile Motifs (subtle icon on each tile)
Each color has a small decorative motif drawn at 25% opacity of the dark variant:
- **Red**: 6-petal flower (center circle + 6 surrounding circles)
- **Blue**: Water droplet (teardrop path)
- **Yellow**: Sun with 8 radiating lines
- **Green**: Leaf shape (two mirrored quadratic curves) with center vein
- **Orange**: Citrus slice (circle with 6 internal segment lines + center dot)

### Frozen Tiles
- Light white frost wash (20% opacity) over the tile
- Diagonal frost crack lines (white, 55% opacity)
- Small shine highlight at top corner
- Thick ice border (3.5dp, dark blue `#0288D1` at 85% opacity)
- Base tile color remains clearly visible through the frost

### Goal Completion Borders
When tiles are part of a completed goal pattern:
- Soft green glow fill (`#43A047` at 25% opacity)
- Bold green stroke border (3dp, `#2E7D32`)

### Board Background
- Warm cream `#E8E0D4` rounded rect behind all cells
- Each cell slot has a lighter `#F5F0E8` rounded rect background

### Grid Sizes
- 5x5 (World 1, beginner)
- 6x6 (World 2)
- 7x7 (World 3)
- 8x8 (Worlds 4-8)
- 9x9 (Worlds 9-10, most challenging)

### Board Features
- **Void cells**: Empty gaps in the grid (no tile, no background) — creates irregular board shapes like diamonds, crosses, etc.
- **Frozen cells**: Tiles that count toward goals but cannot be moved — indicated by ice border overlay

---

## World Themes & Banners

Each world has a unique Canvas-drawn banner (120dp tall, full-width card). All banners use a left-side dark gradient scrim for text readability.

| # | World Name | Level Range | Grid | Banner Art Description |
|---|-----------|-------------|------|----------------------|
| 1 | Seedling Garden | 1-9 | 5x5 | Sunny sky gradient (sky blue to light green), bright sun with glow, two rolling green hills, tiny sprouts with paired leaves |
| 2 | Blooming Meadow | 10-18 | 6x6 | Light blue sky, green grass slopes, 5 colorful flowers (red/yellow/purple/orange/pink) with stems and yellow centers, small white clouds |
| 3 | Ancient Grove | 19-27 | 7x7 | Dark forest gradient (charcoal to deep green), misty green haze, 3 large trees with brown trunks and layered green canopies, hanging vines, glowing particles, dark ground |
| 4 | Crystal Cavern | 28-36 | 8x8 | Deep blue cave gradient (navy to dark blue), stalactite triangles from ceiling, 4 tall crystal shards (cyan/light blue) with white shine strips, sparkle dots, dark stone floor |
| 5 | Shattered Isles | 37-45 | 8x8 | Purple-orange sunset gradient, 3 floating islands with green grass tops, rocky brown undersides, tiny trees, scattered clouds at various opacities |
| 6 | Void Fortress | 46-54 | 8x8 | Dark stormy sky (midnight blue layers), purple energy glow at horizon, 4 fortress towers with battlements and glowing purple windows, energy beam shooting upward, floating purple particles |
| 7 | Molten Core | 55-63 | 8x8 | Dark volcanic sky gradient (dark brown to deep orange to amber), lava river at bottom (orange-red gradient), 3 volcano silhouettes with crater glow, lava bubbles, rising ember particles, smoke wisps |
| 8 | Starfall Summit | 64-72 | 8x8 | Deep space gradient (near-black to dark purple), scattered star dots (varying sizes), 2 shooting star trails (purple), 3 mountain peaks (dark purple), snow cap on tallest peak, nebula glow spots (purple/pink) |
| 9 | Abyssal Depths | 73-81 | 9x9 | Deep ocean gradient (dark navy to dark teal), angled light beam shafts from above, 3 bioluminescent jellyfish (cyan/green/purple) with glowing halos and tentacles, undulating sea floor, coral branches (orange/pink/green), rising bubbles |
| 10 | Prism Citadel | 82-90 | 9x9 | Prismatic pastel gradient (lavender to ice blue), rainbow arc (6 colored rings, 30% opacity), 4 crystal palace towers (pink/purple/indigo/cyan) with pointed tops and white shine, floating diamond-shaped prism shards, sparkle dots, light purple ground |

---

## Avatars

50 emoji avatars organized in categories. Players choose one during profile setup. Displayed as large emoji text.

**Categories:**
- **Flowers (10):** Cherry Blossom, Sunflower, Rose, Tulip, Hibiscus, Blossom, Bouquet, Rosette, Lotus, Sheaf of Rice
- **Trees & Plants (10):** Herb, Four Leaf Clover, Evergreen, Deciduous Tree, Palm Tree, Cactus, Christmas Tree, Maple Leaf, Fallen Leaf, Leaf Fluttering
- **Insects (10):** Honeybee, Butterfly, Bug, Ladybug, Ant, Beetle, Snail, Cricket, Fly, Scorpion
- **Birds (5):** Bird, Parrot, Baby Chick, Duck, Owl
- **Fruits (5):** Apple, Orange, Lemon, Grapes, Strawberry
- **Garden & Nature (5):** Potted Plant, Mushroom, Basket, Shamrock, Glowing Star
- **Weather & Sky (5):** Sun, Sun Behind Cloud, Rainbow, Snowflake, Crescent Moon

**Design note:** Currently using Unicode emoji. If creating custom avatar illustrations, maintain the botanical/nature theme. Consider a consistent art style: round/soft, slightly stylized, with warm colors and gentle outlines.

---

## Goal Types & Shapes

### Goal Types
1. **Line**: N tiles of one color in a row or column (length 3-6)
2. **Square**: 2x2 block of one color
3. **Shape**: A specific shape pattern of one color

### Shape Patterns (7 types)
All shapes can be rotated (4 rotations) and mirrored (horizontal flip), generating up to 8 orientations.

```
L_SHAPE (5 cells):        T_SHAPE (5 cells):        CROSS (5 cells):
X .                        X X X                      . X .
X .                        . X .                      X X X
X .                        . X .                      . X .
X X

Z_SHAPE (4 cells):        U_SHAPE (5 cells):
X X .                      X . X
. X X                      X X X

```

---

## Color Themes (6 available)

Players can choose a theme that changes the entire app's color scheme:

| Theme   | Background | Primary   | Mood |
|---------|-----------|-----------|------|
| Light   | Cream `#FFF8F0` | Sage `#8FBC8F` | Default botanical garden |
| Dark    | Midnight `#1A1A2E` | Teal `#7EBEA0` | Nighttime garden |
| Summer  | Warm yellow `#FFF9E6` | Coral `#FF9E5E` | Sunny and warm |
| Winter  | Ice blue `#EDF2F7` | Steel blue `#5C8DB5` | Cool and crisp |
| Fall    | Warm beige `#FDF2E9` | Terracotta `#C67C4E` | Autumn harvest |
| Spring  | Mint `#F0FFF0` | Fresh green `#66BB6A` | New growth |

---

## Sound Effects Needed

The game has a SoundPool-based audio system with these events. Currently using placeholder stubs (no actual audio files).

| Sound Event | Trigger | Suggested Style |
|------------|---------|----------------|
| `tap` | Player taps a tile to select it | Soft click — wooden or ceramic tap, gentle |
| `swap` | Two tiles swap positions | Smooth slide — soft whoosh or gentle stone-on-stone glide |
| `match` | A goal is completed | Satisfying chime — warm bell or harp arpeggio, 2-3 notes ascending |
| `win` | Level completed (all goals met) | Celebration — gentle fanfare, wind chimes + soft sparkle, scales with star count |
| `win_1star` | Win with 1 star | Short, modest chime |
| `win_2star` | Win with 2 stars | Medium, pleasant melody |
| `win_3star` | Win with 3 stars | Full, delightful jingle with sparkle |
| `lose` | Ran out of moves | Gentle disappointment — soft descending tone, not harsh or punishing |
| `star_collect` | Star animation lands on badge | Bright ping — crisp, satisfying collection sound |
| `hint` | Hint quadrant highlights | Soft notification — subtle attention-getter |
| `frozen_tap` | Player tries to move a frozen tile | Ice/crystal clink — brief, indicates "can't move this" |
| `button_click` | Any UI button press | Subtle pop — light, clean |

**Audio direction:** Calm, organic, garden-themed. Think wooden wind chimes, ceramic taps, gentle bells, soft harp. Nothing loud or jarring. Fits the "zen puzzle" aesthetic. All sounds should be short (0.3-1.5 seconds), clean, and pleasant to hear repeatedly.

---

## Animations

| Animation | Duration | Easing | Description |
|-----------|----------|--------|-------------|
| Tile swap | ~250ms | Cubic ease-out | Two tiles slide toward each other's positions simultaneously |
| Splash fade-in | 800ms | FastOutSlowIn | Title text fades from 0 to 1 opacity and scales 0.8 to 1.0 |
| Win star pulse | 600ms loop | EaseInOutCubic | Star display scales 1.0 to 1.15 and back, infinite |
| Confetti | ~3s | Custom | Colorful confetti particles fall on win |
| Star trail | ~1.5s | Custom | Stars fly from win card to badge in top-right corner |
| Solution replay | 500ms per step | Cubic ease-out | Board swaps tiles one by one with ~500ms pause between moves |
| Hint highlight | 2s | Instant on/off | Quadrant of board highlighted with yellow border |

---

## Content Summary

- **90 levels** across **10 worlds** (9 levels per world, 3x3 grid)
- **5 tile colors**: Red, Blue, Yellow, Green, Orange
- **3 goal types**: Line, Square, Shape
- **7 shape types**: L, T, Cross, Z, U (+ rotations/mirrors)
- **3 difficulty modes**: Easy (more moves), Medium (standard), Hard (fewer moves, stricter rules)
- **Star rating**: 1-3 stars per level based on moves remaining
- **50 emoji avatars**, **6 color themes**
- **Tutorial** on first 3 levels
- Boards feature **void cells** (gaps) and **frozen cells** (immovable tiles) for variety

---

## Asset Checklist

### Graphics / Illustrations
- [ ] App icon (adaptive, foreground + background, 512x512 Play Store version)
- [ ] Feature graphic (1024x500 for Play Store)
- [ ] 10 world banner illustrations (or high-res versions of the Canvas art)
- [ ] 50 custom avatar illustrations (optional — currently using emoji)
- [ ] Tutorial illustrations (optional — currently text-only)
- [ ] Promotional screenshots (phone frame mockups of key screens)

### Sound Effects (12 files, .ogg format)
- [ ] tap.ogg
- [ ] swap.ogg
- [ ] match.ogg
- [ ] win_1star.ogg
- [ ] win_2star.ogg
- [ ] win_3star.ogg
- [ ] lose.ogg
- [ ] star_collect.ogg
- [ ] hint.ogg
- [ ] frozen_tap.ogg
- [ ] button_click.ogg

### Optional
- [ ] Background music track (calm, looping, 60-90 BPM, garden/nature ambience)
- [ ] Loading/transition animation
- [ ] Achievement badge icons
