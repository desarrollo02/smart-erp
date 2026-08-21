#!/usr/bin/env python3
"""Generate the purchasing-request support HTML and PDF from canonical Markdown."""

from __future__ import annotations

import argparse
import html
import re
from pathlib import Path

from PIL import Image as PilImage
from reportlab import rl_config
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase.pdfmetrics import stringWidth
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    Image,
    KeepTogether,
    ListFlowable,
    ListItem,
    LongTable,
    PageBreak,
    PageTemplate,
    Paragraph,
    Preformatted,
    Spacer,
    TableStyle,
)
from reportlab.platypus.tableofcontents import TableOfContents

rl_config.invariant = 1

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE = ROOT / "docs/user-guide/modules/solicitudes-compra-soporte.md"
DEFAULT_HTML = ROOT / "docs/user-guide/modules/web/solicitudes-compra-soporte.html"
DEFAULT_PDF = ROOT / "docs/output/pdf/manual-soporte-solicitudes-compra.pdf"

TEAL = colors.HexColor("#006A64")
TEAL_DARK = colors.HexColor("#173E3A")
TEAL_PALE = colors.HexColor("#D4EFEC")
INK = colors.HexColor("#172321")
MUTED = colors.HexColor("#52615F")
SURFACE = colors.HexColor("#F4F8F7")
LINE = colors.HexColor("#B8C8C5")
ERROR = colors.HexColor("#BA1A1A")


def inline_markup(value: str) -> str:
    escaped = html.escape(value, quote=False)
    escaped = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", escaped)
    escaped = re.sub(r"`([^`]+)`", r'<font name="Courier">\1</font>', escaped)
    return escaped


def ascii_code(value: str) -> str:
    replacements = str.maketrans({
        "┌": "+", "┐": "+", "└": "+", "┘": "+", "├": "+", "┤": "+",
        "┬": "+", "┴": "+", "┼": "+", "─": "-", "│": "|", "▼": "v",
        "→": "->", "←": "<-", "·": "-", "“": '"', "”": '"', "’": "'",
    })
    return value.translate(replacements)


def parse_blocks(lines: list[str]):
    index = 0
    while index < len(lines):
        line = lines[index].rstrip()
        if not line:
            index += 1
            continue
        if line.startswith("```"):
            language = line[3:].strip()
            index += 1
            body = []
            while index < len(lines) and not lines[index].startswith("```"):
                body.append(lines[index].rstrip("\n"))
                index += 1
            index += 1
            yield ("code", language, "\n".join(body))
            continue
        image_match = re.fullmatch(r"!\[(.+)]\((.+)\)", line)
        if image_match:
            yield ("image", image_match.group(1), image_match.group(2))
            index += 1
            continue
        heading = re.match(r"^(#{1,4})\s+(.+)$", line)
        if heading:
            yield ("heading", len(heading.group(1)), heading.group(2))
            index += 1
            continue
        if line.startswith("|") and index + 1 < len(lines) and re.match(
                r"^\|?\s*:?-+", lines[index + 1].strip()):
            rows = []
            rows.append([cell.strip() for cell in line.strip("|").split("|")])
            index += 2
            while index < len(lines) and lines[index].strip().startswith("|"):
                rows.append([cell.strip() for cell in lines[index].strip().strip("|").split("|")])
                index += 1
            yield ("table", rows)
            continue
        if re.match(r"^[-*]\s+", line):
            items = []
            while index < len(lines) and re.match(r"^[-*]\s+", lines[index].rstrip()):
                items.append(re.sub(r"^[-*]\s+", "", lines[index].rstrip()))
                index += 1
            yield ("list", False, items)
            continue
        if re.match(r"^\d+\.\s+", line):
            items = []
            while index < len(lines) and re.match(r"^\d+\.\s+", lines[index].rstrip()):
                items.append(re.sub(r"^\d+\.\s+", "", lines[index].rstrip()))
                index += 1
            yield ("list", True, items)
            continue
        if line.startswith(">"):
            parts = []
            while index < len(lines) and lines[index].rstrip().startswith(">"):
                parts.append(lines[index].rstrip()[1:].strip())
                index += 1
            yield ("quote", " ".join(parts))
            continue
        parts = [line]
        index += 1
        while index < len(lines):
            candidate = lines[index].rstrip()
            if (not candidate or candidate.startswith(("#", "```", "|", ">", "!["))
                    or re.match(r"^[-*]\s+", candidate)
                    or re.match(r"^\d+\.\s+", candidate)):
                break
            parts.append(candidate)
            index += 1
        yield ("paragraph", " ".join(parts))


class ManualDocTemplate(BaseDocTemplate):
    def __init__(self, filename: Path, styles):
        super().__init__(
            str(filename), pagesize=A4, leftMargin=19 * mm, rightMargin=19 * mm,
            topMargin=20 * mm, bottomMargin=18 * mm,
            title="Manual de usuario y soporte - Solicitudes de compra",
            author="Smart ERP", subject="Compras - Solicitudes de compra",
        )
        self.styles = styles
        frame = Frame(self.leftMargin, self.bottomMargin, self.width, self.height, id="normal")
        self.addPageTemplates(PageTemplate(id="manual", frames=frame, onPage=self._decorate))

    def _decorate(self, canvas, doc):
        if doc.page == 1:
            return
        canvas.saveState()
        canvas.setStrokeColor(LINE)
        canvas.line(self.leftMargin, A4[1] - 13 * mm, A4[0] - self.rightMargin, A4[1] - 13 * mm)
        canvas.setFillColor(TEAL_DARK)
        canvas.setFont("Helvetica-Bold", 8)
        canvas.drawString(self.leftMargin, A4[1] - 10 * mm, "SMART ERP · SOPORTE · SOLICITUDES DE COMPRA")
        canvas.setFillColor(MUTED)
        canvas.setFont("Helvetica", 8)
        footer = f"Versión 1.0 · 20/08/2026                                      Página {doc.page}"
        canvas.drawString(self.leftMargin, 9 * mm, footer)
        canvas.restoreState()

    def afterFlowable(self, flowable):
        if not isinstance(flowable, Paragraph):
            return
        level = getattr(flowable, "toc_level", None)
        if level is None:
            return
        text = flowable.getPlainText()
        key = f"heading-{self.seq.nextf('heading')}"
        self.canv.bookmarkPage(key)
        self.canv.addOutlineEntry(text, key, level=level, closed=False)
        self.notify("TOCEntry", (level, text, self.page, key))


def pdf_styles():
    sample = getSampleStyleSheet()
    styles = {
        "body": ParagraphStyle("Body", parent=sample["BodyText"], fontName="Helvetica",
                               fontSize=9.1, leading=12.6, textColor=INK, spaceAfter=5),
        "small": ParagraphStyle("Small", parent=sample["BodyText"], fontName="Helvetica",
                                fontSize=7.4, leading=9.6, textColor=INK),
        "cover_title": ParagraphStyle("CoverTitle", parent=sample["Title"], fontName="Helvetica-Bold",
                                      fontSize=27, leading=31, textColor=TEAL_DARK, alignment=TA_LEFT,
                                      spaceAfter=8),
        "cover_sub": ParagraphStyle("CoverSub", parent=sample["BodyText"], fontName="Helvetica",
                                    fontSize=12, leading=17, textColor=MUTED),
        "h1": ParagraphStyle("H1", parent=sample["Heading1"], fontName="Helvetica-Bold",
                             fontSize=18, leading=22, textColor=TEAL_DARK, spaceBefore=10,
                             spaceAfter=7, keepWithNext=True),
        "h2": ParagraphStyle("H2", parent=sample["Heading2"], fontName="Helvetica-Bold",
                             fontSize=14, leading=17, textColor=TEAL, spaceBefore=10,
                             spaceAfter=6, keepWithNext=True),
        "h3": ParagraphStyle("H3", parent=sample["Heading3"], fontName="Helvetica-Bold",
                             fontSize=11, leading=14, textColor=TEAL_DARK, spaceBefore=8,
                             spaceAfter=4, keepWithNext=True),
        "example": ParagraphStyle("Example", parent=sample["BodyText"], fontName="Helvetica-Bold",
                                  fontSize=9.4, leading=12, textColor=TEAL_DARK, backColor=TEAL_PALE,
                                  borderColor=TEAL, borderWidth=.6, borderPadding=5,
                                  spaceBefore=6, spaceAfter=5, keepWithNext=True),
        "quote": ParagraphStyle("Quote", parent=sample["BodyText"], fontName="Helvetica",
                                fontSize=8.5, leading=11.5, textColor=TEAL_DARK,
                                backColor=SURFACE, borderColor=LINE, borderWidth=.6,
                                borderPadding=7, leftIndent=8, rightIndent=8, spaceAfter=7),
        "caption": ParagraphStyle("Caption", parent=sample["BodyText"], fontName="Helvetica-Oblique",
                                  fontSize=7.5, leading=9, textColor=MUTED, alignment=TA_CENTER,
                                  spaceBefore=3, spaceAfter=8),
        "code": ParagraphStyle("Code", fontName="Courier", fontSize=6.6, leading=8.1,
                               textColor=INK, backColor=SURFACE, borderColor=LINE,
                               borderWidth=.5, borderPadding=6, spaceBefore=4, spaceAfter=7),
    }
    return styles


def make_table(rows: list[list[str]], styles):
    columns = max(len(row) for row in rows)
    normalized = [row + [""] * (columns - len(row)) for row in rows]
    if columns > 4:
        vertical = []
        headers = normalized[0]
        for record in normalized[1:]:
            for key, value in zip(headers, record):
                vertical.append([
                    Paragraph(f"<b>{inline_markup(key)}</b>", styles["small"]),
                    Paragraph(inline_markup(value), styles["small"]),
                ])
        table = LongTable(vertical, colWidths=[44 * mm, 128 * mm], hAlign="LEFT")
        table.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (0, -1), TEAL_PALE),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("GRID", (0, 0), (-1, -1), .35, LINE),
            ("ROWBACKGROUNDS", (1, 0), (1, -1), [colors.white, SURFACE]),
            ("LEFTPADDING", (0, 0), (-1, -1), 5),
            ("RIGHTPADDING", (0, 0), (-1, -1), 5),
            ("TOPPADDING", (0, 0), (-1, -1), 4),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
        ]))
        return table
    data = [[Paragraph(inline_markup(cell), styles["small"]) for cell in row]
            for row in normalized]
    width = 172 * mm
    if columns == 2:
        widths = [48 * mm, width - 48 * mm]
    elif columns == 3:
        widths = [42 * mm, 43 * mm, width - 85 * mm]
    elif columns == 4:
        widths = [36 * mm, 32 * mm, 34 * mm, width - 102 * mm]
    else:
        widths = [width / columns] * columns
    table = LongTable(data, colWidths=widths, repeatRows=1, hAlign="LEFT")
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), TEAL_DARK),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), .35, LINE),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, SURFACE]),
        ("LEFTPADDING", (0, 0), (-1, -1), 4),
        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]))
    return table


def add_image(story, source_dir: Path, alt: str, relative: str, styles):
    path = (source_dir / relative).resolve()
    if not path.is_file():
        raise FileNotFoundError(path)
    with PilImage.open(path) as picture:
        width, height = picture.size
    max_width, max_height = 172 * mm, 205 * mm
    scale = min(max_width / width, max_height / height)
    rendered = Image(str(path), width=width * scale, height=height * scale)
    rendered.hAlign = "CENTER"
    story.extend([rendered, Paragraph(inline_markup(alt), styles["caption"])])


def build_pdf(source: Path, output: Path):
    styles = pdf_styles()
    blocks = list(parse_blocks(source.read_text(encoding="utf-8").splitlines()))
    title = next(block[2] for block in blocks if block[0] == "heading" and block[1] == 1)
    story = [Spacer(1, 25 * mm), Paragraph("SMART ERP", styles["h2"]),
             Paragraph(inline_markup(title), styles["cover_title"]),
             Paragraph("Manual operativo y técnico para personal de soporte", styles["cover_sub"]),
             Spacer(1, 12 * mm),
             Paragraph("Compras · una pantalla · modos lista, alta y detalle", styles["example"]),
             Spacer(1, 58 * mm),
             Paragraph("Versión 1.0 · 20 de agosto de 2026", styles["cover_sub"]),
             PageBreak(), Paragraph("Índice", styles["h1"])]
    toc = TableOfContents()
    toc.levelStyles = [
        ParagraphStyle("TOC1", fontName="Helvetica-Bold", fontSize=9.5, leading=13,
                       leftIndent=0, firstLineIndent=0, textColor=TEAL_DARK),
        ParagraphStyle("TOC2", fontName="Helvetica", fontSize=8.5, leading=12,
                       leftIndent=12, firstLineIndent=0, textColor=INK),
    ]
    story.extend([toc, PageBreak()])
    skipping_manual_index = False
    first_title_skipped = False
    for block in blocks:
        kind = block[0]
        if kind == "heading" and block[1] == 1 and not first_title_skipped:
            first_title_skipped = True
            continue
        if kind == "heading" and block[1] == 2 and block[2] == "Índice":
            skipping_manual_index = True
            continue
        if skipping_manual_index:
            if kind == "heading" and block[1] == 2:
                skipping_manual_index = False
            else:
                continue
        if kind == "heading":
            level, text = block[1], block[2]
            style = styles["h1"] if level == 2 else styles["h2"] if level == 3 else styles["h3"]
            paragraph = Paragraph(inline_markup(text), style)
            paragraph.toc_level = 0 if level == 2 else 1
            story.append(paragraph)
        elif kind == "paragraph":
            text = block[1]
            style = styles["example"] if text == "**Ejemplo verificado con datos reales**" else styles["body"]
            story.append(Paragraph(inline_markup(text), style))
        elif kind == "quote":
            story.append(Paragraph(inline_markup(block[1]), styles["quote"]))
        elif kind == "list":
            ordered, items = block[1], block[2]
            for number, item in enumerate(items, start=1):
                marker = f"{number}." if ordered else "-"
                story.append(Paragraph(
                    f"{marker}&nbsp;&nbsp;{inline_markup(item)}",
                    ParagraphStyle(
                        f"List-{number}-{id(items)}", parent=styles["body"],
                        leftIndent=12, firstLineIndent=-8, spaceAfter=2,
                    ),
                ))
        elif kind == "table":
            story.extend([make_table(block[1], styles), Spacer(1, 6)])
        elif kind == "code":
            story.append(Preformatted(ascii_code(block[2]), styles["code"], maxLineLength=112))
        elif kind == "image":
            add_image(story, source.parent, block[1], block[2], styles)
    output.parent.mkdir(parents=True, exist_ok=True)
    ManualDocTemplate(output, styles).multiBuild(story)


def html_inline(value: str) -> str:
    return inline_markup(value).replace('<font name="Courier">', "<code>").replace("</font>", "</code>")


def build_html(source: Path, output: Path):
    blocks = list(parse_blocks(source.read_text(encoding="utf-8").splitlines()))
    body = []
    for block in blocks:
        kind = block[0]
        if kind == "heading":
            level, text = block[1], block[2]
            body.append(f"<h{level}>{html_inline(text)}</h{level}>")
        elif kind == "paragraph":
            css = ' class="verified-example"' if block[1] == "**Ejemplo verificado con datos reales**" else ""
            body.append(f"<p{css}>{html_inline(block[1])}</p>")
        elif kind == "quote":
            body.append(f"<aside>{html_inline(block[1])}</aside>")
        elif kind == "list":
            tag = "ol" if block[1] else "ul"
            body.append(f"<{tag}>" + "".join(f"<li>{html_inline(item)}</li>" for item in block[2]) + f"</{tag}>")
        elif kind == "table":
            rows = block[1]
            body.append("<div class=\"table-shell\"><table><thead><tr>" + "".join(
                f"<th>{html_inline(cell)}</th>" for cell in rows[0]) + "</tr></thead><tbody>")
            for row in rows[1:]:
                body.append("<tr>" + "".join(f"<td>{html_inline(cell)}</td>" for cell in row) + "</tr>")
            body.append("</tbody></table></div>")
        elif kind == "code":
            body.append(f"<pre>{html.escape(block[2])}</pre>")
        elif kind == "image":
            relative = "../" + block[2]
            body.append(f"<figure><img src=\"{html.escape(relative, quote=True)}\" alt=\"{html.escape(block[1], quote=True)}\"/><figcaption>{html.escape(block[1])}</figcaption></figure>")
    document = """<!doctype html>
<html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Manual de soporte - Solicitudes de compra</title>
<style>
:root{--teal:#006a64;--dark:#173e3a;--pale:#d4efec;--ink:#172321;--line:#b8c8c5;--surface:#f4f8f7}
*{box-sizing:border-box}body{margin:0;color:var(--ink);font:16px/1.55 system-ui,Segoe UI,sans-serif;background:#e9efed}
main{max-width:1100px;margin:auto;background:white;padding:clamp(20px,5vw,64px);box-shadow:0 3px 20px #173e3a22}
h1{font-size:clamp(2rem,5vw,3.2rem);color:var(--dark);line-height:1.08}h2{margin-top:2.4rem;color:var(--teal);border-bottom:1px solid var(--line);padding-bottom:.35rem}h3{color:var(--dark);margin-top:1.7rem}
p,li{max-width:88ch}code{background:var(--surface);padding:.1rem .3rem;border-radius:.25rem}aside,.verified-example{padding:1rem;border-left:5px solid var(--teal);background:var(--pale);border-radius:.5rem}.verified-example{font-weight:700;margin-top:1.5rem}
.table-shell{overflow-x:auto;margin:1rem 0}table{width:100%;border-collapse:collapse;font-size:.9rem}th{background:var(--dark);color:white;text-align:left}th,td{border:1px solid var(--line);padding:.55rem;vertical-align:top}tbody tr:nth-child(even){background:var(--surface)}
pre{overflow:auto;background:var(--surface);border:1px solid var(--line);padding:1rem;border-radius:.6rem;font-size:.78rem}figure{margin:1.5rem 0}img{max-width:100%;height:auto;border:1px solid var(--line);border-radius:.7rem;box-shadow:0 3px 12px #173e3a22}figcaption{text-align:center;color:#52615f;font-size:.85rem;margin-top:.35rem}
@media print{body{background:white}main{box-shadow:none;max-width:none;padding:0}h2{break-before:page}figure,table,pre{break-inside:avoid}}
</style></head><body><main>""" + "\n".join(body) + "</main></body></html>"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(document, encoding="utf-8", newline="\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--html", type=Path, default=DEFAULT_HTML)
    parser.add_argument("--pdf", type=Path, default=DEFAULT_PDF)
    args = parser.parse_args()
    build_html(args.source.resolve(), args.html.resolve())
    build_pdf(args.source.resolve(), args.pdf.resolve())
    print(args.html.resolve())
    print(args.pdf.resolve())


if __name__ == "__main__":
    main()
