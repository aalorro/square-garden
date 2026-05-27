"""
Generate 36 hand-crafted levels for Pro+ worlds 11-14 (levels 91-126).
Appends to app/src/main/res/raw/levels.json before the closing `]`.

Levels feature VIOLET tiles and X_SHAPE / Y_SHAPE goals (Pro+ exclusive).

Run from project root:
    python scripts/gen_pro_plus_levels.py
"""
import json
import random
from pathlib import Path

random.seed(20260527)  # deterministic

LEVELS_PATH = Path("app/src/main/res/raw/levels.json")

COLORS_5 = ["RED", "BLUE", "GREEN", "YELLOW", "ORANGE"]
COLORS_6 = ["RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "VIOLET"]


def mk_tiles(w, h, palette, violet_count=None):
    """Random tiles biased to include the palette. If violet_count is set,
    constrain VIOLET appearances."""
    grid = [[random.choice(palette) for _ in range(w)] for _ in range(h)]
    if violet_count is not None and "VIOLET" in palette:
        # Force exactly violet_count VIOLET tiles
        all_cells = [(r, c) for r in range(h) for c in range(w)]
        random.shuffle(all_cells)
        # First remove any existing VIOLET (replace with random other color)
        other = [c for c in palette if c != "VIOLET"]
        for r in range(h):
            for c in range(w):
                if grid[r][c] == "VIOLET":
                    grid[r][c] = random.choice(other)
        # Then place violet_count VIOLETs
        for (r, c) in all_cells[:violet_count]:
            grid[r][c] = "VIOLET"
    return grid


def line(color, length):
    return {"type": "line", "color": color, "length": length}


def square(color):
    return {"type": "square", "color": color}


def shape(color, sh):
    return {"type": "shape", "color": color, "shape": sh}


def stars(twoStar, threeStar):
    return {"twoStar": twoStar, "threeStar": threeStar}


def level(id, world, name, w, h, maxMoves, tiles, goals, twoStar, threeStar,
          tutorial=None, frozen=None, voids=None):
    obj = {
        "id": id,
        "world": world,
        "name": name,
        "width": w,
        "height": h,
        "maxMoves": maxMoves,
        "tiles": tiles,
        "goals": goals,
        "stars": stars(twoStar, threeStar),
    }
    if tutorial:
        obj["tutorial"] = tutorial
    if frozen:
        obj["frozen"] = frozen
    if voids:
        obj["voids"] = voids
    return obj


def cell(r, c):
    return {"row": r, "col": c}


# ─────────────────────────────────────────────────────────────
# WORLD 11 — Nebula Verge (7x7, 5 colors + sparse VIOLET, intro X_SHAPE)
# ─────────────────────────────────────────────────────────────

W11 = []

# 91 — Tutorial: introduce VIOLET + X_SHAPE
W11.append(level(
    91, 11, "Stardust Awakens", 7, 7, 14,
    mk_tiles(7, 7, COLORS_6, violet_count=8),
    [
        line("VIOLET", 3),
        shape("RED", "X_SHAPE"),
        line("BLUE", 4),
    ],
    twoStar=4, threeStar=8,
    tutorial=[
        {"message": "Welcome to Pro+!\nYou have unlocked four cosmic worlds beyond Pro."},
        {"message": "Meet the VIOLET tile — a new color found only in these worlds."},
        {"message": "The X-shape is a diagonal cross of 5 tiles.\nForm one of any color to complete the goal."},
    ],
))

# 92 — Constellation lines
W11.append(level(
    92, 11, "Constellation Lines", 7, 7, 13,
    mk_tiles(7, 7, COLORS_6, violet_count=7),
    [
        line("VIOLET", 4),
        line("GREEN", 4),
        square("YELLOW"),
    ],
    twoStar=4, threeStar=7,
))

# 93 — First X
W11.append(level(
    93, 11, "Crossed Stars", 7, 7, 14,
    mk_tiles(7, 7, COLORS_6, violet_count=9),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("ORANGE", "L_SHAPE"),
        line("BLUE", 3),
    ],
    twoStar=4, threeStar=8,
))

# 94 — Mixed nebula
W11.append(level(
    94, 11, "Nebula Bloom", 7, 7, 15,
    mk_tiles(7, 7, COLORS_6, violet_count=8),
    [
        square("VIOLET"),
        shape("RED", "T_SHAPE"),
        line("GREEN", 5),
    ],
    twoStar=5, threeStar=9,
))

# 95 — Multi-shape
W11.append(level(
    95, 11, "Twin Crosses", 7, 7, 16,
    mk_tiles(7, 7, COLORS_6, violet_count=10),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("BLUE", "CROSS"),
        square("ORANGE"),
        line("YELLOW", 4),
    ],
    twoStar=5, threeStar=10,
))

# 96 — Dense X
W11.append(level(
    96, 11, "Galactic Drift", 7, 7, 16,
    mk_tiles(7, 7, COLORS_6, violet_count=9),
    [
        shape("VIOLET", "U_SHAPE"),
        shape("RED", "X_SHAPE"),
        line("GREEN", 5),
    ],
    twoStar=5, threeStar=10,
))

# 97 — Frozen seeds
W11.append(level(
    97, 11, "Frozen Stardust", 7, 7, 16,
    mk_tiles(7, 7, COLORS_6, violet_count=8),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("YELLOW", "Z_SHAPE"),
        square("BLUE"),
    ],
    twoStar=5, threeStar=10,
    frozen=[cell(2, 2), cell(4, 4)],
))

# 98 — Complex
W11.append(level(
    98, 11, "Pulsar Symphony", 7, 7, 17,
    mk_tiles(7, 7, COLORS_6, violet_count=10),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("ORANGE", "CROSS"),
        shape("RED", "L_SHAPE"),
        line("BLUE", 4),
    ],
    twoStar=6, threeStar=11,
    frozen=[cell(1, 5), cell(5, 1)],
))

# 99 — Marquee
W11.append(level(
    99, 11, "Nebula Verge", 7, 7, 18,
    mk_tiles(7, 7, COLORS_6, violet_count=11),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("GREEN", "U_SHAPE"),
        shape("RED", "CROSS"),
        line("YELLOW", 5),
        square("BLUE"),
    ],
    twoStar=7, threeStar=12,
    frozen=[cell(0, 3), cell(3, 0), cell(3, 6), cell(6, 3)],
))


# ─────────────────────────────────────────────────────────────
# WORLD 12 — Quantum Lattice (8x8, 6 colors, intro Y_SHAPE)
# ─────────────────────────────────────────────────────────────

W12 = []

# 100 — Y_SHAPE tutorial
W12.append(level(
    100, 12, "Quantum Spark", 8, 8, 17,
    mk_tiles(8, 8, COLORS_6, violet_count=10),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("RED", "X_SHAPE"),
        line("BLUE", 4),
    ],
    twoStar=5, threeStar=10,
    tutorial=[
        {"message": "The Y-shape is a vertical stem with two arms reaching to the top."},
        {"message": "5 tiles of one color form the Y.\nLook for natural Y patterns on the board."},
    ],
))

# 101
W12.append(level(
    101, 12, "Entangled Threads", 8, 8, 16,
    mk_tiles(8, 8, COLORS_6, violet_count=9),
    [
        shape("VIOLET", "Y_SHAPE"),
        line("ORANGE", 5),
        square("GREEN"),
    ],
    twoStar=5, threeStar=10,
))

# 102
W12.append(level(
    102, 12, "Probability Wave", 8, 8, 17,
    mk_tiles(8, 8, COLORS_6, violet_count=10),
    [
        shape("YELLOW", "Y_SHAPE"),
        shape("VIOLET", "X_SHAPE"),
        line("RED", 5),
    ],
    twoStar=6, threeStar=11,
))

# 103
W12.append(level(
    103, 12, "Photon Net", 8, 8, 18,
    mk_tiles(8, 8, COLORS_6, violet_count=11),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("BLUE", "U_SHAPE"),
        square("RED"),
        line("GREEN", 4),
    ],
    twoStar=6, threeStar=12,
))

# 104
W12.append(level(
    104, 12, "Superposition", 8, 8, 18,
    mk_tiles(8, 8, COLORS_6, violet_count=10),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("ORANGE", "Y_SHAPE"),
        shape("BLUE", "T_SHAPE"),
        line("RED", 5),
    ],
    twoStar=6, threeStar=12,
    frozen=[cell(3, 3), cell(4, 4)],
))

# 105
W12.append(level(
    105, 12, "Bose Field", 8, 8, 19,
    mk_tiles(8, 8, COLORS_6, violet_count=12),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("RED", "CROSS"),
        line("GREEN", 6),
        square("YELLOW"),
    ],
    twoStar=7, threeStar=13,
))

# 106
W12.append(level(
    106, 12, "Lattice Knot", 8, 8, 19,
    mk_tiles(8, 8, COLORS_6, violet_count=11),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("GREEN", "Y_SHAPE"),
        shape("ORANGE", "Z_SHAPE"),
        line("BLUE", 5),
    ],
    twoStar=7, threeStar=13,
    frozen=[cell(1, 6), cell(6, 1)],
))

# 107
W12.append(level(
    107, 12, "Decoherent Dawn", 8, 8, 20,
    mk_tiles(8, 8, COLORS_6, violet_count=12),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("RED", "X_SHAPE"),
        shape("YELLOW", "U_SHAPE"),
        line("BLUE", 6),
    ],
    twoStar=7, threeStar=14,
    frozen=[cell(2, 5), cell(5, 2), cell(3, 3)],
))

# 108 — Marquee
W12.append(level(
    108, 12, "Quantum Lattice", 8, 8, 21,
    mk_tiles(8, 8, COLORS_6, violet_count=14),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("VIOLET", "X_SHAPE"),
        shape("RED", "CROSS"),
        shape("GREEN", "U_SHAPE"),
        line("BLUE", 6),
        square("ORANGE"),
    ],
    twoStar=8, threeStar=15,
    frozen=[cell(0, 4), cell(4, 0), cell(4, 7), cell(7, 4)],
))


# ─────────────────────────────────────────────────────────────
# WORLD 13 — Singularity Spire (8x8, all shapes mixed)
# ─────────────────────────────────────────────────────────────

W13 = []

# 109
W13.append(level(
    109, 13, "Event Horizon", 8, 8, 18,
    mk_tiles(8, 8, COLORS_6, violet_count=11),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("RED", "X_SHAPE"),
        line("ORANGE", 5),
        square("BLUE"),
    ],
    twoStar=6, threeStar=12,
))

# 110
W13.append(level(
    110, 13, "Gravity Well", 8, 8, 19,
    mk_tiles(8, 8, COLORS_6, violet_count=12),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("GREEN", "Y_SHAPE"),
        shape("YELLOW", "CROSS"),
        line("RED", 5),
    ],
    twoStar=7, threeStar=13,
    frozen=[cell(3, 3), cell(4, 4)],
))

# 111
W13.append(level(
    111, 13, "Spaghettified", 8, 8, 19,
    mk_tiles(8, 8, COLORS_6, violet_count=12),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("BLUE", "X_SHAPE"),
        shape("ORANGE", "L_SHAPE"),
        line("GREEN", 6),
    ],
    twoStar=7, threeStar=13,
    frozen=[cell(1, 1), cell(6, 6)],
))

# 112
W13.append(level(
    112, 13, "Tidal Forces", 8, 8, 20,
    mk_tiles(8, 8, COLORS_6, violet_count=13),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("RED", "Y_SHAPE"),
        shape("YELLOW", "U_SHAPE"),
        line("BLUE", 6),
        square("GREEN"),
    ],
    twoStar=7, threeStar=14,
    frozen=[cell(2, 5), cell(5, 2)],
))

# 113
W13.append(level(
    113, 13, "Photon Sphere", 8, 8, 20,
    mk_tiles(8, 8, COLORS_6, violet_count=12),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("ORANGE", "X_SHAPE"),
        shape("BLUE", "CROSS"),
        shape("RED", "T_SHAPE"),
        line("GREEN", 5),
    ],
    twoStar=8, threeStar=14,
    frozen=[cell(0, 0), cell(7, 7), cell(0, 7), cell(7, 0)],
))

# 114
W13.append(level(
    114, 13, "Accretion Disk", 8, 8, 21,
    mk_tiles(8, 8, COLORS_6, violet_count=13),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "Y_SHAPE"),
        shape("YELLOW", "Z_SHAPE"),
        line("RED", 6),
        square("BLUE"),
    ],
    twoStar=8, threeStar=15,
    frozen=[cell(2, 2), cell(5, 5)],
))

# 115
W13.append(level(
    115, 13, "Singular Pulse", 8, 8, 21,
    mk_tiles(8, 8, COLORS_6, violet_count=13),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("BLUE", "Y_SHAPE"),
        shape("GREEN", "CROSS"),
        shape("ORANGE", "U_SHAPE"),
        line("YELLOW", 6),
    ],
    twoStar=8, threeStar=15,
    frozen=[cell(1, 4), cell(4, 1), cell(4, 6), cell(6, 4)],
))

# 116
W13.append(level(
    116, 13, "Collapsed Star", 8, 8, 22,
    mk_tiles(8, 8, COLORS_6, violet_count=14),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("VIOLET", "X_SHAPE"),
        shape("RED", "Z_SHAPE"),
        shape("BLUE", "U_SHAPE"),
        line("GREEN", 6),
        square("ORANGE"),
    ],
    twoStar=9, threeStar=16,
    frozen=[cell(2, 3), cell(3, 5), cell(5, 2), cell(6, 4)],
))

# 117 — Marquee
W13.append(level(
    117, 13, "Singularity Spire", 8, 8, 23,
    mk_tiles(8, 8, COLORS_6, violet_count=15),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "Y_SHAPE"),
        shape("RED", "CROSS"),
        shape("BLUE", "U_SHAPE"),
        shape("GREEN", "T_SHAPE"),
        line("ORANGE", 6),
        square("YELLOW"),
    ],
    twoStar=10, threeStar=17,
    frozen=[cell(0, 3), cell(3, 0), cell(3, 7), cell(7, 3), cell(4, 4)],
))


# ─────────────────────────────────────────────────────────────
# WORLD 14 — Infinity Prism (9x9, marquee challenge)
# ─────────────────────────────────────────────────────────────

W14 = []

# 118
W14.append(level(
    118, 14, "Prismatic Dawn", 9, 9, 21,
    mk_tiles(9, 9, COLORS_6, violet_count=14),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "Y_SHAPE"),
        shape("RED", "CROSS"),
        line("BLUE", 6),
        square("GREEN"),
    ],
    twoStar=8, threeStar=15,
))

# 119
W14.append(level(
    119, 14, "Spectral Bridge", 9, 9, 22,
    mk_tiles(9, 9, COLORS_6, violet_count=15),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("YELLOW", "X_SHAPE"),
        shape("ORANGE", "U_SHAPE"),
        line("RED", 7),
        square("BLUE"),
    ],
    twoStar=8, threeStar=15,
    frozen=[cell(4, 4)],
))

# 120
W14.append(level(
    120, 14, "Refracted Path", 9, 9, 22,
    mk_tiles(9, 9, COLORS_6, violet_count=14),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("BLUE", "Y_SHAPE"),
        shape("GREEN", "Z_SHAPE"),
        shape("RED", "T_SHAPE"),
        line("ORANGE", 6),
    ],
    twoStar=9, threeStar=16,
    frozen=[cell(2, 2), cell(6, 6)],
))

# 121
W14.append(level(
    121, 14, "Holographic Veil", 9, 9, 23,
    mk_tiles(9, 9, COLORS_6, violet_count=15),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("VIOLET", "X_SHAPE"),
        shape("RED", "CROSS"),
        shape("GREEN", "L_SHAPE"),
        line("BLUE", 6),
        square("YELLOW"),
    ],
    twoStar=9, threeStar=16,
    frozen=[cell(1, 4), cell(4, 1), cell(4, 7), cell(7, 4)],
))

# 122
W14.append(level(
    122, 14, "Crystalline Mind", 9, 9, 23,
    mk_tiles(9, 9, COLORS_6, violet_count=16),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("ORANGE", "Y_SHAPE"),
        shape("RED", "U_SHAPE"),
        shape("BLUE", "CROSS"),
        line("GREEN", 7),
        square("YELLOW"),
    ],
    twoStar=10, threeStar=17,
    frozen=[cell(2, 4), cell(4, 2), cell(4, 6), cell(6, 4)],
))

# 123
W14.append(level(
    123, 14, "Resonant Bloom", 9, 9, 24,
    mk_tiles(9, 9, COLORS_6, violet_count=16),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("VIOLET", "X_SHAPE"),
        shape("YELLOW", "CROSS"),
        shape("BLUE", "Z_SHAPE"),
        shape("RED", "U_SHAPE"),
        line("ORANGE", 6),
    ],
    twoStar=10, threeStar=17,
    frozen=[cell(0, 4), cell(4, 0), cell(8, 4), cell(4, 8), cell(4, 4)],
))

# 124
W14.append(level(
    124, 14, "Mirror Universe", 9, 9, 24,
    mk_tiles(9, 9, COLORS_6, violet_count=17),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "Y_SHAPE"),
        shape("GREEN", "CROSS"),
        shape("ORANGE", "T_SHAPE"),
        line("RED", 7),
        line("BLUE", 6),
        square("YELLOW"),
    ],
    twoStar=11, threeStar=18,
    frozen=[cell(2, 2), cell(2, 6), cell(6, 2), cell(6, 6)],
))

# 125
W14.append(level(
    125, 14, "Eternity's Gate", 9, 9, 25,
    mk_tiles(9, 9, COLORS_6, violet_count=18),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "Y_SHAPE"),
        shape("RED", "CROSS"),
        shape("BLUE", "U_SHAPE"),
        shape("YELLOW", "Z_SHAPE"),
        line("GREEN", 7),
        square("ORANGE"),
    ],
    twoStar=11, threeStar=19,
    frozen=[cell(1, 1), cell(1, 7), cell(7, 1), cell(7, 7), cell(4, 4)],
))

# 126 — Final marquee
W14.append(level(
    126, 14, "Infinity Prism", 9, 9, 26,
    mk_tiles(9, 9, COLORS_6, violet_count=20),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "Y_SHAPE"),
        shape("RED", "CROSS"),
        shape("BLUE", "U_SHAPE"),
        shape("GREEN", "T_SHAPE"),
        shape("ORANGE", "Z_SHAPE"),
        shape("YELLOW", "L_SHAPE"),
        line("RED", 7),
        line("BLUE", 7),
    ],
    twoStar=12, threeStar=20,
    frozen=[cell(0, 4), cell(4, 0), cell(4, 8), cell(8, 4), cell(2, 2), cell(2, 6), cell(6, 2), cell(6, 6)],
    voids=[cell(0, 0), cell(0, 8), cell(8, 0), cell(8, 8), cell(4, 4)],
))


# ─────────────────────────────────────────────────────────────
# Combine, renumber, and write
# ─────────────────────────────────────────────────────────────

# Pro+ worlds use strict consecutive numbering: 11-14 with level IDs 91-126.
# The Challenge Lab lives at world 0 and does not consume a world number.
ID_OFFSET = 0       # 91 stays 91, ..., 126 stays 126
WORLD_OFFSET = 0    # 11 stays 11, ..., 14 stays 14

new_levels = W11 + W12 + W13 + W14
assert len(new_levels) == 36, f"Expected 36 levels, got {len(new_levels)}"

# Apply offsets so worlds become 12-15 and IDs 100-135
for lv in new_levels:
    lv["id"] += ID_OFFSET
    lv["world"] += WORLD_OFFSET

for i, lv in enumerate(new_levels):
    assert lv["id"] == 91 + i, f"Level id mismatch at index {i}: {lv['id']}"
    assert 11 <= lv["world"] <= 14, f"World out of range: {lv['world']}"

# Load existing levels.json (list)
with open(LEVELS_PATH, "r", encoding="utf-8") as f:
    existing = json.load(f)

# Remove any already-existing levels with id >= 91 (re-runs from any prior schema)
existing = [l for l in existing if l.get("id", 0) < 91]

combined = existing + new_levels

with open(LEVELS_PATH, "w", encoding="utf-8") as f:
    json.dump(combined, f, indent=2)

print(f"Wrote {len(combined)} total levels (added {len(new_levels)} new) to {LEVELS_PATH}")
