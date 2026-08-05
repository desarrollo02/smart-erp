"""Generate the reviewed PDF edition of the IntelliJ IDEA setup runbook."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from datetime import date
from pathlib import Path

from reportlab import rl_config
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    KeepTogether,
    NextPageTemplate,
    PageBreak,
    PageTemplate,
    Paragraph,
    Preformatted,
    Spacer,
    Table,
    TableStyle,
)
from reportlab.platypus.tableofcontents import TableOfContents


rl_config.invariant = 1

PRIMARY = colors.HexColor("#0B57D0")
PRIMARY_DARK = colors.HexColor("#17365D")
TEAL = colors.HexColor("#006C67")
INK = colors.HexColor("#1F2937")
MUTED = colors.HexColor("#5F6B7A")
SURFACE = colors.HexColor("#F4F7FB")
SURFACE_TEAL = colors.HexColor("#E7F5F3")
LINE = colors.HexColor("#D8E1EC")
AMBER = colors.HexColor("#A84F00")
WHITE = colors.white


def register_fonts() -> tuple[str, str, str, str]:
    candidates = [
        (
            Path("C:/Windows/Fonts/arial.ttf"),
            Path("C:/Windows/Fonts/arialbd.ttf"),
            Path("C:/Windows/Fonts/ariali.ttf"),
            Path("C:/Windows/Fonts/consola.ttf"),
        ),
        (
            Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
            Path("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"),
            Path("/usr/share/fonts/truetype/dejavu/DejaVuSans-Oblique.ttf"),
            Path("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"),
        ),
    ]
    for regular, bold, italic, mono in candidates:
        if all(path.exists() for path in (regular, bold, italic, mono)):
            pdfmetrics.registerFont(TTFont("SetupSans", str(regular)))
            pdfmetrics.registerFont(TTFont("SetupSans-Bold", str(bold)))
            pdfmetrics.registerFont(TTFont("SetupSans-Italic", str(italic)))
            pdfmetrics.registerFont(TTFont("SetupMono", str(mono)))
            pdfmetrics.registerFontFamily(
                "SetupSans",
                normal="SetupSans",
                bold="SetupSans-Bold",
                italic="SetupSans-Italic",
                boldItalic="SetupSans-Bold",
            )
            return "SetupSans", "SetupSans-Bold", "SetupSans-Italic", "SetupMono"
    return "Helvetica", "Helvetica-Bold", "Helvetica-Oblique", "Courier"


REGULAR_FONT, BOLD_FONT, ITALIC_FONT, MONO_FONT = register_fonts()


def escape(text: str) -> str:
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def inline_markup(text: str) -> str:
    value = escape(text)
    value = re.sub(
        r"\[([^\]]+)\]\(([^)]+)\)",
        rf'<link href="\2" color="{PRIMARY.hexval()}"><u>\1</u></link>',
        value,
    )
    value = re.sub(
        r"`([^`]+)`",
        rf'<font name="{MONO_FONT}" color="{PRIMARY_DARK.hexval()}">\1</font>',
        value,
    )
    value = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", value)
    value = re.sub(r"\*([^*]+)\*", r"<i>\1</i>", value)
    return value


def styles() -> dict[str, ParagraphStyle]:
    sample = getSampleStyleSheet()
    return {
        "cover_title": ParagraphStyle(
            "CoverTitle",
            parent=sample["Title"],
            fontName=BOLD_FONT,
            fontSize=29,
            leading=34,
            textColor=WHITE,
            alignment=TA_LEFT,
            spaceAfter=7 * mm,
        ),
        "cover_subtitle": ParagraphStyle(
            "CoverSubtitle",
            parent=sample["BodyText"],
            fontName=REGULAR_FONT,
            fontSize=13,
            leading=19,
            textColor=colors.HexColor("#DCE8FF"),
            spaceAfter=8 * mm,
        ),
        "cover_meta": ParagraphStyle(
            "CoverMeta",
            parent=sample["BodyText"],
            fontName=REGULAR_FONT,
            fontSize=9.5,
            leading=14,
            textColor=INK,
        ),
        "toc_title": ParagraphStyle(
            "TocTitle",
            parent=sample["Heading1"],
            fontName=BOLD_FONT,
            fontSize=23,
            leading=28,
            textColor=PRIMARY_DARK,
            spaceAfter=7 * mm,
        ),
        "h2": ParagraphStyle(
            "GuideH2",
            parent=sample["Heading1"],
            fontName=BOLD_FONT,
            fontSize=17,
            leading=21,
            textColor=PRIMARY_DARK,
            spaceBefore=7 * mm,
            spaceAfter=3 * mm,
            keepWithNext=True,
        ),
        "h3": ParagraphStyle(
            "GuideH3",
            parent=sample["Heading2"],
            fontName=BOLD_FONT,
            fontSize=12.5,
            leading=16,
            textColor=TEAL,
            spaceBefore=5 * mm,
            spaceAfter=2 * mm,
            keepWithNext=True,
        ),
        "h4": ParagraphStyle(
            "GuideH4",
            parent=sample["Heading3"],
            fontName=BOLD_FONT,
            fontSize=10.5,
            leading=14,
            textColor=PRIMARY_DARK,
            spaceBefore=3 * mm,
            spaceAfter=1.5 * mm,
            keepWithNext=True,
        ),
        "body": ParagraphStyle(
            "GuideBody",
            parent=sample["BodyText"],
            fontName=REGULAR_FONT,
            fontSize=9.3,
            leading=13.5,
            textColor=INK,
            spaceAfter=2.5 * mm,
        ),
        "bullet": ParagraphStyle(
            "GuideBullet",
            parent=sample["BodyText"],
            fontName=REGULAR_FONT,
            fontSize=9.2,
            leading=13.2,
            textColor=INK,
            leftIndent=6 * mm,
            firstLineIndent=0,
            bulletIndent=1.5 * mm,
            spaceAfter=1.2 * mm,
        ),
        "number": ParagraphStyle(
            "GuideNumber",
            parent=sample["BodyText"],
            fontName=REGULAR_FONT,
            fontSize=9.2,
            leading=13.2,
            textColor=INK,
            leftIndent=8 * mm,
            firstLineIndent=0,
            bulletIndent=1.5 * mm,
            spaceAfter=1.2 * mm,
        ),
        "code": ParagraphStyle(
            "GuideCode",
            parent=sample["Code"],
            fontName=MONO_FONT,
            fontSize=7.1,
            leading=9.4,
            textColor=colors.HexColor("#10233F"),
            backColor=colors.HexColor("#EEF3F9"),
            borderColor=LINE,
            borderWidth=0.6,
            borderPadding=6,
            leftIndent=2 * mm,
            rightIndent=2 * mm,
            spaceBefore=1.5 * mm,
            spaceAfter=3 * mm,
        ),
        "quote": ParagraphStyle(
            "GuideQuote",
            parent=sample["BodyText"],
            fontName=REGULAR_FONT,
            fontSize=9.2,
            leading=13.5,
            textColor=INK,
            backColor=colors.HexColor("#FFF4E5"),
            borderColor=colors.HexColor("#F2B766"),
            borderWidth=0.8,
            borderPadding=7,
            leftIndent=2 * mm,
            rightIndent=2 * mm,
            spaceBefore=1.5 * mm,
            spaceAfter=3 * mm,
        ),
        "small": ParagraphStyle(
            "GuideSmall",
            parent=sample["BodyText"],
            fontName=REGULAR_FONT,
            fontSize=8,
            leading=11,
            textColor=MUTED,
        ),
    }


class SetupDocument(BaseDocTemplate):
    def afterFlowable(self, flowable):
        if not isinstance(flowable, Paragraph):
            return
        style_name = flowable.style.name
        if style_name != "GuideH2":
            return
        level = 0
        plain_text = flowable.getPlainText()
        key = f"section-{level}-{self.seq.nextf('section')}"
        self.canv.bookmarkPage(key)
        self.canv.addOutlineEntry(plain_text, key, level=level, closed=False)
        self.notify("TOCEntry", (level, plain_text, self.page, key))


def parse_header(lines: list[str]) -> tuple[str, list[str], int]:
    title = "Levantar Logixone con IntelliJ IDEA Ultimate"
    metadata: list[str] = []
    index = 0
    if lines and lines[0].startswith("# "):
        title = lines[0][2:].strip()
        index = 1
    while index < len(lines):
        line = lines[index].strip()
        if line.startswith("## "):
            break
        if line.startswith("- "):
            metadata.append(line[2:].strip())
        index += 1
    return title, metadata, index


def markdown_flowables(
    lines: list[str], start: int, style: dict[str, ParagraphStyle]
) -> list:
    result: list = []
    index = start
    paragraph_lines: list[str] = []

    def flush_paragraph():
        if paragraph_lines:
            text = " ".join(part.strip() for part in paragraph_lines).strip()
            if text:
                result.append(Paragraph(inline_markup(text), style["body"]))
            paragraph_lines.clear()

    while index < len(lines):
        raw = lines[index]
        stripped = raw.strip()

        if stripped.startswith("```"):
            flush_paragraph()
            index += 1
            code_lines: list[str] = []
            while index < len(lines) and not lines[index].strip().startswith("```"):
                code_lines.append(lines[index].rstrip())
                index += 1
            result.append(
                Preformatted(
                    "\n".join(code_lines),
                    style["code"],
                    maxLineLength=86,
                )
            )
        elif stripped.startswith("## "):
            flush_paragraph()
            result.append(Paragraph(inline_markup(stripped[3:]), style["h2"]))
        elif stripped.startswith("### "):
            flush_paragraph()
            result.append(Paragraph(inline_markup(stripped[4:]), style["h3"]))
        elif stripped.startswith("#### "):
            flush_paragraph()
            result.append(Paragraph(inline_markup(stripped[5:]), style["h4"]))
        elif stripped.startswith("> "):
            flush_paragraph()
            quote_lines = [stripped[2:]]
            while index + 1 < len(lines):
                candidate = lines[index + 1].strip()
                if not candidate.startswith(">"):
                    break
                index += 1
                quote_lines.append(candidate[1:].strip())
            result.append(
                Paragraph(inline_markup(" ".join(quote_lines)), style["quote"])
            )
        elif re.match(r"^- \[[ xX]\] ", stripped):
            flush_paragraph()
            checked = stripped[3].lower() == "x"
            item = stripped[6:]
            while index + 1 < len(lines):
                continuation = lines[index + 1]
                if not continuation.startswith(("  ", "\t")):
                    break
                index += 1
                item += " " + continuation.strip()
            result.append(
                Paragraph(
                    f'<font name="{MONO_FONT}">[{"x" if checked else " "}]</font> '
                    f"{inline_markup(item)}",
                    style["bullet"],
                    bulletText="-",
                )
            )
        elif stripped.startswith("- "):
            flush_paragraph()
            item = stripped[2:]
            while index + 1 < len(lines):
                continuation = lines[index + 1]
                if not continuation.startswith(("  ", "\t")):
                    break
                index += 1
                item += " " + continuation.strip()
            result.append(
                Paragraph(
                    inline_markup(item), style["bullet"], bulletText="-"
                )
            )
        elif re.match(r"^\d+\.\s+", stripped):
            flush_paragraph()
            match = re.match(r"^(\d+)\.\s+(.*)", stripped)
            assert match is not None
            item = match.group(2)
            while index + 1 < len(lines):
                continuation = lines[index + 1]
                if not continuation.startswith(("  ", "\t")):
                    break
                index += 1
                item += " " + continuation.strip()
            result.append(
                Paragraph(
                    inline_markup(item),
                    style["number"],
                    bulletText=f"{match.group(1)}.",
                )
            )
        elif not stripped:
            flush_paragraph()
        else:
            paragraph_lines.append(stripped)
        index += 1

    flush_paragraph()
    return result


def draw_cover(canvas, doc):
    width, height = A4
    canvas.saveState()
    canvas.setFillColor(PRIMARY_DARK)
    canvas.rect(0, 0, width, height, stroke=0, fill=1)
    canvas.setFillColor(PRIMARY)
    canvas.circle(width - 22 * mm, height - 25 * mm, 42 * mm, stroke=0, fill=1)
    canvas.setFillColor(TEAL)
    canvas.circle(width - 2 * mm, 24 * mm, 32 * mm, stroke=0, fill=1)
    canvas.setStrokeColor(colors.HexColor("#90CAF9"))
    canvas.setLineWidth(1.2)
    canvas.line(22 * mm, 35 * mm, 88 * mm, 35 * mm)
    canvas.setFillColor(colors.HexColor("#DCE8FF"))
    canvas.setFont(REGULAR_FONT, 8.5)
    canvas.drawString(22 * mm, 25 * mm, "Logixone · Jakarta EE 11 · Guía operativa")
    canvas.restoreState()


def draw_content(canvas, doc):
    width, height = A4
    canvas.saveState()
    canvas.setStrokeColor(LINE)
    canvas.setLineWidth(0.6)
    canvas.line(18 * mm, height - 16 * mm, width - 18 * mm, height - 16 * mm)
    canvas.setFillColor(MUTED)
    canvas.setFont(REGULAR_FONT, 7.5)
    canvas.drawString(
        18 * mm, height - 12 * mm, "Logixone · Levantar el proyecto con IntelliJ IDEA"
    )
    canvas.drawRightString(
        width - 18 * mm, 10 * mm, f"Página {doc.page}"
    )
    canvas.drawString(
        18 * mm, 10 * mm, "Fuente canónica: docs/runbooks/levantar-logixone-intellij-idea-ultimate.md"
    )
    canvas.restoreState()


def build_story(
    source: Path, title: str, metadata: list[str], content_start: int
) -> list:
    style = styles()
    story: list = [
        Spacer(1, 42 * mm),
        Paragraph(title, style["cover_title"]),
        Paragraph(
            "Guía paso a paso para importar, configurar, construir, ejecutar y "
            "diagnosticar el ERP desde IntelliJ sin apartarse del baseline Docker.",
            style["cover_subtitle"],
        ),
    ]

    metadata_rows = []
    for item in metadata:
        if ":" in item:
            label, value = item.split(":", 1)
        else:
            label, value = "Dato", item
        metadata_rows.append(
            [
                Paragraph(f"<b>{escape(label.strip())}</b>", style["cover_meta"]),
                Paragraph(inline_markup(value.strip()), style["cover_meta"]),
            ]
        )
    metadata_table = Table(metadata_rows, colWidths=[39 * mm, 93 * mm])
    metadata_table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#F8FAFF")),
                ("BOX", (0, 0), (-1, -1), 0.8, colors.HexColor("#8AA8D8")),
                ("INNERGRID", (0, 0), (-1, -1), 0.35, LINE),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 7),
                ("RIGHTPADDING", (0, 0), (-1, -1), 7),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
            ]
        )
    )
    story.extend(
        [
            metadata_table,
            Spacer(1, 8 * mm),
            Paragraph(
                "<b>Ruta rápida:</b> configurar Java 21 → preparar secretos → "
                "construir dos imágenes → ejecutar Compose → comprobar readiness → "
                "abrir la interfaz.",
                ParagraphStyle(
                    "CoverCallout",
                    parent=style["cover_meta"],
                    backColor=SURFACE_TEAL,
                    borderColor=colors.HexColor("#76BDB7"),
                    borderWidth=0.8,
                    borderPadding=8,
                    leading=14,
                ),
            ),
            NextPageTemplate("content"),
            PageBreak(),
            Paragraph("Contenido", style["toc_title"]),
        ]
    )

    toc = TableOfContents()
    toc.levelStyles = [
        ParagraphStyle(
            "TOC1",
            fontName=BOLD_FONT,
            fontSize=9.3,
            leading=13,
            textColor=PRIMARY_DARK,
            leftIndent=0,
            firstLineIndent=0,
            spaceAfter=1.5 * mm,
        ),
    ]
    story.extend(
        [
            toc,
            PageBreak(),
            *markdown_flowables(
                source.read_text(encoding="utf-8").splitlines(),
                content_start,
                style,
            ),
        ]
    )
    return story


def generate(source: Path, output: Path) -> dict:
    lines = source.read_text(encoding="utf-8").splitlines()
    title, metadata, content_start = parse_header(lines)
    output.parent.mkdir(parents=True, exist_ok=True)

    cover_frame = Frame(
        22 * mm,
        20 * mm,
        A4[0] - 44 * mm,
        A4[1] - 40 * mm,
        id="cover-frame",
        showBoundary=0,
    )
    content_frame = Frame(
        18 * mm,
        16 * mm,
        A4[0] - 36 * mm,
        A4[1] - 36 * mm,
        id="content-frame",
        showBoundary=0,
    )
    doc = SetupDocument(
        str(output),
        pagesize=A4,
        title=title,
        author="Proyecto Logixone",
        subject="Guía para levantar Logixone con IntelliJ IDEA Ultimate 2026.2",
        creator="Logixone documentation toolchain",
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=18 * mm,
        bottomMargin=16 * mm,
    )
    doc.addPageTemplates(
        [
            PageTemplate(
                id="cover",
                frames=[cover_frame],
                onPage=draw_cover,
                autoNextPageTemplate="content",
            ),
            PageTemplate(
                id="content",
                frames=[content_frame],
                onPage=draw_content,
                autoNextPageTemplate="content",
            ),
        ]
    )
    doc.multiBuild(build_story(source, title, metadata, content_start))

    payload = output.read_bytes()
    return {
        "source": str(source),
        "output": str(output),
        "bytes": len(payload),
        "sha256": hashlib.sha256(payload).hexdigest(),
    }


def main():
    parser = argparse.ArgumentParser()
    root = Path(__file__).resolve().parents[1]
    parser.add_argument(
        "--source",
        type=Path,
        default=root / "docs/runbooks/levantar-logixone-intellij-idea-ultimate.md",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=root
        / "docs/output/pdf/guia-levantar-logixone-intellij-idea-ultimate.pdf",
    )
    args = parser.parse_args()
    source = args.source if args.source.is_absolute() else root / args.source
    output = args.output if args.output.is_absolute() else root / args.output
    print(json.dumps(generate(source.resolve(), output.resolve()), ensure_ascii=False))


if __name__ == "__main__":
    main()
