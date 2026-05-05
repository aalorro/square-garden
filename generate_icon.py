"""Generate 512x512 PNG app icon and 1024x500 feature graphic using Pillow."""
from PIL import Image, ImageDraw, ImageFont
import os

OUTDIR = os.path.dirname(os.path.abspath(__file__))
S = 512  # icon size
# Scale factor: 108 viewport → 512 pixels
F = S / 108.0


def sc(x):
    """Scale coordinate."""
    return int(x * F)


def draw_rounded_rect(draw, x, y, w, h, r, fill):
    """Draw a rounded rectangle."""
    x1, y1 = sc(x), sc(y)
    x2, y2 = sc(x + w), sc(y + h)
    r = sc(r)
    draw.rounded_rectangle([x1, y1, x2, y2], radius=r, fill=fill)


def draw_tile(draw, x, y, shadow_color, face_color, sheen_alpha=48):
    """Draw a single embossed tile at (x, y) with width=16, height=16."""
    tw, th = 16, 16
    r = 3

    # Shadow (offset down 1.5 units)
    draw_rounded_rect(draw, x, y + 1.5, tw, th, r, shadow_color)
    # Main face
    draw_rounded_rect(draw, x, y, tw, th, r, face_color)
    # Highlight sheen (top strip)
    x1, y1 = sc(x), sc(y)
    x2, y2 = sc(x + tw), sc(y + 5)
    r_sc = sc(r)
    overlay = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    od.rounded_rectangle([x1, y1, x2, y2], radius=r_sc, fill=(255, 255, 255, sheen_alpha))
    draw._image.paste(Image.alpha_composite(draw._image.copy(), overlay), (0, 0))


# ── Create icon canvas ──
icon = Image.new("RGBA", (S, S), (232, 224, 212, 255))  # #E8E0D4 cream background
draw = ImageDraw.Draw(icon)

# Drop shadow behind 2x2 grid
draw_rounded_rect(draw, 33, 34.5, 42, 42, 4, (0, 0, 0, 32))

# RED tile (top-left) at (35, 36), 16x16
draw_rounded_rect(draw, 35, 37.5, 16, 16, 3, (198, 40, 40, 255))   # shadow
draw_rounded_rect(draw, 35, 36, 16, 16, 3, (239, 83, 80, 255))     # face
# sheen
sheen = Image.new("RGBA", (S, S), (0, 0, 0, 0))
sd = ImageDraw.Draw(sheen)
draw_rounded_rect(sd, 35, 36, 16, 5, 3, (255, 255, 255, 48))
icon = Image.alpha_composite(icon, sheen)

# BLUE tile (top-right) at (54, 36), 16x16
draw = ImageDraw.Draw(icon)
draw_rounded_rect(draw, 54, 37.5, 16, 16, 3, (21, 101, 192, 255))  # shadow
draw_rounded_rect(draw, 54, 36, 16, 16, 3, (66, 165, 245, 255))    # face
sheen = Image.new("RGBA", (S, S), (0, 0, 0, 0))
sd = ImageDraw.Draw(sheen)
draw_rounded_rect(sd, 54, 36, 16, 5, 3, (255, 255, 255, 48))
icon = Image.alpha_composite(icon, sheen)

# GREEN tile (bottom-left) at (35, 55), 16x16
draw = ImageDraw.Draw(icon)
draw_rounded_rect(draw, 35, 56.5, 16, 16, 3, (46, 125, 50, 255))   # shadow
draw_rounded_rect(draw, 35, 55, 16, 16, 3, (102, 187, 106, 255))   # face
sheen = Image.new("RGBA", (S, S), (0, 0, 0, 0))
sd = ImageDraw.Draw(sheen)
draw_rounded_rect(sd, 35, 55, 16, 5, 3, (255, 255, 255, 48))
icon = Image.alpha_composite(icon, sheen)

# YELLOW tile (bottom-right) at (54, 55), 16x16
draw = ImageDraw.Draw(icon)
draw_rounded_rect(draw, 54, 56.5, 16, 16, 3, (249, 168, 37, 255))  # shadow
draw_rounded_rect(draw, 54, 55, 16, 16, 3, (255, 202, 40, 255))    # face
sheen = Image.new("RGBA", (S, S), (0, 0, 0, 0))
sd = ImageDraw.Draw(sheen)
draw_rounded_rect(sd, 54, 55, 16, 5, 3, (255, 255, 255, 38))
icon = Image.alpha_composite(icon, sheen)

# Garden sprout
draw = ImageDraw.Draw(icon)
# Stem - curved line from center gap upward
stem_points = []
for t in range(100):
    t_f = t / 99.0
    # Bezier: M54,53 C53.5,47 54.5,40 54,30
    x = (1 - t_f) ** 3 * 54 + 3 * (1 - t_f) ** 2 * t_f * 53.5 + 3 * (1 - t_f) * t_f ** 2 * 54.5 + t_f ** 3 * 54
    y = (1 - t_f) ** 3 * 53 + 3 * (1 - t_f) ** 2 * t_f * 47 + 3 * (1 - t_f) * t_f ** 2 * 40 + t_f ** 3 * 30
    stem_points.append((sc(x), sc(y)))

for i in range(len(stem_points) - 1):
    draw.line([stem_points[i], stem_points[i + 1]], fill=(67, 160, 71, 255), width=max(2, sc(1.8)))

# Left leaf (simple ellipse approximation)
leaf_cx, leaf_cy = sc(50.5), sc(39.5)
leaf_w, leaf_h = sc(5), sc(3.5)
draw.ellipse([leaf_cx - leaf_w // 2, leaf_cy - leaf_h // 2,
              leaf_cx + leaf_w // 2, leaf_cy + leaf_h // 2],
             fill=(102, 187, 106, 255))

# Right leaf
leaf_cx, leaf_cy = sc(57.5), sc(33.5)
draw.ellipse([leaf_cx - leaf_w // 2, leaf_cy - leaf_h // 2,
              leaf_cx + leaf_w // 2, leaf_cy + leaf_h // 2],
             fill=(129, 199, 132, 255))

# Tiny bud at top
bud_cx, bud_cy = sc(54), sc(28)
bud_r = sc(2.5)
draw.ellipse([bud_cx - bud_r, bud_cy - bud_r, bud_cx + bud_r, bud_cy + bud_r],
             fill=(165, 214, 167, 255))

# Save 512x512 icon
icon_path = os.path.join(OUTDIR, "icon_512x512.png")
icon.save(icon_path)
print(f"Saved: {icon_path}")

# ── Feature Graphic 1024x500 ──
W, H = 1024, 500
fg = Image.new("RGBA", (W, H))
fg_draw = ImageDraw.Draw(fg)

# Green gradient background
for y in range(H):
    ratio = y / H
    r = int(76 * (1 - ratio) + 30 * ratio)
    g = int(175 * (1 - ratio) + 100 * ratio)
    b = int(80 * (1 - ratio) + 40 * ratio)
    fg_draw.line([(0, y), (W, y)], fill=(r, g, b, 255))

# Paste icon on left
icon_small = icon.resize((320, 320), Image.LANCZOS)
fg.paste(icon_small, (60, (H - 320) // 2), icon_small)

# Text
try:
    title_font = ImageFont.truetype("arial.ttf", 72)
    sub_font = ImageFont.truetype("arial.ttf", 30)
except:
    try:
        title_font = ImageFont.truetype("/c/Windows/Fonts/arial.ttf", 72)
        sub_font = ImageFont.truetype("/c/Windows/Fonts/arial.ttf", 30)
    except:
        title_font = ImageFont.load_default()
        sub_font = ImageFont.load_default()

fg_draw = ImageDraw.Draw(fg)
tx = 430
fg_draw.text((tx, 120), "Square", fill=(255, 255, 255, 255), font=title_font)
fg_draw.text((tx, 200), "Garden", fill=(200, 235, 200, 255), font=title_font)
fg_draw.text((tx, 310), "A calm tile-swap puzzle game", fill=(210, 230, 210, 230), font=sub_font)
fg_draw.text((tx, 355), "90 levels across 10 worlds", fill=(200, 220, 200, 200), font=sub_font)

fg_path = os.path.join(OUTDIR, "feature_graphic_1024x500.png")
fg.save(fg_path)
print(f"Saved: {fg_path}")

print("\nDone! Files ready for Google Play upload.")
