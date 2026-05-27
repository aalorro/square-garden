"""
Generate 36 hand-crafted levels for Pro+ worlds 11-14 (levels 91-126).
Overwrites any levels with id >= 91 in app/src/main/res/raw/levels.json.

Levels feature VIOLET tiles and X_SHAPE / Y_SHAPE goals (Pro+ exclusive).

DIFFICULTY MODEL
================
Pro+ skill applies a 0.55x multiplier to maxMoves at runtime. So a level
with maxMoves=30 in JSON is actually played with round(30 * 0.55) = 17
swaps. Star thresholds (twoStar / threeStar) are "moves remaining" — they
are compared against the *adjusted* movesRemaining, so they must fit
within the adjusted budget.

Pro+ is supposed to be HARDER than world 10. World 10 finale on Pro skill
(0.7x) plays at ~16-20 actual moves for 6-7 goals on 9x9. Pro+ worlds
11-14 should match or exceed that pressure:

  6-goal level   -> target ~13-15 actual moves
  7-goal level   -> target ~16-18 actual moves
  8-goal level   -> target ~19-21 actual moves
  9-goal level   -> target ~22-24 actual moves
 10-goal level   -> target ~26 actual moves

Board sizes are intentionally mixed:
  * 9x9 — the dominant size (matches world 10 baseline)
  * 8x8 — scattered, slightly less swap room
  * 7x7 — rare, very tight: fewer goals but extreme constraint
Every level has at least 6 goals (user requirement: never less than 5).

Star thresholds are tuned so 3-star requires near-optimal play (about
half the adjusted budget remaining).

Run from project root:
    python scripts/gen_pro_plus_levels.py
"""
import json
import random
from pathlib import Path

random.seed(20260527)  # deterministic

LEVELS_PATH = Path("app/src/main/res/raw/levels.json")

COLORS_6 = ["RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "VIOLET"]


def mk_tiles(w, h, palette, violet_count=None):
    """Random tiles; if violet_count is set, force exactly that many VIOLET."""
    grid = [[random.choice(palette) for _ in range(w)] for _ in range(h)]
    if violet_count is not None and "VIOLET" in palette:
        other = [c for c in palette if c != "VIOLET"]
        for r in range(h):
            for c in range(w):
                if grid[r][c] == "VIOLET":
                    grid[r][c] = random.choice(other)
        all_cells = [(r, c) for r in range(h) for c in range(w)]
        random.shuffle(all_cells)
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
# WORLD 11 — Nebula Verge (intro VIOLET + X_SHAPE)
# Mixed 7x7 / 8x8 / 9x9. Always 6+ goals; line lengths kept modest on
# smaller boards (max length = min(w,h) - 1).
# ─────────────────────────────────────────────────────────────

W11 = []

# 91 — 9x9 tutorial: introduce VIOLET + X_SHAPE, 6 goals
W11.append(level(
    91, 11, "Stardust Awakens", 9, 9, 28,
    mk_tiles(9, 9, COLORS_6, violet_count=16),
    [
        shape("VIOLET", "X_SHAPE"),
        line("VIOLET", 4),
        line("BLUE", 6),
        square("YELLOW"),
        shape("RED", "L_SHAPE"),
        shape("GREEN", "T_SHAPE"),
    ],
    twoStar=5, threeStar=9,
    tutorial=[
        {"message": "Welcome to Pro+!\nFour cosmic worlds await beyond Pro."},
        {"message": "Meet VIOLET — a sixth color found only in these worlds."},
        {"message": "The X-shape is a diagonal cross of 5 tiles.\nForm one of any color to clear the goal."},
    ],
))

# 92 — 8x8, 6 goals, no frozen
W11.append(level(
    92, 11, "Constellation Lines", 8, 8, 26,
    mk_tiles(8, 8, COLORS_6, violet_count=13),
    [
        shape("VIOLET", "X_SHAPE"),
        line("VIOLET", 5),
        square("GREEN"),
        line("RED", 6),
        shape("ORANGE", "U_SHAPE"),
        shape("BLUE", "Z_SHAPE"),
    ],
    twoStar=4, threeStar=8,
))

# 93 — 9x9, 6 goals
W11.append(level(
    93, 11, "Crossed Stars", 9, 9, 27,
    mk_tiles(9, 9, COLORS_6, violet_count=15),
    [
        shape("VIOLET", "X_SHAPE"),
        square("VIOLET"),
        shape("GREEN", "CROSS"),
        shape("BLUE", "Z_SHAPE"),
        line("YELLOW", 6),
        shape("RED", "L_SHAPE"),
    ],
    twoStar=4, threeStar=8,
))

# 94 — 7x7, 6 goals: very tight (49 cells, 6 distinct goals)
W11.append(level(
    94, 11, "Nebula Bloom", 7, 7, 22,
    mk_tiles(7, 7, COLORS_6, violet_count=10),
    [
        shape("VIOLET", "X_SHAPE"),
        line("VIOLET", 4),
        shape("YELLOW", "T_SHAPE"),
        shape("BLUE", "U_SHAPE"),
        line("GREEN", 5),
        square("ORANGE"),
    ],
    twoStar=4, threeStar=7,
))

# 95 — 9x9, 6 goals, dual X
W11.append(level(
    95, 11, "Twin Crosses", 9, 9, 28,
    mk_tiles(9, 9, COLORS_6, violet_count=15),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("RED", "X_SHAPE"),
        shape("YELLOW", "U_SHAPE"),
        shape("GREEN", "L_SHAPE"),
        line("ORANGE", 6),
        line("BLUE", 7),
    ],
    twoStar=4, threeStar=8,
))

# 96 — 8x8, 7 goals, frozen
W11.append(level(
    96, 11, "Galactic Drift", 8, 8, 30,
    mk_tiles(8, 8, COLORS_6, violet_count=14),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("BLUE", "X_SHAPE"),
        shape("VIOLET", "Z_SHAPE"),
        shape("YELLOW", "CROSS"),
        shape("RED", "T_SHAPE"),
        line("ORANGE", 6),
        square("GREEN"),
    ],
    twoStar=5, threeStar=9,
    frozen=[cell(2, 2), cell(5, 5)],
))

# 97 — 9x9, 7 goals, four frozen
W11.append(level(
    97, 11, "Frozen Stardust", 9, 9, 32,
    mk_tiles(9, 9, COLORS_6, violet_count=16),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "U_SHAPE"),
        shape("BLUE", "L_SHAPE"),
        shape("YELLOW", "Z_SHAPE"),
        shape("GREEN", "T_SHAPE"),
        line("RED", 7),
        line("ORANGE", 6),
    ],
    twoStar=5, threeStar=10,
    frozen=[cell(1, 4), cell(4, 1), cell(4, 7), cell(7, 4)],
))

# 98 — 9x9, 7 goals, heavy frozen
W11.append(level(
    98, 11, "Pulsar Symphony", 9, 9, 33,
    mk_tiles(9, 9, COLORS_6, violet_count=17),
    [
        shape("VIOLET", "X_SHAPE"),
        square("VIOLET"),
        shape("RED", "CROSS"),
        shape("BLUE", "Z_SHAPE"),
        shape("YELLOW", "U_SHAPE"),
        shape("ORANGE", "L_SHAPE"),
        line("GREEN", 7),
    ],
    twoStar=6, threeStar=10,
    frozen=[cell(0, 4), cell(4, 0), cell(4, 8), cell(8, 4)],
))

# 99 — 9x9 marquee, 7 goals
W11.append(level(
    99, 11, "Nebula Verge", 9, 9, 34,
    mk_tiles(9, 9, COLORS_6, violet_count=18),
    [
        shape("VIOLET", "X_SHAPE"),
        square("VIOLET"),
        shape("YELLOW", "CROSS"),
        shape("RED", "T_SHAPE"),
        shape("BLUE", "Z_SHAPE"),
        shape("ORANGE", "U_SHAPE"),
        line("GREEN", 8),
    ],
    twoStar=6, threeStar=11,
    frozen=[cell(2, 2), cell(2, 6), cell(6, 2), cell(6, 6), cell(4, 4)],
))


# ─────────────────────────────────────────────────────────────
# WORLD 12 — Quantum Lattice (intro Y_SHAPE)
# ─────────────────────────────────────────────────────────────

W12 = []

# 100 — 9x9 Y_SHAPE tutorial, 7 goals
W12.append(level(
    100, 12, "Quantum Spark", 9, 9, 32,
    mk_tiles(9, 9, COLORS_6, violet_count=15),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("VIOLET", "X_SHAPE"),
        shape("RED", "CROSS"),
        line("BLUE", 6),
        square("ORANGE"),
        shape("GREEN", "T_SHAPE"),
        shape("YELLOW", "U_SHAPE"),
    ],
    twoStar=5, threeStar=10,
    tutorial=[
        {"message": "The Y-shape: a vertical stem with two arms reaching the top."},
        {"message": "5 tiles of one color form the Y.\nLook for natural Y patterns on the board."},
    ],
))

# 101 — 8x8, 7 goals (tight)
W12.append(level(
    101, 12, "Entangled Threads", 8, 8, 31,
    mk_tiles(8, 8, COLORS_6, violet_count=13),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("ORANGE", "X_SHAPE"),
        shape("GREEN", "U_SHAPE"),
        shape("BLUE", "L_SHAPE"),
        line("RED", 6),
        line("YELLOW", 7),
        square("BLUE"),
    ],
    twoStar=5, threeStar=9,
))

# 102 — 9x9, 7 goals
W12.append(level(
    102, 12, "Probability Wave", 9, 9, 33,
    mk_tiles(9, 9, COLORS_6, violet_count=15),
    [
        shape("YELLOW", "Y_SHAPE"),
        shape("VIOLET", "X_SHAPE"),
        shape("RED", "CROSS"),
        shape("BLUE", "T_SHAPE"),
        shape("GREEN", "Z_SHAPE"),
        line("ORANGE", 7),
        square("YELLOW"),
    ],
    twoStar=5, threeStar=10,
    frozen=[cell(3, 3), cell(5, 5)],
))

# 103 — 7x7, 6 goals (brutal compact challenge)
W12.append(level(
    103, 12, "Photon Net", 7, 7, 24,
    mk_tiles(7, 7, COLORS_6, violet_count=11),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("VIOLET", "X_SHAPE"),
        shape("BLUE", "U_SHAPE"),
        shape("GREEN", "T_SHAPE"),
        line("YELLOW", 5),
        square("ORANGE"),
    ],
    twoStar=4, threeStar=8,
    frozen=[cell(3, 3)],
))

# 104 — 9x9, 8 goals
W12.append(level(
    104, 12, "Superposition", 9, 9, 35,
    mk_tiles(9, 9, COLORS_6, violet_count=15),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("ORANGE", "X_SHAPE"),
        shape("BLUE", "Y_SHAPE"),
        shape("RED", "CROSS"),
        shape("YELLOW", "U_SHAPE"),
        line("GREEN", 7),
        line("RED", 6),
        square("BLUE"),
    ],
    twoStar=6, threeStar=11,
    frozen=[cell(3, 3), cell(4, 4), cell(5, 5)],
))

# 105 — 8x8, 7 goals, intricate
W12.append(level(
    105, 12, "Bose Field", 8, 8, 32,
    mk_tiles(8, 8, COLORS_6, violet_count=15),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("VIOLET", "X_SHAPE"),
        shape("RED", "CROSS"),
        shape("BLUE", "Z_SHAPE"),
        shape("GREEN", "U_SHAPE"),
        line("YELLOW", 7),
        square("GREEN"),
    ],
    twoStar=5, threeStar=10,
    frozen=[cell(1, 1), cell(6, 6)],
))

# 106 — 9x9, 8 goals
W12.append(level(
    106, 12, "Lattice Knot", 9, 9, 37,
    mk_tiles(9, 9, COLORS_6, violet_count=17),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("GREEN", "Y_SHAPE"),
        shape("ORANGE", "Z_SHAPE"),
        shape("RED", "U_SHAPE"),
        shape("YELLOW", "L_SHAPE"),
        shape("BLUE", "T_SHAPE"),
        line("BLUE", 7),
        line("RED", 6),
    ],
    twoStar=6, threeStar=11,
    frozen=[cell(1, 6), cell(6, 1), cell(2, 2), cell(6, 6)],
))

# 107 — 9x9, 8 goals
W12.append(level(
    107, 12, "Decoherent Dawn", 9, 9, 38,
    mk_tiles(9, 9, COLORS_6, violet_count=18),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("VIOLET", "X_SHAPE"),
        shape("RED", "CROSS"),
        shape("YELLOW", "U_SHAPE"),
        shape("BLUE", "L_SHAPE"),
        shape("GREEN", "T_SHAPE"),
        line("ORANGE", 7),
        square("VIOLET"),
    ],
    twoStar=6, threeStar=12,
    frozen=[cell(2, 5), cell(5, 2), cell(3, 3), cell(5, 5)],
))

# 108 — 9x9 marquee, 8 goals
W12.append(level(
    108, 12, "Quantum Lattice", 9, 9, 40,
    mk_tiles(9, 9, COLORS_6, violet_count=19),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("VIOLET", "X_SHAPE"),
        shape("RED", "CROSS"),
        shape("GREEN", "U_SHAPE"),
        shape("YELLOW", "Z_SHAPE"),
        shape("ORANGE", "T_SHAPE"),
        line("BLUE", 8),
        square("ORANGE"),
    ],
    twoStar=7, threeStar=12,
    frozen=[cell(0, 4), cell(4, 0), cell(4, 8), cell(8, 4), cell(4, 4)],
))


# ─────────────────────────────────────────────────────────────
# WORLD 13 — Singularity Spire (dense shapes, more frozen)
# ─────────────────────────────────────────────────────────────

W13 = []

# 109 — 9x9, 7 goals
W13.append(level(
    109, 13, "Event Horizon", 9, 9, 33,
    mk_tiles(9, 9, COLORS_6, violet_count=15),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("VIOLET", "X_SHAPE"),
        shape("RED", "CROSS"),
        shape("BLUE", "U_SHAPE"),
        shape("ORANGE", "L_SHAPE"),
        line("GREEN", 7),
        square("YELLOW"),
    ],
    twoStar=5, threeStar=10,
    frozen=[cell(2, 2), cell(6, 6)],
))

# 110 — 8x8, 7 goals
W13.append(level(
    110, 13, "Gravity Well", 8, 8, 32,
    mk_tiles(8, 8, COLORS_6, violet_count=14),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("GREEN", "Y_SHAPE"),
        shape("YELLOW", "CROSS"),
        shape("RED", "T_SHAPE"),
        shape("BLUE", "Z_SHAPE"),
        line("ORANGE", 7),
        square("RED"),
    ],
    twoStar=5, threeStar=10,
    frozen=[cell(3, 3), cell(4, 4)],
))

# 111 — 9x9, 8 goals
W13.append(level(
    111, 13, "Spaghettified", 9, 9, 36,
    mk_tiles(9, 9, COLORS_6, violet_count=17),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("BLUE", "X_SHAPE"),
        shape("ORANGE", "Y_SHAPE"),
        shape("RED", "U_SHAPE"),
        shape("GREEN", "Z_SHAPE"),
        shape("YELLOW", "L_SHAPE"),
        line("GREEN", 7),
        square("BLUE"),
    ],
    twoStar=6, threeStar=11,
    frozen=[cell(1, 1), cell(6, 6), cell(1, 7), cell(7, 1)],
))

# 112 — 9x9, 8 goals
W13.append(level(
    112, 13, "Tidal Forces", 9, 9, 37,
    mk_tiles(9, 9, COLORS_6, violet_count=18),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "Y_SHAPE"),
        shape("RED", "CROSS"),
        shape("YELLOW", "U_SHAPE"),
        shape("BLUE", "T_SHAPE"),
        shape("GREEN", "L_SHAPE"),
        line("ORANGE", 7),
        square("GREEN"),
    ],
    twoStar=6, threeStar=11,
    frozen=[cell(2, 5), cell(5, 2), cell(3, 3), cell(5, 5)],
))

# 113 — 8x8, 7 goals, very intricate
W13.append(level(
    113, 13, "Photon Sphere", 8, 8, 33,
    mk_tiles(8, 8, COLORS_6, violet_count=14),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("ORANGE", "X_SHAPE"),
        shape("BLUE", "CROSS"),
        shape("RED", "T_SHAPE"),
        shape("YELLOW", "Z_SHAPE"),
        line("GREEN", 7),
        line("BLUE", 6),
    ],
    twoStar=5, threeStar=10,
    frozen=[cell(0, 0), cell(7, 7), cell(0, 7), cell(7, 0)],
))

# 114 — 9x9, 8 goals
W13.append(level(
    114, 13, "Accretion Disk", 9, 9, 38,
    mk_tiles(9, 9, COLORS_6, violet_count=18),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "Y_SHAPE"),
        shape("YELLOW", "Z_SHAPE"),
        shape("RED", "CROSS"),
        shape("BLUE", "U_SHAPE"),
        shape("GREEN", "T_SHAPE"),
        line("ORANGE", 8),
        square("YELLOW"),
    ],
    twoStar=6, threeStar=12,
    frozen=[cell(2, 2), cell(5, 5), cell(2, 6), cell(6, 2)],
))

# 115 — 7x7, 6 goals (extreme compact challenge)
W13.append(level(
    115, 13, "Singular Pulse", 7, 7, 25,
    mk_tiles(7, 7, COLORS_6, violet_count=11),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "Y_SHAPE"),
        shape("GREEN", "CROSS"),
        shape("RED", "T_SHAPE"),
        line("YELLOW", 5),
        square("BLUE"),
    ],
    twoStar=4, threeStar=8,
    frozen=[cell(3, 3)],
))

# 116 — 9x9, 9 goals
W13.append(level(
    116, 13, "Collapsed Star", 9, 9, 40,
    mk_tiles(9, 9, COLORS_6, violet_count=19),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("VIOLET", "X_SHAPE"),
        shape("RED", "Z_SHAPE"),
        shape("BLUE", "U_SHAPE"),
        shape("GREEN", "T_SHAPE"),
        shape("YELLOW", "L_SHAPE"),
        shape("ORANGE", "CROSS"),
        line("GREEN", 7),
        square("ORANGE"),
    ],
    twoStar=7, threeStar=12,
    frozen=[cell(2, 3), cell(3, 5), cell(5, 2), cell(6, 4)],
))

# 117 — 9x9 marquee, 9 goals
W13.append(level(
    117, 13, "Singularity Spire", 9, 9, 42,
    mk_tiles(9, 9, COLORS_6, violet_count=20),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "Y_SHAPE"),
        shape("RED", "CROSS"),
        shape("BLUE", "U_SHAPE"),
        shape("GREEN", "T_SHAPE"),
        shape("ORANGE", "Z_SHAPE"),
        shape("YELLOW", "L_SHAPE"),
        line("ORANGE", 8),
        square("YELLOW"),
    ],
    twoStar=7, threeStar=13,
    frozen=[cell(0, 4), cell(4, 0), cell(4, 8), cell(8, 4), cell(4, 4),
            cell(2, 2), cell(6, 6)],
))


# ─────────────────────────────────────────────────────────────
# WORLD 14 — Infinity Prism (escalating, voids in late levels)
# ─────────────────────────────────────────────────────────────

W14 = []

# 118 — 9x9, 8 goals
W14.append(level(
    118, 14, "Prismatic Dawn", 9, 9, 36,
    mk_tiles(9, 9, COLORS_6, violet_count=17),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "Y_SHAPE"),
        shape("RED", "CROSS"),
        shape("BLUE", "U_SHAPE"),
        shape("YELLOW", "T_SHAPE"),
        shape("GREEN", "Z_SHAPE"),
        line("ORANGE", 7),
        square("RED"),
    ],
    twoStar=6, threeStar=11,
    frozen=[cell(3, 3), cell(5, 5)],
))

# 119 — 8x8, 7 goals, frozen-heavy
W14.append(level(
    119, 14, "Spectral Bridge", 8, 8, 33,
    mk_tiles(8, 8, COLORS_6, violet_count=15),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("YELLOW", "X_SHAPE"),
        shape("ORANGE", "U_SHAPE"),
        shape("BLUE", "CROSS"),
        shape("RED", "T_SHAPE"),
        line("RED", 7),
        square("BLUE"),
    ],
    twoStar=5, threeStar=10,
    frozen=[cell(2, 4), cell(4, 2), cell(4, 6), cell(6, 4)],
))

# 120 — 9x9, 8 goals
W14.append(level(
    120, 14, "Refracted Path", 9, 9, 38,
    mk_tiles(9, 9, COLORS_6, violet_count=18),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("BLUE", "Y_SHAPE"),
        shape("GREEN", "Z_SHAPE"),
        shape("RED", "T_SHAPE"),
        shape("YELLOW", "CROSS"),
        shape("ORANGE", "U_SHAPE"),
        line("VIOLET", 6),
        square("GREEN"),
    ],
    twoStar=6, threeStar=12,
    frozen=[cell(2, 2), cell(6, 6), cell(2, 6), cell(6, 2)],
))

# 121 — 9x9, 9 goals, first void
W14.append(level(
    121, 14, "Holographic Veil", 9, 9, 40,
    mk_tiles(9, 9, COLORS_6, violet_count=18),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("VIOLET", "X_SHAPE"),
        shape("RED", "CROSS"),
        shape("GREEN", "L_SHAPE"),
        shape("BLUE", "U_SHAPE"),
        shape("YELLOW", "T_SHAPE"),
        shape("ORANGE", "Z_SHAPE"),
        line("BLUE", 7),
        square("YELLOW"),
    ],
    twoStar=7, threeStar=12,
    frozen=[cell(1, 4), cell(4, 1), cell(4, 7), cell(7, 4)],
    voids=[cell(4, 4)],
))

# 122 — 7x7, 6 goals (extreme compact, final 7x7)
W14.append(level(
    122, 14, "Crystalline Mind", 7, 7, 26,
    mk_tiles(7, 7, COLORS_6, violet_count=12),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "Y_SHAPE"),
        shape("RED", "U_SHAPE"),
        shape("BLUE", "CROSS"),
        line("GREEN", 6),
        square("YELLOW"),
    ],
    twoStar=4, threeStar=8,
    frozen=[cell(3, 3)],
))

# 123 — 9x9, 9 goals, voids in corners
W14.append(level(
    123, 14, "Resonant Bloom", 9, 9, 42,
    mk_tiles(9, 9, COLORS_6, violet_count=20),
    [
        shape("VIOLET", "Y_SHAPE"),
        shape("VIOLET", "X_SHAPE"),
        shape("YELLOW", "CROSS"),
        shape("BLUE", "Z_SHAPE"),
        shape("RED", "U_SHAPE"),
        shape("GREEN", "T_SHAPE"),
        shape("ORANGE", "L_SHAPE"),
        line("ORANGE", 7),
        square("RED"),
    ],
    twoStar=7, threeStar=13,
    frozen=[cell(0, 4), cell(4, 0), cell(8, 4), cell(4, 8), cell(4, 4)],
    voids=[cell(0, 0), cell(8, 8), cell(0, 8), cell(8, 0)],
))

# 124 — 9x9, 9 goals
W14.append(level(
    124, 14, "Mirror Universe", 9, 9, 43,
    mk_tiles(9, 9, COLORS_6, violet_count=20),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "Y_SHAPE"),
        shape("GREEN", "CROSS"),
        shape("ORANGE", "T_SHAPE"),
        shape("RED", "U_SHAPE"),
        shape("BLUE", "Z_SHAPE"),
        line("RED", 8),
        line("BLUE", 7),
        square("YELLOW"),
    ],
    twoStar=7, threeStar=13,
    frozen=[cell(2, 2), cell(2, 6), cell(6, 2), cell(6, 6)],
    voids=[cell(0, 0), cell(8, 8), cell(4, 4)],
))

# 125 — 9x9, 9 goals, heavy voids
W14.append(level(
    125, 14, "Eternity's Gate", 9, 9, 44,
    mk_tiles(9, 9, COLORS_6, violet_count=21),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "Y_SHAPE"),
        shape("RED", "CROSS"),
        shape("BLUE", "U_SHAPE"),
        shape("YELLOW", "Z_SHAPE"),
        shape("ORANGE", "L_SHAPE"),
        shape("GREEN", "T_SHAPE"),
        line("GREEN", 8),
        square("ORANGE"),
    ],
    twoStar=7, threeStar=14,
    frozen=[cell(1, 1), cell(1, 7), cell(7, 1), cell(7, 7), cell(4, 4)],
    voids=[cell(0, 4), cell(4, 0), cell(4, 8), cell(8, 4)],
))

# 126 — 9x9 final marquee, 10 goals, max constraints
W14.append(level(
    126, 14, "Infinity Prism", 9, 9, 48,
    mk_tiles(9, 9, COLORS_6, violet_count=22),
    [
        shape("VIOLET", "X_SHAPE"),
        shape("VIOLET", "Y_SHAPE"),
        shape("RED", "CROSS"),
        shape("BLUE", "U_SHAPE"),
        shape("GREEN", "T_SHAPE"),
        shape("ORANGE", "Z_SHAPE"),
        shape("YELLOW", "L_SHAPE"),
        line("RED", 8),
        line("BLUE", 7),
        square("VIOLET"),
    ],
    twoStar=8, threeStar=14,
    frozen=[cell(0, 4), cell(4, 0), cell(4, 8), cell(8, 4),
            cell(2, 2), cell(2, 6), cell(6, 2), cell(6, 6)],
    voids=[cell(0, 0), cell(0, 8), cell(8, 0), cell(8, 8), cell(4, 4)],
))


# ─────────────────────────────────────────────────────────────
# Combine, validate, and write
# ─────────────────────────────────────────────────────────────

new_levels = W11 + W12 + W13 + W14
assert len(new_levels) == 36, f"Expected 36 levels, got {len(new_levels)}"

for i, lv in enumerate(new_levels):
    assert lv["id"] == 91 + i, f"Level id mismatch at index {i}: {lv['id']}"
    assert 11 <= lv["world"] <= 14, f"World out of range: {lv['world']}"
    assert lv["width"] in (7, 8, 9) and lv["height"] in (7, 8, 9), \
        f"L{lv['id']} unexpected size {lv['width']}x{lv['height']}"
    assert len(lv["goals"]) >= 6, \
        f"L{lv['id']} has only {len(lv['goals'])} goals (min 6 required)"
    adj = round(lv["maxMoves"] * 0.55)
    assert lv["stars"]["threeStar"] < adj, \
        f"L{lv['id']}: 3-star {lv['stars']['threeStar']} >= adjMax {adj}"
    assert lv["stars"]["twoStar"] < lv["stars"]["threeStar"], \
        f"L{lv['id']}: 2-star >= 3-star"
    # Sanity: line lengths must fit the smaller board dim
    short = min(lv["width"], lv["height"])
    for g in lv["goals"]:
        if g["type"] == "line":
            assert g["length"] <= short, \
                f"L{lv['id']} line of length {g['length']} > board dim {short}"

# Load existing levels.json (list); strip any prior id >= 91.
with open(LEVELS_PATH, "r", encoding="utf-8") as f:
    existing = json.load(f)
existing = [l for l in existing if l.get("id", 0) < 91]

combined = existing + new_levels

with open(LEVELS_PATH, "w", encoding="utf-8") as f:
    json.dump(combined, f, indent=2)

# Per-level summary for tuning audit.
print(f"Wrote {len(combined)} levels (Pro+: {len(new_levels)}) to {LEVELS_PATH}\n")
print(f"{'id':>4} {'w':>2} size  {'name':<22} goals raw adj 2*  3*")
for lv in new_levels:
    adj = round(lv["maxMoves"] * 0.55)
    sz = f"{lv['width']}x{lv['height']}"
    print(f"{lv['id']:>4} {lv['world']:>2} {sz:>4}  {lv['name']:<22} "
          f"{len(lv['goals']):>5} {lv['maxMoves']:>3} {adj:>3} "
          f"{lv['stars']['twoStar']:>2} {lv['stars']['threeStar']:>3}")
