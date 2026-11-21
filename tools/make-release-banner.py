#!/usr/bin/env python3
"""Draws an Edupage2 release announcement banner (1920x1080).

The style of a "feature drop" card: the name huge and wide, a hairline subtitle, the
highlights as light chips, the version big in a corner and a scalloped Material shape
holding the app icon. Behind them, Material 3 Expressive shapes (cookie, pill, clover)
in the app's greys, blurred into light, with a halftone of dots that shows only where
that light falls, and a fine grain. Needs numpy and Pillow.

The words come from the release's notes, `.github/releases/<version>.md` (README
there), the version from `app/build.gradle.kts`. CI runs it for every release
(release.yml):

    python3 tools/make-release-banner.py [--version 1.7.0] [--out path.png]
"""
import argparse
import math
import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
NOTES = ROOT / ".github" / "releases"
OUT = ROOT / ".github" / "assets" / "releases"
LOGO = ROOT / "app" / "src" / "main" / "res" / "mipmap-xxxhdpi" / "ic_launcher.webp"
FONT = ROOT / "tools" / "fonts" / "google_sans_flex.ttf"

FOOTER = "github.com/holyschool/app"
NAME = "edupage 2"

W, H = 1920, 1080
S = 2  # drawn at twice the size, then scaled down: smooth curves and hairlines
L = 8  # the light is drawn at 1/L of the final size: it is all blur anyway

# The app is grayscale: #131313, #292727, #393939 and #d4d4d4, with white for text.
BASE = np.array((19, 19, 19), np.float32)      # #131313
LIGHT = (212, 212, 212)                         # #d4d4d4
INK = (19, 19, 19)                              # #131313
TEXT = (242, 242, 242)                          # near white, a little soft
DIM = (168, 168, 168)


def face(size, wdth=100, wght=400, rond=0, slnt=0):
    f = ImageFont.truetype(str(FONT), size)
    f.set_variation_by_axes([wdth, wght, rond, slnt])
    return f


def app_version():
    text = (ROOT / "app" / "build.gradle.kts").read_text()
    return re.search(r'versionName\s*=\s*"([^"]+)"', text).group(1)


def read_notes(version):
    """`subtitle:` and `- highlight` lines above the first `---`; the release text below it."""
    path = NOTES / f"{version}.md"
    if not path.exists():
        sys.exit(
            f"{path} is missing: write a 'subtitle:' line and up to four '- ' highlights "
            "above '---' before tagging a release"
        )
    head, _, _ = path.read_text().partition("\n---")
    subtitle, highlights = "", []
    for line in head.splitlines():
        line = line.strip()
        if line.lower().startswith("subtitle:"):
            subtitle = line.split(":", 1)[1].strip()
        elif line.startswith("- "):
            highlights.append(line[2:].strip())
    if not subtitle or not highlights:
        sys.exit(f"{path}: needs a 'subtitle:' line and '- ' highlights above '---'")
    return subtitle, highlights


# ── Shapes ─────────────────────────────────────────────────────────────

def polar(cx, cy, r, f, n=720, sx=1.0):
    return [(cx + r * f(t) * math.cos(t) * sx, cy + r * f(t) * math.sin(t)) for t in (i / n * 2 * math.pi for i in range(n))]


def cookie(cx, cy, r, lobes=12, depth=0.075, sx=1.0):
    """The M3E scalloped cookie: a circle with soft, even lobes."""
    return polar(cx, cy, r, lambda t: 1 + depth * math.cos(lobes * t), sx=sx)


def clover(cx, cy, r):
    """M3E's four-leaf clover."""
    return polar(cx, cy, r, lambda t: 0.78 + 0.22 * math.cos(4 * t) ** 2)


def gradient(w, h, c0, c1, angle):
    """A linear gradient from c0 to c1 across the canvas at [angle] (radians)."""
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    t = (xs / w - 0.5) * math.cos(angle) + (ys / h - 0.5) * math.sin(angle)
    t = np.clip(t * 1.4 + 0.5, 0, 1)[..., None]
    return np.array(c0, np.float32) * (1 - t) + np.array(c1, np.float32) * t


def light():
    """The blurred shapes, as linear light at 1/L size: (h, w, 3), 0–255."""
    w, h = W // L, H // L
    acc = np.zeros((h, w, 3), np.float32)
    # (outline, colour from, colour to, angle, blur in final px, strength, where the light
    # falls on it in canvas fractions: the mass is brightest there and fades across)
    shapes = [
        # A great cookie rising behind the version, the brightest mass, bleeding off the corner.
        (cookie(w * 0.80, h * 0.92, h * 0.50), (57, 57, 57), (90, 88, 88), -2.3, 34, 0.88, (0.66, 0.62)),
        # A pill leaning across the top left, cooler.
        (None, (41, 39, 39), (57, 57, 57), 0.4, 40, 0.68, (0.04, 0.36)),
        # A clover low on the left, warmer, under the footer.
        (clover(w * 0.18, h * 1.05, h * 0.30), (80, 78, 78), (41, 39, 39), 1.2, 30, 0.62, (0.26, 0.86)),
        # A small cookie at the top edge between the title and the icon.
        (cookie(w * 0.63, h * 0.02, h * 0.16, lobes=9, depth=0.1), (57, 57, 57), (80, 78, 78), 1.6, 26, 0.52, (0.60, 0.10)),
    ]
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    for outline, c0, c1, angle, blur, strength, (lx, ly) in shapes:
        mask = Image.new("L", (w, h), 0)
        d = ImageDraw.Draw(mask)
        if outline is None:
            # The pill: a rounded bar, rotated.
            bar = Image.new("L", (int(w * 0.62), int(h * 0.24)), 0)
            ImageDraw.Draw(bar).rounded_rectangle((0, 0, bar.width - 1, bar.height - 1), bar.height // 2, fill=255)
            bar = bar.rotate(-14, expand=True, resample=Image.BICUBIC)
            mask.paste(bar, (int(-w * 0.12), int(h * 0.18)), bar)
        else:
            d.polygon(outline, fill=255)
        mask = mask.filter(ImageFilter.GaussianBlur(blur / L))
        m = np.asarray(mask, np.float32)[..., None] / 255.0
        lit = np.exp(-(((xs / w - lx) * 1.78) ** 2 + (ys / h - ly) ** 2) / (2 * 0.22 ** 2))[..., None]
        colour = gradient(w, h, c0, c1, angle)
        # Toward white where it is lit: the bright edge of the glows.
        colour += (255 - colour) * lit * 0.16
        acc += colour * m * strength * (0.35 + 0.65 * lit)
    return acc


def upscale(arr, size):
    """A float image scaled up smoothly, channel by channel."""
    out = [np.asarray(Image.fromarray(arr[..., c]).resize(size, Image.BICUBIC)) for c in range(arr.shape[2])]
    return np.stack(out, axis=-1)


def halftone(glow, pitch):
    """Dots on a grid, each as large as the light under it: (h, w) coverage, 0–1."""
    h, w = glow.shape
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    # Every other row half a pitch over: the hexagonal screen.
    row = np.floor(ys / pitch)
    xs = xs + (row % 2) * pitch / 2
    dx = (xs % pitch) - pitch / 2
    dy = (ys % pitch) - pitch / 2
    dist = np.sqrt(dx * dx + dy * dy)
    r = pitch * 0.36 * np.clip(glow, 0, 1) ** 0.8
    return np.clip(r - dist + 0.75, 0, 1) * (r > 0.4)


def background():
    w, h = W * S, H * S
    glow = upscale(light(), (w, h))
    base = BASE + glow
    # The texture lives in the light: its dots take a lighter tint of what is under them.
    # Dots grow with the light and vanish in the dark, so the texture traces the shapes.
    lum = glow.max(axis=2) / 160.0
    dots = halftone(np.clip(lum, 0, 1) ** 1.3, pitch=13.0 * S)
    tint = np.clip(glow * 1.8 + 30, 0, 255)
    base += (tint - base) * (dots * 0.30 * np.clip(lum * 1.5, 0, 1))[..., None]
    # Grain: fine, monochrome, the same everywhere (seeded: CI and a local run match).
    rng = np.random.default_rng(7)
    base += rng.normal(0, 5.0, (h, w, 1)).astype(np.float32)
    return Image.fromarray(np.clip(base, 0, 255).astype(np.uint8)).convert("RGBA")


# ── Foreground ─────────────────────────────────────────────────────────

def fit_text(draw, text, max_final_px, size, **axes):
    """The largest size (final px) at which [text] fits [max_final_px], drawn at S scale."""
    while size > 24:
        f = face(size * S, **axes)
        if draw.textlength(text, font=f) / S <= max_final_px:
            return f
        size -= 2
    return face(size * S, **axes)


def chip_row(draw, x0, y0, max_x, highlights, label_font, chip_font):
    """'Highlights:' then light pills that wrap like text."""
    pad_x, chip_h, gap = 26 * S, 66 * S, 14 * S
    draw.text((x0, y0 + chip_h / 2), "Highlights:", font=label_font, fill=TEXT, anchor="lm")
    x = x0 + draw.textlength("Highlights:", font=label_font) + 26 * S
    y = y0
    for text in highlights + ["+"]:
        tw = draw.textlength(text, font=chip_font)
        cw = tw + pad_x * 2 if text != "+" else chip_h
        if x + cw > max_x:
            x, y = x0, y + chip_h + gap
        draw.rounded_rectangle((x, y, x + cw, y + chip_h), chip_h / 2, fill=LIGHT)
        draw.text((x + cw / 2, y + chip_h / 2 + 1 * S), text, font=chip_font, fill=INK, anchor="mm")
        x += cw + gap
    return y + chip_h


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--version", default=None, help="defaults to app/build.gradle.kts's versionName")
    ap.add_argument("--out", default=None, help=f"defaults to {OUT.relative_to(ROOT)}/edupage-<version>.png")
    args = ap.parse_args()
    version = args.version or app_version()
    subtitle, highlights = read_notes(version)

    img = background()
    draw = ImageDraw.Draw(img)
    m = 96 * S

    # The icon on a light cookie, top right, with a soft bloom of its own colour.
    ccx, ccy, cr = W * S - 250 * S, 250 * S, 205 * S
    bloom = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(bloom).polygon(cookie(ccx, ccy, cr * 1.05), fill=LIGHT + (110,))
    img.alpha_composite(bloom.filter(ImageFilter.GaussianBlur(60 * S)))
    shapes = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(shapes).polygon(cookie(ccx, ccy, cr), fill=LIGHT + (255,))
    img.alpha_composite(shapes)
    logo = Image.open(LOGO).convert("RGBA").resize((int(cr * 1.18),) * 2, Image.LANCZOS)
    mask = Image.new("L", logo.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, logo.size[0] - 1, logo.size[1] - 1), logo.size[0] * 0.26, fill=255)
    logo.putalpha(mask)
    img.alpha_composite(logo, (int(ccx - logo.size[0] / 2), int(ccy - logo.size[1] / 2)))

    # Name and subtitle.
    name_font = fit_text(draw, NAME, 1270, 210, wdth=132, wght=860, rond=100)
    draw.text((m - 6 * S, 78 * S), NAME, font=name_font, fill=TEXT, anchor="lt")
    sub_font = fit_text(draw, subtitle, 1300, 104, wdth=151, wght=230, rond=0)
    draw.text((m, 318 * S), subtitle, font=sub_font, fill=TEXT, anchor="lt")

    # Highlights: the left column, clear of the version in the right one.
    chip_row(draw, m, 500 * S, 1180 * S, highlights, face(40 * S, wdth=110, wght=820, rond=100), face(32 * S, wdth=100, wght=520, rond=100))

    # Footer pill: the icon and where to get it.
    fy, fh = H * S - m - 50 * S, 92 * S
    foot = face(36 * S, wdth=125, wght=560, rond=100)
    fw = fh + 24 * S + draw.textlength(FOOTER, font=foot) + 44 * S
    draw.rounded_rectangle((m - 20 * S, fy, m - 20 * S + fw, fy + fh), fh / 2, fill=LIGHT)
    small = Image.open(LOGO).convert("RGBA").resize((fh - 20 * S,) * 2, Image.LANCZOS)
    sm = Image.new("L", small.size, 0)
    ImageDraw.Draw(sm).ellipse((0, 0, small.size[0] - 1, small.size[1] - 1), fill=255)
    small.putalpha(sm)
    img.alpha_composite(small, (m - 10 * S, fy + 10 * S))
    draw.text((m - 10 * S + fh + 14 * S, fy + fh / 2), FOOTER, font=foot, fill=INK, anchor="lm")

    # The version, big in the bottom right, and its line.
    right = W * S - m + 10 * S
    draw.text((right, H * S - m - 66 * S), "v" + version, font=face(190 * S, wdth=132, wght=860, rond=100), fill=LIGHT, anchor="rs")
    # The font has no arrow: draw one at the end of the line.
    ay = H * S - m + 16 * S
    ax1 = right - 8 * S
    ax0 = ax1 - 34 * S
    cy = ay - 15 * S
    draw.line((ax0, cy, ax1, cy), fill=TEXT, width=4 * S)
    draw.line((ax1 - 14 * S, cy - 13 * S, ax1, cy, ax1 - 14 * S, cy + 13 * S), fill=TEXT, width=4 * S, joint="curve")
    draw.text((ax0 - 16 * S, ay), "Update in the app or on GitHub", font=face(38 * S, wdth=125, wght=520, rond=100), fill=TEXT, anchor="rs")

    out = img.resize((W, H), Image.LANCZOS).convert("RGB")
    path = Path(args.out) if args.out else OUT / f"edupage-{version}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    out.save(path, optimize=True)
    print(path)


if __name__ == "__main__":
    main()