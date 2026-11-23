#!/usr/bin/env python3
"""manifest.json 이 가리키는 플래티콘 아이콘으로 안드로이드 런처 아이콘 세트를 만든다.

- res/mipmap-{mdpi..xxxhdpi}/ic_launcher.png, ic_launcher_round.png (레거시)
- res/drawable/ic_launcher_foreground.png (적응형 전경 432x432 = 108dp@xxxhdpi)
"""
from PIL import Image, ImageDraw
import os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RES = os.path.join(ROOT, "app/src/main/res")
SRC = sys.argv[1] if len(sys.argv) > 1 else "/workspace/work/tt-icon/original.png"

BG = (255, 255, 255, 255)
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def rounded_mask(size, radius_ratio=0.22):
    m = Image.new("L", (size * 4, size * 4), 0)
    d = ImageDraw.Draw(m)
    r = int(size * 4 * radius_ratio)
    d.rounded_rectangle([0, 0, size * 4 - 1, size * 4 - 1], radius=r, fill=255)
    return m.resize((size, size), Image.LANCZOS)


def circle_mask(size):
    m = Image.new("L", (size * 4, size * 4), 0)
    d = ImageDraw.Draw(m)
    d.ellipse([0, 0, size * 4 - 1, size * 4 - 1], fill=255)
    return m.resize((size, size), Image.LANCZOS)


def legacy(size, glyph_ratio, mask_fn):
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    bg = Image.new("RGBA", (size, size), BG)
    inner = int(size * glyph_ratio)
    g = src.resize((inner, inner), Image.LANCZOS)
    off = (size - inner) // 2
    bg.alpha_composite(g, (off, off))
    canvas.paste(bg, (0, 0), mask_fn(size))
    return canvas


src = Image.open(SRC).convert("RGBA")

# 적응형 전경: 108dp 캔버스에서 안전 영역(66%) 안에 글리프
fg_size = 432
fg = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
inner = int(fg_size * 0.60)
g = src.resize((inner, inner), Image.LANCZOS)
fg.alpha_composite(g, ((fg_size - inner) // 2, (fg_size - inner) // 2))
os.makedirs(os.path.join(RES, "drawable"), exist_ok=True)
fg.save(os.path.join(RES, "drawable/ic_launcher_foreground.png"))

for name, size in DENSITIES.items():
    d = os.path.join(RES, "mipmap-" + name)
    os.makedirs(d, exist_ok=True)
    legacy(size, 0.82, rounded_mask).save(os.path.join(d, "ic_launcher.png"))
    legacy(size, 0.82, circle_mask).save(os.path.join(d, "ic_launcher_round.png"))
    print(name, size, "ok")

print("foreground", fg_size, "ok")
