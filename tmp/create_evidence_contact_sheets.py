from __future__ import annotations

import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


def main() -> int:
    if len(sys.argv) != 3:
        raise SystemExit("usage: create_evidence_contact_sheets.py INPUT_DIR OUTPUT_DIR")

    source = Path(sys.argv[1])
    output = Path(sys.argv[2])
    output.mkdir(parents=True, exist_ok=True)
    files = sorted(source.glob("*.png"))
    if not files:
        raise SystemExit(f"no PNG files found in {source}")

    columns = 3
    rows = 4
    cell_width = 420
    cell_height = 300
    label_height = 34
    page_size = columns * rows
    font = ImageFont.load_default(size=15)

    for page_index in range(math.ceil(len(files) / page_size)):
        sheet = Image.new("RGB", (columns * cell_width, rows * cell_height), "white")
        draw = ImageDraw.Draw(sheet)
        page_files = files[page_index * page_size : (page_index + 1) * page_size]
        for index, path in enumerate(page_files):
            row, column = divmod(index, columns)
            left = column * cell_width
            top = row * cell_height
            with Image.open(path) as source_image:
                preview = source_image.convert("RGB")
                preview.thumbnail((cell_width - 16, cell_height - label_height - 16))
                image_left = left + (cell_width - preview.width) // 2
                image_top = top + label_height + (cell_height - label_height - preview.height) // 2
                sheet.paste(preview, (image_left, image_top))
            draw.rectangle(
                (left, top, left + cell_width - 1, top + cell_height - 1),
                outline="#9aa0a6",
                width=1,
            )
            draw.text((left + 8, top + 8), path.name, fill="#111111", font=font)

        target = output / f"contact-sheet-{page_index + 1:02d}.png"
        sheet.save(target, optimize=True)
        print(target)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
