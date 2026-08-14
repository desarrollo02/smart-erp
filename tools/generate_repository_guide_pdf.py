"""Generate the repository structure guide required at every Sprint close."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
from collections import Counter, defaultdict
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
    Spacer,
    Table,
    TableStyle,
)
from reportlab.platypus.tableofcontents import TableOfContents
from svglib.svglib import svg2rlg


rl_config.invariant = 1


PRIMARY = colors.HexColor("#0B57D0")
PRIMARY_DARK = colors.HexColor("#17365D")
TEAL = colors.HexColor("#006C67")
INK = colors.HexColor("#1F2937")
MUTED = colors.HexColor("#5F6B7A")
SURFACE = colors.HexColor("#F4F7FB")
LINE = colors.HexColor("#D8E1EC")
AMBER = colors.HexColor("#B45309")
WHITE = colors.white

EXCLUDED_PARTS = {".git", ".tools", "target", "tmp", "__pycache__", ".idea", ".vscode", "bin", "build", "current"}
EXCLUDED_FILES = {"infra/compose/compose.env.local"}

TOP_LEVEL_DESCRIPTIONS = {
    "root": "Gobierno del repositorio, Maven padre, Wrapper y reglas transversales.",
    ".mvn": "Configuración reproducible del Maven Wrapper.",
    "platform-bom": "BOM que centraliza versiones compartidas.",
    "plugin-api": "Contratos Java puros para plugins, dependencias, contribuciones y pantallas.",
    "kernel-api": "Contratos públicos neutrales del kernel, empresa y seguridad.",
    "kernel-domain": "Reglas y políticas transversales sin Jakarta ni infraestructura.",
    "kernel-application": "Casos de uso, puertos, consultas, comandos y guardas.",
    "kernel-infrastructure-jakarta": "Adaptadores CDI, JPA/JTA, PostgreSQL, OIDC y health.",
    "migrator": "Ejecutable one-shot de Flyway para core V1-V6 y migraciones privadas de plugins.",
    "plugins": "Datos de Referencia, Socios Comerciales, Catálogo Comercial, Inventario, Compras, sus API públicas y los fixtures funcional/personalizaciones A/B.",
    "web-shell": "Superficie Jakarta Faces/JAX-RS, sesión, autorización y administración visual.",
    "distribution": "Ensamblado físico del WAR y perfiles de composición base, referencia y demo productiva.",
    "tests": "Gates de arquitectura, integración, Playwright y arnés JTA opt-in.",
    "infra": "Docker, Compose, Keycloak y configuración reproducible de WildFly.",
    "installer": "Fuente, manifiesto, pruebas y scripts del instalador Windows; los binarios derivados se excluyen del inventario canónico.",
    "tools": "Herramientas versionadas para generar plugins y verificar artefactos del proyecto.",
    "docs": "ADR, arquitectura, backlog, guías, runbooks, Sprints y evidencias.",
}


def register_fonts() -> tuple[str, str, str]:
    candidates = [
        (
            Path("C:/Windows/Fonts/arial.ttf"),
            Path("C:/Windows/Fonts/arialbd.ttf"),
            Path("C:/Windows/Fonts/consola.ttf"),
        ),
        (
            Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
            Path("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"),
            Path("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"),
        ),
    ]
    for regular, bold, mono in candidates:
        if regular.exists() and bold.exists() and mono.exists():
            pdfmetrics.registerFont(TTFont("GuideSans", str(regular)))
            pdfmetrics.registerFont(TTFont("GuideSans-Bold", str(bold)))
            pdfmetrics.registerFont(TTFont("GuideMono", str(mono)))
            return "GuideSans", "GuideSans-Bold", "GuideMono"
    return "Helvetica", "Helvetica-Bold", "Courier"


REGULAR_FONT, BOLD_FONT, MONO_FONT = register_fonts()


def escape(text: str) -> str:
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def humanize(name: str) -> str:
    stem = re.sub(r"\.(java|cs|ps1|xhtml|css|sql|md|xml|json|properties|yml|yaml|py)$", "", name)
    words = re.sub(r"([a-z0-9])([A-Z])", r"\1 \2", stem).replace("-", " ").replace("_", " ")
    return words.strip()


def markdown_title(path: Path) -> str | None:
    try:
        for line in path.read_text(encoding="utf-8").splitlines()[:20]:
            if line.startswith("# "):
                return line[2:].strip()
    except (OSError, UnicodeError):
        return None
    return None


def describe_file(root: Path, relative: Path) -> str:
    posix = relative.as_posix()
    name = relative.name
    lower = name.lower()

    exact = {
        "AGENTS.md": "Reglas obligatorias de arquitectura, pruebas, documentación y operación del repositorio.",
        "pom.xml": "POM padre del reactor: Java 21, 24 módulos, versiones, perfiles y Maven Enforcer.",
        "mvnw": "Lanzador POSIX del Maven Wrapper.",
        "mvnw.cmd": "Lanzador Windows del Maven Wrapper.",
        ".dockerignore": "Excluye cachés, secretos, targets y temporales del contexto OCI.",
        ".editorconfig": "Normaliza codificación, finales de línea e indentación.",
        ".gitattributes": "Define el tratamiento consistente de texto y binarios.",
        ".gitignore": "Evita versionar secretos, cachés, builds y configuración local.",
    }
    if posix in exact:
        return exact[posix]

    if name == "pom.xml":
        return f"POM Maven del módulo `{relative.parent.as_posix()}`: dependencias, plugins y empaquetado."
    if lower.endswith(".md"):
        title = markdown_title(root / relative)
        return f"Documento canónico: {title}." if title else "Documento Markdown canónico del área."
    if lower.endswith(".java"):
        label = humanize(name)
        if "/src/test/" in f"/{posix}":
            if name.endswith("IT.java"):
                return f"Prueba de integración/runtime: {label}."
            if "architecture" in posix.lower():
                return f"Gate ArchUnit o composición: {label}."
            return f"Prueba automatizada: {label}."
        suffixes = {
            "Resource.java": "Recurso REST",
            "Filter.java": "Filtro de frontera web",
            "Bean.java": "Backing bean Jakarta Faces",
            "Entity.java": "Entidad JPA",
            "Repository.java": "Adaptador o contrato de repositorio",
            "Service.java": "Servicio de aplicación",
            "Policy.java": "Política de dominio",
            "Command.java": "Comando de aplicación",
            "Query.java": "Consulta de aplicación",
            "Port.java": "Puerto público o de infraestructura",
            "Exception.java": "Excepción tipada",
        }
        for suffix, kind in suffixes.items():
            if name.endswith(suffix):
                return f"{kind}: {label}."
        return f"Tipo Java del módulo propietario: {label}."
    if lower.endswith(".cs"):
        return f"Fuente C# del bootstrapper Windows: {humanize(name)}."
    if lower.endswith(".ps1"):
        return f"Script PowerShell reproducible del instalador: {humanize(name)}."
    if lower.endswith(".xhtml"):
        return f"Vista Jakarta Faces responsive: {humanize(name)}."
    if lower.endswith(".css"):
        return f"Estilos Material Design y responsive: {humanize(name)}."
    if lower.endswith(".sql"):
        version = re.search(r"V(\d+)__([^.]*)", name)
        if version:
            return f"Migración Flyway inmutable V{version.group(1)}: {humanize(version.group(2))}."
        return "Script SQL versionado del propietario del esquema."
    if lower.endswith((".yml", ".yaml")):
        return "Configuración declarativa YAML para construcción u operación."
    if lower.endswith(".xml"):
        return "Configuración XML declarativa del módulo o runtime."
    if lower.endswith(".json"):
        return "Configuración JSON declarativa sin secretos versionados."
    if lower.endswith(".properties"):
        return "Propiedades versionadas de build, runtime o migración."
    if lower.endswith(".pdf"):
        return "PDF derivado para consulta; las fuentes canónicas permanecen en código y Markdown."
    if lower.endswith(".py"):
        return f"Herramienta reproducible en Python: {humanize(name)}."
    if lower.endswith((".sh", ".cmd")):
        return "Script operativo reproducible del proyecto."
    return "Archivo mantenido por el repositorio para build, documentación u operación."


def describe_directory(relative: Path) -> str:
    posix = relative.as_posix()
    if posix in TOP_LEVEL_DESCRIPTIONS:
        return TOP_LEVEL_DESCRIPTIONS[posix]
    if posix.startswith("docs/"):
        area = relative.parts[1] if len(relative.parts) > 1 else "documentación"
        return f"Área documental `{area}`; conserva fuentes canónicas o artefactos derivados identificados."
    if "/src/main/java" in posix:
        return "Paquete de código Java productivo del módulo propietario."
    if "/src/test/java" in posix:
        return "Paquete de pruebas automatizadas del módulo propietario."
    if "/src/main/resources" in posix:
        return "Recursos productivos empaquetados con el módulo."
    if "/src/test/resources" in posix:
        return "Fixtures y configuración exclusiva de pruebas."
    if posix.startswith("infra/"):
        return "Configuración de infraestructura como código para el entorno indicado."
    if posix.startswith("plugins/"):
        return "Código o configuración de un plugin aislado por contratos públicos."
    if posix.startswith("tests/"):
        return "Módulo o recurso dedicado a validación automatizada; no entra al WAR normal."
    return "Directorio mantenido dentro de su módulo o área propietaria."


def collect_inventory(root: Path) -> tuple[list[Path], list[Path]]:
    files: list[Path] = []
    directories: set[Path] = set()
    for current, dir_names, file_names in os.walk(root, topdown=True):
        dir_names[:] = sorted(name for name in dir_names if name not in EXCLUDED_PARTS)
        current_path = Path(current)
        current_relative = current_path.relative_to(root)
        if current_relative != Path("."):
            directories.add(current_relative)
        for name in sorted(file_names):
            relative = (current_path / name).relative_to(root)
            if relative.as_posix() in EXCLUDED_FILES:
                continue
            files.append(relative)
            parent = relative.parent
            while parent != Path("."):
                directories.add(parent)
                parent = parent.parent
    return sorted(files, key=lambda p: p.as_posix().lower()), sorted(
        directories, key=lambda p: p.as_posix().lower()
    )


def top_group(path: Path) -> str:
    if len(path.parts) == 1:
        return "root"
    return path.parts[0]


def build_styles():
    base = getSampleStyleSheet()
    styles = {
        "Title": ParagraphStyle(
            "Title",
            parent=base["Title"],
            fontName=BOLD_FONT,
            fontSize=27,
            leading=31,
            textColor=WHITE,
            alignment=TA_LEFT,
            spaceAfter=8,
        ),
        "CoverSub": ParagraphStyle(
            "CoverSub",
            fontName=REGULAR_FONT,
            fontSize=12,
            leading=17,
            textColor=colors.HexColor("#DCEBFF"),
        ),
        "Heading1": ParagraphStyle(
            "Heading1",
            parent=base["Heading1"],
            fontName=BOLD_FONT,
            fontSize=16,
            leading=20,
            textColor=PRIMARY_DARK,
            spaceBefore=10,
            spaceAfter=8,
            keepWithNext=True,
        ),
        "Heading2": ParagraphStyle(
            "Heading2",
            parent=base["Heading2"],
            fontName=BOLD_FONT,
            fontSize=12,
            leading=15,
            textColor=PRIMARY,
            spaceBefore=8,
            spaceAfter=5,
            keepWithNext=True,
        ),
        "Body": ParagraphStyle(
            "Body",
            parent=base["BodyText"],
            fontName=REGULAR_FONT,
            fontSize=8.6,
            leading=12.2,
            textColor=INK,
            spaceAfter=5,
        ),
        "Small": ParagraphStyle(
            "Small",
            parent=base["BodyText"],
            fontName=REGULAR_FONT,
            fontSize=7.1,
            leading=9.4,
            textColor=INK,
        ),
        "HeaderSmall": ParagraphStyle(
            "HeaderSmall",
            parent=base["BodyText"],
            fontName=BOLD_FONT,
            fontSize=7.1,
            leading=9.4,
            textColor=WHITE,
        ),
        "Tiny": ParagraphStyle(
            "Tiny",
            parent=base["BodyText"],
            fontName=REGULAR_FONT,
            fontSize=6.4,
            leading=8.2,
            textColor=INK,
        ),
        "Path": ParagraphStyle(
            "Path",
            parent=base["BodyText"],
            fontName=MONO_FONT,
            fontSize=6.1,
            leading=7.6,
            textColor=PRIMARY_DARK,
            wordWrap="CJK",
        ),
        "Code": ParagraphStyle(
            "Code",
            fontName=MONO_FONT,
            fontSize=7.2,
            leading=10.2,
            leftIndent=6,
            rightIndent=6,
            borderColor=LINE,
            borderWidth=0.5,
            borderPadding=6,
            backColor=SURFACE,
            textColor=INK,
            spaceBefore=3,
            spaceAfter=7,
        ),
        "Callout": ParagraphStyle(
            "Callout",
            fontName=REGULAR_FONT,
            fontSize=8.3,
            leading=12,
            leftIndent=8,
            rightIndent=8,
            borderColor=PRIMARY,
            borderWidth=0.8,
            borderPadding=8,
            backColor=colors.HexColor("#EAF2FF"),
            textColor=INK,
            spaceBefore=5,
            spaceAfter=8,
        ),
        "Caption": ParagraphStyle(
            "Caption",
            fontName=REGULAR_FONT,
            fontSize=6.6,
            leading=8.4,
            textColor=MUTED,
        ),
    }
    return styles


STYLES = build_styles()


def paragraph(text: str, style: str = "Body") -> Paragraph:
    text = text.translate(
        {
            ord("‐"): "-",
            ord("‑"): "-",
            ord("‒"): "-",
            ord("–"): "-",
            ord("—"): "-",
            ord("−"): "-",
        }
    )
    return Paragraph(text, STYLES[style])


def bullet(text: str) -> Paragraph:
    return Paragraph(f"- {text}", STYLES["Body"])


def topology_drawing(root: Path):
    source = root / "docs/sprints/sprint-09/estructura-plugins-y-dependencias.svg"
    drawing = svg2rlg(str(source))
    if drawing is None or not drawing.width or not drawing.height:
        raise RuntimeError(f"No se pudo convertir el diagrama SVG: {source}")
    original_width = drawing.width
    original_height = drawing.height
    factor = min((160 * mm) / original_width, (98 * mm) / original_height)
    drawing.scale(factor, factor)
    drawing.width = original_width * factor
    drawing.height = original_height * factor
    drawing.hAlign = "CENTER"
    return drawing


def data_table(rows, widths, header=True, small=False):
    style_name = "Tiny" if small else "Small"
    normalized = []
    for row_index, row in enumerate(rows):
        cells = []
        for value in row:
            if isinstance(value, Paragraph):
                cells.append(value)
            else:
                cell_style = "HeaderSmall" if row_index == 0 and header else style_name
                cells.append(paragraph(escape(str(value)), cell_style))
        normalized.append(cells)
    table = Table(normalized, colWidths=widths, repeatRows=1 if header else 0, hAlign="LEFT")
    commands = [
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.35, LINE),
        ("LEFTPADDING", (0, 0), (-1, -1), 4),
        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
        ("TOPPADDING", (0, 0), (-1, -1), 3),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
        ("ROWBACKGROUNDS", (0, 1 if header else 0), (-1, -1), [WHITE, SURFACE]),
    ]
    if header:
        commands.extend(
            [
                ("BACKGROUND", (0, 0), (-1, 0), PRIMARY_DARK),
                ("TEXTCOLOR", (0, 0), (-1, 0), WHITE),
                ("FONTNAME", (0, 0), (-1, 0), BOLD_FONT),
            ]
        )
    table.setStyle(TableStyle(commands))
    return table


class GuideDocument(BaseDocTemplate):
    def __init__(self, filename: str, sprint: str, edition_date: str, **kwargs):
        super().__init__(filename, **kwargs)
        self.sprint = sprint
        self.edition_date = edition_date

    def afterFlowable(self, flowable):
        if isinstance(flowable, Paragraph) and flowable.style.name in {"Heading1", "Heading2"}:
            level = 0 if flowable.style.name == "Heading1" else 1
            text = flowable.getPlainText()
            key = "section-" + hashlib.sha1(f"{level}:{text}".encode("utf-8")).hexdigest()[:16]
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(text, key, level=level, closed=False)
            self.notify("TOCEntry", (level, text, self.page, key))


def draw_cover(canvas, doc):
    canvas.saveState()
    width, height = A4
    canvas.setFillColor(PRIMARY_DARK)
    canvas.rect(0, 0, width, height, fill=1, stroke=0)
    canvas.setFillColor(PRIMARY)
    canvas.rect(0, height - 48 * mm, width, 48 * mm, fill=1, stroke=0)
    canvas.setFillColor(TEAL)
    canvas.rect(0, 0, 12 * mm, height, fill=1, stroke=0)
    canvas.setFillColor(colors.HexColor("#9FC5FF"))
    canvas.circle(width - 28 * mm, 27 * mm, 14 * mm, fill=1, stroke=0)
    canvas.setTitle(f"Smart ERP - Guía de estructura - {doc.sprint}")
    canvas.setAuthor("Proyecto Smart ERP")
    canvas.setSubject("Arquitectura, carpetas, archivos, estado y continuidad del ERP modular")
    canvas.setKeywords("Smart ERP, Jakarta EE 11, Sprint 9, reference data, business partners, commercial catalog, inventory, purchasing, plugins, repositorio")
    canvas.restoreState()


def draw_content_page(canvas, doc):
    canvas.saveState()
    width, height = A4
    canvas.setStrokeColor(LINE)
    canvas.setLineWidth(0.5)
    canvas.line(17 * mm, height - 15 * mm, width - 17 * mm, height - 15 * mm)
    canvas.setFont(BOLD_FONT, 7)
    canvas.setFillColor(PRIMARY_DARK)
    canvas.drawString(17 * mm, height - 11.5 * mm, "SMART ERP")
    canvas.setFont(REGULAR_FONT, 7)
    canvas.setFillColor(MUTED)
    canvas.drawRightString(width - 17 * mm, height - 11.5 * mm, f"{doc.sprint.upper()} - {doc.edition_date}")
    canvas.line(17 * mm, 14 * mm, width - 17 * mm, 14 * mm)
    canvas.setFont(REGULAR_FONT, 6.5)
    canvas.drawString(17 * mm, 9.8 * mm, "Documento derivado; código y Markdown son las fuentes canónicas")
    canvas.drawRightString(width - 17 * mm, 9.8 * mm, f"Página {doc.page}")
    canvas.restoreState()


def build_story(root: Path, files: list[Path], directories: list[Path], sprint: str, edition_date: str):
    story = []
    counts = Counter(path.suffix.lower() or "(sin extensión)" for path in files)
    source_files = [p for p in files if p.suffix.lower() != ".pdf"]
    derived_pdfs = [p for p in files if p.suffix.lower() == ".pdf"]

    story.extend(
        [
            Spacer(1, 35 * mm),
            paragraph("GUÍA TÉCNICA DEL REPOSITORIO", "CoverSub"),
            Spacer(1, 4 * mm),
            paragraph("Smart ERP", "Title"),
            paragraph("Arquitectura, carpetas, archivos y estado del proyecto", "CoverSub"),
            Spacer(1, 22 * mm),
            data_table(
                [
                    ["EDICIÓN", "CORTE", "ESTADO"],
                    [sprint, edition_date, "Instalador interno creado; G7 y matriz Windows pendientes"],
                ],
                [43 * mm, 35 * mm, 78 * mm],
            ),
            Spacer(1, 10 * mm),
            paragraph(
                "Esta edición explica la organización real del ERP modular, la responsabilidad de cada carpeta y archivo mantenido, "
                "la ejecución con Docker y la demo visual de cinco plugins productivos. El instalador interno 0.9.0-internal.1 representa este baseline, pero permanece sin firma y restringido a evaluación interna.",
                "CoverSub",
            ),
            Spacer(1, 22 * mm),
            paragraph("Jakarta EE 11 | Java 21 | WildFly 41 | PostgreSQL | Maven | Docker | JSF", "CoverSub"),
            NextPageTemplate("content"),
            PageBreak(),
        ]
    )

    story.append(paragraph("Contenido", "Heading1"))
    toc = TableOfContents()
    toc.levelStyles = [
        ParagraphStyle(
            "TOC1", fontName=BOLD_FONT, fontSize=9.2, leading=13, leftIndent=0, firstLineIndent=0, textColor=PRIMARY_DARK
        ),
        ParagraphStyle(
            "TOC2", fontName=REGULAR_FONT, fontSize=7.6, leading=10.5, leftIndent=10, firstLineIndent=0, textColor=INK
        ),
    ]
    story.extend([paragraph("Índice generado desde la estructura real de esta edición.", "Body"), toc, PageBreak()])

    story.append(paragraph("1. Alcance y forma de lectura", "Heading1"))
    story.append(
        paragraph(
            "Las fuentes canónicas son el código, los POM, la infraestructura declarativa y los Markdown. Este PDF es una fotografía derivada "
            "para consulta y entrega. No debe editarse para cambiar arquitectura ni sustituye ADR, historias, runbooks o evidencia ejecutable."
        )
    )
    story.append(
        data_table(
            [
                ["Métrica", "Valor", "Tratamiento"],
                ["Archivos mantenidos", len(files), "Incluye fuentes y PDF derivados; excluye cachés, targets y secretos locales"],
                ["Fuentes/configuración", len(source_files), "Código, infraestructura y Markdown canónicos"],
                ["Directorios mantenidos", len(directories), "Todos contienen al menos un archivo inventariado"],
                ["Java / Markdown / XHTML", f"{counts['.java']} / {counts['.md']} / {counts['.xhtml']}", "Implementación, conocimiento y UI"],
                ["PDF derivados", len(derived_pdfs), "Consulta; nunca fuente primaria"],
                ["Módulos Maven", 26, "Reactor completo; el arnés JTA es opt-in"],
            ],
            [42 * mm, 35 * mm, 83 * mm],
        )
    )
    story.extend(
        [
            bullet("Primero: comprender arquitectura, límites y secuencia de arranque."),
            bullet("Segundo: ubicar la carpeta propietaria de cada cambio."),
            bullet("Tercero: consultar el inventario archivo por archivo."),
            bullet("Cuarto: revisar qué está probado, qué falta y cuál es el siguiente trabajo autorizado."),
        ]
    )

    story.append(paragraph("2. Resumen ejecutivo", "Heading1"))
    story.append(
        paragraph(
            "Smart ERP es un ERP nuevo construido como monolito modular. Se despliega un solo WAR, pero las capacidades empresariales se "
            "incorporan como JAR de plugins. El kernel conserva responsabilidades transversales; ventas, facturación y otros "
            "dominios pertenecen a plugins productivos futuros."
        )
    )
    story.append(
        data_table(
            [
                ["Dimensión", "Decisión vigente"],
                ["Runtime", "Java 21, Jakarta EE 11 y WildFly 41"],
                ["Empaquetado", "Un WAR; agregar o retirar un plugin exige rebuild y redeploy"],
                ["Datos", "PostgreSQL 18.4; core V1-V6, reference_data V1-V4, business_partners V1-V4, commercial_catalog V1-V4, inventory V1-V2, purchasing V1-V2 y fixture V1; Hibernate solo valida"],
                ["Empresa", "UUID opaco, membresía confiable y una personalización exclusiva"],
                ["Seguridad", "OIDC autentica; permisos empresariales y globales se resuelven en el kernel"],
                ["UI", "Jakarta Faces 4.1, Material Design 3 y responsive obligatorio"],
                ["Operación", "Docker/Compose, secretos por archivo, volúmenes explícitos y health semántico"],
                ["Instalación Windows", "Bootstrapper nativo; preflight antes de consentimiento/UAC; canal interno sin firma"],
            ],
            [43 * mm, 117 * mm],
        )
    )

    story.append(paragraph("3. Arquitectura vigente", "Heading1"))
    story.append(paragraph("3.1 Dependencias y composición física de Sprint 9", "Heading2"))
    story.append(topology_drawing(root))
    story.append(
        paragraph(
            "La flecha continua sale del consumidor y apunta a una dependencia funcional REQUIRED; la flecha discontinua muestra la selección única consumida por WAR y migrador. La sección 6 ofrece la alternativa textual.",
            "Caption",
        )
    )
    architecture_rows = [
        ["Capa", "Responsabilidad", "Dependencias prohibidas"],
        ["plugin-api / kernel-api", "Contratos Java puros y estables", "Jakarta, JPA, JSF, WildFly"],
        ["kernel-domain", "Reglas, políticas, compatibilidad y revocación", "SQL, HTTP, CDI, entidades JPA"],
        ["kernel-application", "Casos de uso, puertos, consultas y autorización", "Adaptadores concretos y UI"],
        ["infraestructura Jakarta", "CDI, JPA/JTA, PostgreSQL, OIDC y health", "Dominios ERP"],
        ["plugins funcionales", "Capacidades ERP y contratos de pantalla", "Internos o tablas de otros plugins"],
        ["CUSTOMIZATION", "Uno distinto por empresa; overlays públicos al final", "Reemplazar XHTML o relajar seguridad"],
        ["web-shell / WAR", "JSF, filtros, sesión y ensamblado físico", "Reglas de negocio de plugins"],
    ]
    story.append(data_table(architecture_rows, [37 * mm, 72 * mm, 51 * mm]))
    story.append(
        paragraph(
            "El catálogo rechaza IDs duplicados, dependencias ausentes, ciclos y versiones incompatibles. Un plugin presente puede activarse "
            "por empresa, pero desactivarlo o retirarlo no elimina automáticamente tablas, migraciones ni datos.",
            "Callout",
        )
    )

    story.append(paragraph("4. Arranque, migraciones y volúmenes", "Heading1"))
    story.append(
        data_table(
            [
                ["Orden", "Componente", "Responsabilidad"],
                ["1", "PostgreSQL", "Crea o reutiliza `postgres-data`; inicializa solo un directorio vacío"],
                ["2", "migrator", "Compara Flyway y aplica solo versiones pendientes de cada propietario"],
                ["3", "Keycloak", "Crea o reutiliza `keycloak-data` e importa realm declarativo"],
                ["4", "WildFly", "Valida JPA, configura OIDC y despliega el WAR"],
                ["5", "health", "Liveness confirma proceso; readiness confirma que puede atender"],
            ],
            [17 * mm, 38 * mm, 105 * mm],
        )
    )
    story.append(
        paragraph(
            "`docker compose down` retira contenedores y redes, pero conserva los volúmenes. `down --volumes` también elimina los datos y solo "
            "se admite sobre un proyecto sintético inequívoco. En el corte J11-S9-07 se recreó únicamente `app`: PostgreSQL y Keycloak conservaron "
            "sus volúmenes explícitos y los datos de los cinco plugins productivos; dos ejecuciones del migrador informaron cero cambios.",
            "Callout",
        )
    )

    story.append(paragraph("5. Flujo de plugins por empresa", "Heading1"))
    story.append(
        data_table(
            [
                ["Paso", "Qué ocurre", "Fallo seguro"],
                ["1. Catálogo físico", "CDI descubre definiciones incluidas en el WAR", "Catálogo DOWN ante duplicado/ciclo/incompatibilidad"],
                ["2. Empresa", "Se registra con personalización exclusiva obligatoria", "Sin personalización válida no puede operar"],
                ["3. Intención", "Se persiste ENABLED/DISABLED por empresa/plugin", "Fila ausente equivale a no efectivo"],
                ["4. Resolución", "Cruza catálogo, compatibilidad y dependencias", "Ausencia o dependencia rota deniega"],
                ["5. Contribuciones", "Filtra permisos, menús, capacidades y pantallas", "Nada de otra empresa participa"],
                ["6. Personalización", "Aplica el único overlay empresarial al final", "Overlay inválido se rechaza completo"],
            ],
            [29 * mm, 76 * mm, 55 * mm],
        )
    )

    story.append(paragraph("6. Estado del baseline J11-S9-08", "Heading1"))
    story.append(
        data_table(
            [
                ["Gate", "Resultado"],
                ["Reactor", "28/28 módulos; 535 pruebas sin fallos, errores u omisiones; 34 ArchUnit"],
                ["Persistencia", "Compras PostgreSQL/Testcontainers 7/7; Flyway V1-V4/V1-V2 e idempotencia acumulada"],
                ["Seguridad", "Health 2/2 y OIDC 4/4 contra la imagen final; autorización negativa cubierta por Playwright"],
                ["UI", "Playwright acumulado 9/9; 170 capturas revisadas en 375/720/1280 sin overflow normal"],
                ["Imagen", "App `sha256:60f5de23...d49a`; migrator `sha256:5e1d1db7...fb95`"],
                ["Instalador", "0.9.0-internal.1; 8 archivos, 58 aserciones, preflight bloqueado sin cambios, UI smoke verde; NotSigned"],
                ["Pendiente", "Validación independiente G7, Authenticode y matriz Windows real"],
            ],
            [43 * mm, 117 * mm],
        )
    )
    story.extend(
        [
            bullet("Disponible: empresas, catálogo, activación, personalización, identidad, sesión, autorización, administración y auditoría visual."),
            bullet("Disponible: composición física única, migraciones `plg_*`, plantilla reproducible y contrato rector de eventos/outbox."),
            bullet("Disponible: demo empresarial A/B, administración, Datos de referencia, Socios, Catálogo, Inventario y Compras responsive con Material Design sobre JSF."),
            bullet("Disponible: cinco plugins productivos con dominio, API, persistencia, aplicación, seguridad y UI."),
            bullet("Disponible: instalador interno 0.9.0-internal.1 ligado a los digests de J11-S9-07; distribución externa bloqueada."),
            bullet("Pendiente: plugins siguientes del roadmap y despliegue productivo."),
            bullet("Decisión registrada: producto respondió SÍ el 2026-08-14 y J11-S9-08 promovió los ocho derivados declarados."),
            bullet("Bloqueos restantes: validación independiente G7, Authenticode y matriz Windows externa; este PDF no los sustituye."),
        ]
    )

    story.append(paragraph("7. Árbol de alto nivel", "Heading1"))
    tree = """smart-erp/
|-- AGENTS.md, pom.xml, mvnw, mvnw.cmd
|-- .mvn/                         Wrapper Maven
|-- platform-bom/                Versiones compartidas
|-- plugin-api/                  Contratos de plugins y pantallas
|-- kernel-api/                  Contratos públicos del kernel
|-- kernel-domain/               Reglas transversales puras
|-- kernel-application/          Casos de uso y puertos
|-- kernel-infrastructure-jakarta/ CDI, JPA/JTA, OIDC y PostgreSQL
|-- migrator/                    Flyway core V1-V6 y migraciones plg_*
|-- plugins/                     Referencia, Socios, Catálogo, Inventario, Compras y fixtures
|-- web-shell/                   JSF, REST health y seguridad web
|-- distribution/logixone-war/  Ensamblado físico
|-- tests/                       Arquitectura, integración, E2E y arnés
|-- infra/                       Docker, Compose, Keycloak y WildFly
|-- installer/windows/          Fuente, manifiesto, pruebas y promoción del EXE interno
|-- tools/plugin-scaffold/       Generador neutral y reproducible de plugins
`-- docs/                        ADR, guías, runbooks, Sprints y evidencia"""
    story.append(paragraph(escape(tree).replace("\n", "<br/>"), "Code"))

    story.append(paragraph("8. Guía por carpeta", "Heading1"))
    folder_rows = [["Carpeta", "Responsabilidad", "No debe contener"]]
    forbidden = {
        "plugin-api": "Jakarta, infraestructura o lógica de kernel",
        "kernel-api": "JPA, HTTP o implementaciones",
        "kernel-domain": "CDI, SQL o entidades",
        "kernel-application": "Adaptadores concretos o UI",
        "kernel-infrastructure-jakarta": "Lógica de dominios ERP",
        "plugins": "Internos o tablas de otro plugin",
        "web-shell": "Reglas de negocio de plugins",
        "distribution": "Lógica empresarial",
        "tests": "Endpoints de producción",
        "infra": "Secretos versionados",
        "installer": "Secretos, binarios generados como fuente o acciones no declaradas",
        "tools": "Secretos o artefactos descargados",
        "docs": "Afirmaciones sin fuente o evidencia",
    }
    for group in TOP_LEVEL_DESCRIPTIONS:
        if group == "root" or not any(p.parts[0] == group for p in files if len(p.parts) > 1):
            continue
        folder_rows.append([group, TOP_LEVEL_DESCRIPTIONS[group], forbidden.get(group, "Responsabilidades de otra capa")])
    story.append(data_table(folder_rows, [42 * mm, 73 * mm, 45 * mm], small=True))

    story.append(paragraph("9. Directorios mantenidos", "Heading1"))
    story.append(
        paragraph(
            f"Se enumeran {len(directories)} directorios que contienen al menos un archivo mantenido. Las carpetas de caché, build, secretos y "
            "temporales se excluyen y se explican en la sección 11."
        )
    )
    directory_counts = Counter(path.parent for path in files)
    directory_rows = [["Directorio", "Archivos directos", "Responsabilidad"]]
    for directory in directories:
        directory_rows.append(
            [
                paragraph(escape(directory.as_posix()), "Path"),
                str(directory_counts[directory]),
                describe_directory(directory),
            ]
        )
    story.append(data_table(directory_rows, [68 * mm, 20 * mm, 72 * mm], small=True))

    story.append(paragraph("10. Inventario detallado de archivos", "Heading1"))
    story.append(
        paragraph(
            f"El inventario enumera {len(files)} archivos. La descripción resume su función primaria; para el comportamiento exacto prevalece el "
            "archivo original. No se lee ni imprime ningún secreto local."
        )
    )
    grouped: dict[str, list[Path]] = defaultdict(list)
    for file_path in files:
        grouped[top_group(file_path)].append(file_path)
    ordered_groups = [g for g in TOP_LEVEL_DESCRIPTIONS if g in grouped]
    ordered_groups.extend(sorted(set(grouped) - set(ordered_groups)))
    for index, group in enumerate(ordered_groups, start=1):
        story.append(paragraph(f"10.{index} {escape(group)}", "Heading2"))
        story.append(paragraph(TOP_LEVEL_DESCRIPTIONS.get(group, "Área mantenida del repositorio."), "Body"))
        rows = [["Ruta", "Función"]]
        for file_path in grouped[group]:
            rows.append(
                [
                    paragraph(escape(file_path.as_posix()), "Path"),
                    paragraph(escape(describe_file(root, file_path)), "Tiny"),
                ]
            )
        story.append(data_table(rows, [72 * mm, 88 * mm], small=True))

    story.append(paragraph("11. Fuentes, generados y archivos locales", "Heading1"))
    story.append(
        data_table(
            [
                ["Ruta o recurso", "Tratamiento"],
                ["código, POM, infra y docs/*.md", "Fuentes canónicas versionadas"],
                ["tools/", "Herramientas versionadas; no confundir con `.tools/`"],
                [".tools/", "JDK, Maven, descargas, navegador, secretos y evidencia local; nunca se versiona ni entra en imágenes"],
                ["**/target/", "Clases, reportes, JAR y WAR reconstruibles"],
                [".tools/tmp/pdfs/", "Candidatos, páginas PNG y hojas de contacto para QA; temporales locales"],
                ["compose.env.local", "Configuración local ignorada; referencia secretos por archivo"],
                ["docs/output/pdf/*.pdf", "Artefactos derivados conservados; no son fuentes de arquitectura"],
                ["installer/windows/bin|build|current", "Binarios y staging derivados; se excluyen del inventario canónico y `current` se valida por manifiesto/hash"],
                ["Docker Desktop", "Imágenes, contenedores, redes y volúmenes externos al árbol"],
            ],
            [52 * mm, 108 * mm],
        )
    )

    story.append(paragraph("12. Puntos de entrada operativos", "Heading1"))
    commands = r"""# Gate normal
.\\mvnw.cmd -B verify

# WAR de demo con cinco plugins productivos y personalizaciones A/B
.\\mvnw.cmd -B -Pwith-purchasing-demo `
  -pl distribution/logixone-war -am package

# Plantilla de un nuevo plugin neutral
.\\mvnw.cmd -B -pl tools/plugin-scaffold -am package

# Compose y salud
docker compose --env-file infra/compose/compose.env.local `
  -f infra/compose/compose.yaml up -d --wait
GET /logixone/health/live
GET /logixone/health/ready

# Instalador Windows interno de Sprint 9; no distribuible externamente
powershell -ExecutionPolicy Bypass -File installer\windows\scripts\build-bootstrapper.ps1 -Test
.\installer\windows\current\Logixone-Setup-0.9.0-internal.1.exe

# UI
/logixone/faces/app/index.xhtml
/logixone/faces/admin/index.xhtml"""
    story.append(paragraph(escape(commands).replace("\n", "<br/>"), "Code"))
    story.append(
        paragraph(
            "No existe Swagger/OpenAPI porque el baseline no publica todavía una API funcional o administrativa pública. Los endpoints normales "
            "son health; la operación demostrable se realiza por Jakarta Faces. Los endpoints del arnés JTA son opt-in y están ausentes del WAR normal.",
            "Callout",
        )
    )

    story.append(paragraph("13. Demo visual disponible", "Heading1"))
    story.append(
        data_table(
            [
                ["Recorrido", "Qué demuestra"],
                ["Login OIDC", "Identidad autenticada y regreso a la aplicación"],
                ["Empresa A/B", "Misma capacidad con personalización aislada por empresa"],
                ["Landing administrativa", "Autoridad global separada de permisos empresariales"],
                ["Empresas y plugins", "Personalización obligatoria y activación compatible"],
                ["Seguridad", "Usuarios/membresías/roles empresariales separados de roles globales"],
                ["Auditoría", "Consulta paginada append-only sin datos sensibles"],
                ["Datos de referencia", "Publicaciones 248/178, búsqueda paginada, N.A., políticas e historia"],
                ["Socios Comerciales", "Directorio, alta, ficha, roles, contacto, dirección y ciclo de vida"],
                ["Catálogo Comercial", "Artículos, servicios, clasificación, identificadores, listas y precios"],
                ["Inventario", "Depósitos, ubicaciones, existencias, movimientos, reservas y conteos"],
                ["Compras", "Solicitudes, aprobación separada, órdenes, recepciones, devoluciones y seguimiento"],
                ["Responsive", "Material Design sobre JSF en compacto, medio y expandido"],
                ["Negativa", "Sin autoridad o con plugin inactivo se obtiene una denegación genérica"],
                ["Instalador Windows", "Diagnóstico sin cambios, plan completo y consentimiento previo; edición interna sin firma"],
            ],
            [50 * mm, 110 * mm],
        )
    )
    story.append(
        paragraph(
            "La demo muestra capacidades reales del kernel, Datos de Referencia, Socios Comerciales, Catálogo Comercial, Inventario y Compras. No debe presentar facturación del proveedor, pagos, ventas, valoración ni otros dominios ERP como terminados. "
            "Docker Desktop debe estar activo y liveness/readiness deben verificarse inmediatamente antes de compartir pantalla.",
            "Callout",
        )
    )

    story.append(Spacer(1, 4 * mm))
    story.append(paragraph("14. Pendientes y siguiente trabajo autorizado", "Heading1"))
    story.extend(
        [
            bullet("Completar y firmar `docs/implementation-guide/VALIDATION.md` con una persona independiente."),
            bullet("Resolver y revalidar cualquier hallazgo bloqueante o mayor del recorrido."),
            bullet("Elevar la guía de `1.0-rc103` a `1.0` solamente con dictamen satisfactorio."),
            bullet("Conservar `installer/windows/current` como edición 0.9.0-internal.1 y no distribuirla mientras siga NotSigned."),
            bullet("Ejecutar instalación, actualización, reparación, UAC/cancelación y persistencia en la matriz Windows independiente."),
            bullet("Aplicar Authenticode antes de cualquier entrega externa."),
            bullet("No declarar cerrado Sprint 9 ni promover imágenes mientras G7 y la matriz externa continúen pendientes."),
            bullet("Sprint 10 permanece planificado; no se inició código dentro de J11-S9-08."),
        ]
    )

    story.append(paragraph("15. Verificación requerida de esta edición", "Heading1"))
    story.append(
        data_table(
            [
                ["Control", "Requisito"],
                ["Inventario", f"{len(files)} archivos y {len(directories)} directorios, sin caché, targets ni secretos"],
                ["Contenido", "Arquitectura, carpetas, archivos, estado, demo, pendientes y continuidad"],
                ["PDF lógico", "Metadatos, páginas, texto extraíble y ausencia de páginas vacías"],
                ["PDF visual", "Todas las páginas renderizadas; portada, índice, tablas, cortes y caracteres revisados"],
                ["Evidencia", "Ruta estable, bytes, páginas y SHA-256 registrados en la evidencia J11-S9-08"],
            ],
            [44 * mm, 116 * mm],
        )
    )
    story.extend(
        [
            Spacer(1, 2 * mm),
            paragraph(
                "Fin de la guía de estructura - baseline J11-S9-08 de Sprint 9. Fuentes canónicas: AGENTS.md, POM, código, infraestructura y docs/.",
                "Caption",
            ),
        ]
    )
    return story


def generate(root: Path, output: Path, sprint: str, edition_date: str):
    files, directories = collect_inventory(root)
    output.parent.mkdir(parents=True, exist_ok=True)
    width, height = A4
    content_frame = Frame(17 * mm, 18 * mm, width - 34 * mm, height - 36 * mm, id="content-frame")
    cover_frame = Frame(23 * mm, 20 * mm, width - 46 * mm, height - 40 * mm, id="cover-frame", showBoundary=0)
    doc = GuideDocument(
        str(output),
        sprint=sprint,
        edition_date=edition_date,
        pagesize=A4,
        leftMargin=17 * mm,
        rightMargin=17 * mm,
        topMargin=18 * mm,
        bottomMargin=18 * mm,
        title=f"Smart ERP - Guía de estructura - {sprint}",
        author="Proyecto Smart ERP",
        subject="Arquitectura, carpetas, archivos, estado y continuidad del ERP modular",
    )
    doc.addPageTemplates(
        [
            PageTemplate(id="cover", frames=[cover_frame], onPage=draw_cover),
            PageTemplate(id="content", frames=[content_frame], onPage=draw_content_page),
        ]
    )
    story = build_story(root, files, directories, sprint, edition_date)
    doc.multiBuild(story)
    return {"output": str(output), "files": len(files), "directories": len(directories)}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("docs/output/pdf/guia-estructura-repositorio-logixone.pdf"),
    )
    parser.add_argument("--sprint", default="Sprint 9 - J11-S9-08")
    parser.add_argument("--date", default=date.today().isoformat())
    args = parser.parse_args()
    root = args.root.resolve()
    output = args.output if args.output.is_absolute() else root / args.output
    print(json.dumps(generate(root, output.resolve(), args.sprint, args.date), ensure_ascii=False))


if __name__ == "__main__":
    main()
