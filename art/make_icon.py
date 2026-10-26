#!/usr/bin/env python3
"""Renders the launcher icon (res/drawable/ic_launcher_foreground.xml on @color/icon_bg) as art/icon-512.png
for Void Store: the adaptive icon's 72-unit safe zone, one leaf pixel (3 units) = 21 px, on a soft glow.

    python3 art/make_icon.py
"""
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
SIZE, CELL, ORIGIN = 512, 21, 19  # 24 cells of 3 units from x/y = 19 (the safe zone 18..90)

bg = re.search(r'name="icon_bg">#FF([0-9A-Fa-f]{6})', (RES / "values/themes.xml").read_text()).group(1)
fg = (RES / "drawable/ic_launcher_foreground.xml").read_text()

im = Image.new("RGB", (SIZE, SIZE), "#" + bg)
glow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
ImageDraw.Draw(glow).ellipse((96, 80, 416, 400), fill=(120, 200, 80, 70))
im.paste(glow.filter(ImageFilter.GaussianBlur(60)), (0, 0), glow.filter(ImageFilter.GaussianBlur(60)))

d = ImageDraw.Draw(im)
pad = (SIZE - 24 * CELL) // 2
for color, data in re.findall(r'fillColor="#FF([0-9A-Fa-f]{6})" android:pathData="([^"]+)"', fg):
    for x, y in re.findall(r"M(\d+),(\d+)h3v3h-3z", data):
        cx, cy = (int(x) - ORIGIN) // 3, (int(y) - ORIGIN) // 3
        x0, y0 = pad + cx * CELL, pad + cy * CELL
        d.rectangle((x0, y0, x0 + CELL - 1, y0 + CELL - 1), fill="#" + color)

im.save(ROOT / "art/icon-512.png")
print("art/icon-512.png")
