"""Compila el manual Windows y los manuales funcionales en un único PDF."""

from __future__ import annotations

import argparse
from pathlib import Path

from pypdf import PdfReader, PdfWriter


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--front", required=True, type=Path)
    parser.add_argument("--modules", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    parts = [
        ("Manual integrado: instalación y puesta en marcha", args.front),
        ("Apéndice A: Administración segura del kernel", args.modules / "01-manual-administracion-kernel.pdf"),
        ("Apéndice B: Datos de referencia", args.modules / "02-manual-datos-referencia.pdf"),
        ("Apéndice C: Socios comerciales", args.modules / "03-manual-socios-comerciales.pdf"),
        ("Apéndice D: Catálogo comercial", args.modules / "04-manual-catalogo-comercial.pdf"),
        ("Apéndice E: Inventario", args.modules / "05-manual-inventario.pdf"),
        ("Apéndice F: Compras", args.modules / "07-manual-compras.pdf"),
        ("Apéndice G: Panel de demostración", args.modules / "06-manual-panel-demostracion.pdf"),
    ]

    for title, path in parts:
        if not path.is_file():
            raise FileNotFoundError(f"Falta {title}: {path}")
        if path.stat().st_size < 10_000:
            raise ValueError(f"PDF anormalmente pequeño para {title}: {path}")

    writer = PdfWriter()
    total_pages = 0
    for title, path in parts:
        reader = PdfReader(path)
        if not reader.pages:
            raise ValueError(f"PDF sin páginas para {title}: {path}")
        writer.append(path, outline_item=title, import_outline=True)
        total_pages += len(reader.pages)

    writer.add_metadata(
        {
            "/Title": "LogixOne - Instalador Windows y puesta en marcha completa",
            "/Author": "Proyecto LogixOne / Smart ERP",
            "/Subject": "Instalación local, empresa, plugins, usuarios, permisos y operación",
            "/Keywords": "LogixOne, Windows, instalador, empresa, plugins, usuarios, soporte",
            "/Creator": "tools/generate_windows_onboarding_manual.ps1",
        }
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("wb") as stream:
        writer.write(stream)

    final_reader = PdfReader(args.output)
    if len(final_reader.pages) != total_pages:
        raise ValueError(
            f"Cantidad inesperada de páginas: {len(final_reader.pages)} != {total_pages}"
        )
    print(f"PDF compilado: {args.output} ({total_pages} páginas)")


if __name__ == "__main__":
    main()
