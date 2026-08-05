from pathlib import Path
from PIL import Image, ImageDraw


root = Path(__file__).resolve().parent
pages = sorted(root.glob("page-*.png"))
per_sheet = 8
thumb_width = 300
gap = 16
label_height = 24

for start in range(0, len(pages), per_sheet):
    batch = pages[start:start + per_sheet]
    thumbs = []
    for page in batch:
        with Image.open(page) as source:
            ratio = thumb_width / source.width
            thumb = source.convert("RGB").resize(
                (thumb_width, int(source.height * ratio)), Image.Resampling.LANCZOS
            )
        thumbs.append((page, thumb))

    cell_height = max(image.height for _, image in thumbs) + label_height
    sheet = Image.new("RGB", (4 * thumb_width + 5 * gap, 2 * cell_height + 3 * gap), "#D7DEE8")
    draw = ImageDraw.Draw(sheet)
    for index, (page, thumb) in enumerate(thumbs):
        column = index % 4
        row = index // 4
        x = gap + column * (thumb_width + gap)
        y = gap + row * (cell_height + gap)
        sheet.paste(thumb, (x, y + label_height))
        draw.text((x, y + 3), page.stem, fill="#111827")

    first = start + 1
    last = start + len(batch)
    sheet.save(root / f"contact-{first:02d}-{last:02d}.png", optimize=True)
