#!/usr/bin/env python3
"""Builds the server selector's Nexo glyphs from Kenney's CC0 asset packs.

Run from anywhere:  python tools/build_selector_assets.py
Needs Pillow:       python -m pip install -r tools/requirements.txt

Writes:
  nexo/glyphs/selector.yml
  nexo/pack/assets/minecraft/textures/selector/*.png
  nexo/pack/assets/minecraft/textures/gui/sprites/hud/{crosshair,experience_bar_background,
    experience_bar_progress,locator_bar_background}.png
  nexo/LICENSE-kenney.txt
Regenerate rather than editing the outputs: the plugin's Glyph enum and layout assume these
characters and sizes.
"""
import io
import urllib.request
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
NEXO = ROOT / "nexo"
TEXTURES = NEXO / "pack" / "assets" / "minecraft" / "textures" / "selector"
CACHE = ROOT / "tools" / ".cache"

# Full-screen tint over the lobby world behind the selector; tune colour and opacity here
TINT_COLOR = (0x33, 0x33, 0x33)
TINT_OPACITY = 0.65

PACKS = {
    "cursor": "https://kenney.nl/media/pages/assets/cursor-pack/461b29df18-1717599281/kenney_cursor-pack.zip",
    "ui": "https://kenney.nl/media/pages/assets/ui-pack/f651646eab-1718203990/kenney_ui-pack.zip",
    "icons": "https://kenney.nl/media/pages/assets/game-icons/1ebf9c14af-1677661579/kenney_game-icons.zip",
}


def load_pack(name):
    CACHE.mkdir(parents=True, exist_ok=True)
    path = CACHE / f"{name}.zip"
    if not path.exists():
        request = urllib.request.Request(PACKS[name], headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(request) as response:
            path.write_bytes(response.read())
    return zipfile.ZipFile(path)


def kenney(pack, member):
    with load_pack(pack) as archive:
        return Image.open(io.BytesIO(archive.read(member))).convert("RGBA")


def button_card(width, height, fill, border, lip):
    # Kenney button style: the lip colour peeks out under the face as the button's 3D edge
    image = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((0, 0, width - 1, height - 1), radius=8, fill=lip)
    draw.rounded_rectangle((0, 0, width - 1, height - 1 - CARD_LIP), radius=8, fill=fill, outline=border, width=2)
    return image


def solid(width, height, color):
    return Image.new("RGBA", (width, height), color)


# (name, char code, rendered height in px, image factory)
GLYPHS = [
    ("cursor_arrow", 0xE300, 16, lambda: kenney("cursor", "PNG/Outline/Default/pointer_b.png")),
    ("cursor_hand", 0xE301, 16, lambda: kenney("cursor", "PNG/Outline/Default/hand_point.png")),
    ("tint", 0xE308, 36, lambda: solid(64, 36, (*TINT_COLOR, round(255 * TINT_OPACITY)))),
    ("play", 0xE30D, 40, lambda: kenney("ui", "PNG/Green/Default/button_rectangle_depth_flat.png")),
    ("play_hover", 0xE30E, 40, lambda: kenney("ui", "PNG/Green/Default/button_rectangle_depth_gradient.png")),
    ("play_disabled", 0xE30F, 40, lambda: kenney("ui", "PNG/Grey/Default/button_rectangle_depth_flat.png")),
    ("icon_sword", 0xE310, 32, lambda: kenney("cursor", "PNG/Outline/Default/tool_sword_a.png")),
    ("icon_pickaxe", 0xE311, 32, lambda: kenney("cursor", "PNG/Outline/Default/tool_pickaxe.png")),
    ("icon_axe", 0xE312, 32, lambda: kenney("cursor", "PNG/Outline/Default/tool_axe.png")),
    ("icon_bow", 0xE313, 32, lambda: kenney("cursor", "PNG/Outline/Default/tool_bow.png")),
    ("icon_hammer", 0xE314, 32, lambda: kenney("cursor", "PNG/Outline/Default/tool_hammer.png")),
    ("icon_wand", 0xE315, 32, lambda: kenney("cursor", "PNG/Outline/Default/tool_wand.png")),
    ("icon_bomb", 0xE316, 32, lambda: kenney("cursor", "PNG/Outline/Default/tool_bomb.png")),
    ("icon_torch", 0xE317, 32, lambda: kenney("cursor", "PNG/Outline/Default/tool_torch.png")),
    ("icon_star", 0xE318, 32, lambda: kenney("icons", "PNG/White/2x/star.png")),
]

# Cards come pre-rendered in a grid of sizes so the plugin can pick the smallest one that fits its text.
# char = 0xE320 + style * 12 + width index * 3 + height index; the plugin's CardArt relies on this.
CARD_LIP = 6
CARD_WIDTHS = (128, 160, 192, 224)
CARD_HEIGHTS = (144, 164, 184)
CARD_STYLES = (
    ("normal", (24, 28, 38, 220), (90, 100, 120, 255), (52, 58, 72, 255)),
    ("selected", (26, 40, 28, 225), (115, 205, 75, 255), (62, 128, 38, 255)),
)
for _style, (_label, _fill, _border, _lip) in enumerate(CARD_STYLES):
    for _wi, _w in enumerate(CARD_WIDTHS):
        for _hi, _h in enumerate(CARD_HEIGHTS):
            GLYPHS.append((f"card_{_label}_{_w}x{_h}", 0xE320 + _style * 12 + _wi * 3 + _hi, _h,
                           lambda w=_w, h=_h, f=_fill, b=_border, l=_lip: button_card(w, h, f, b, l)))

# Vanilla HUD sprites hidden on the lobby: the selector has its own cursor and nothing to level
HIDDEN_HUD = [("crosshair", 15, 15), ("experience_bar_background", 182, 5),
              ("experience_bar_progress", 182, 5), ("locator_bar_background", 182, 5)]
HUD = NEXO / "pack" / "assets" / "minecraft" / "textures" / "gui" / "sprites" / "hud"


def config_text():
    lines = [
        "# Generated by tools/build_selector_assets.py - edit the script, not this file.",
        "#",
        "# ascent = height / 2 + 3 centres every glyph on the text line, so the plugin can",
        "# position glyphs and text the same way. char is a \\uXXXX escape because Nexo reads",
        "# this file with the platform charset, which mangles literal private-use characters.",
        "",
    ]
    for name, code, height, _ in GLYPHS:
        lines += [
            f"selector_{name}:",
            f"  texture: selector/{name}",
            f"  ascent: {height // 2 + 3}",
            f"  height: {height}",
            f'  char: "\\u{code:04X}"',
            "",
        ]
    return "\n".join(lines)


def main():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    for name, _, _, make in GLYPHS:
        make().save(TEXTURES / f"{name}.png")
    HUD.mkdir(parents=True, exist_ok=True)
    for name, w, h in HIDDEN_HUD:
        solid(w, h, (0, 0, 0, 0)).save(HUD / f"{name}.png")
    (NEXO / "glyphs").mkdir(parents=True, exist_ok=True)
    (NEXO / "glyphs" / "selector.yml").write_text(config_text(), encoding="utf-8", newline="\n")
    with load_pack("cursor") as archive:
        (NEXO / "LICENSE-kenney.txt").write_bytes(archive.read("License.txt"))
    print(f"Wrote {len(GLYPHS)} glyphs to {NEXO}")


if __name__ == "__main__":
    main()
