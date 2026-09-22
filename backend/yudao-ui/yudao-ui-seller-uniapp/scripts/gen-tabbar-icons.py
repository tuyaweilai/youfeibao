#!/usr/bin/env python3
"""生成底部导航（tabBar）图标。

原生 tabBar 只吃图片路径（不支持字体图标），图标是二进制，改起来看不见摸不着，
所以生成脚本入库、产物一并入库：改配色或形状时重跑本脚本即可。

    python3 scripts/gen-tabbar-icons.py

输出 `src/static/tabbar/`：每个 tab 两张 81×81 的 PNG（常态灰 / 选中蓝），
81×81 是微信小程序 tabBar 的推荐尺寸。
"""

from pathlib import Path

from PIL import Image, ImageDraw

SIZE = 81
NORMAL = (152, 162, 179, 255)  # $seller-text-muted #98a2b3
ACTIVE = (22, 119, 255, 255)   # $seller-primary #1677ff
OUT = Path(__file__).resolve().parent.parent / "src" / "static" / "tabbar"


def house(draw: ImageDraw.ImageDraw, color) -> None:
    """首页：屋顶 + 屋身（实心）。"""
    draw.polygon([(40, 10), (72, 38), (63, 38), (63, 70), (17, 70), (17, 38), (8, 38)], fill=color)


def exchange(draw: ImageDraw.ImageDraw, color) -> None:
    """交易：一对反向箭头（收进来 / 付出去）。"""
    draw.line([(16, 30), (58, 30)], fill=color, width=9)
    draw.polygon([(56, 18), (74, 30), (56, 42)], fill=color)
    draw.line([(65, 54), (23, 54)], fill=color, width=9)
    draw.polygon([(25, 42), (7, 54), (25, 66)], fill=color)


def person(draw: ImageDraw.ImageDraw, color) -> None:
    """我的：头 + 肩。"""
    draw.ellipse([27, 12, 53, 38], fill=color)
    draw.pieslice([13, 44, 67, 98], 180, 360, fill=color)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, paint in (("home", house), ("transaction", exchange), ("my", person)):
        for suffix, color in (("", NORMAL), ("-active", ACTIVE)):
            image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
            paint(ImageDraw.Draw(image), color)
            image.save(OUT / f"{name}{suffix}.png")
            print(f"wrote {OUT / f'{name}{suffix}.png'}")


if __name__ == "__main__":
    main()
