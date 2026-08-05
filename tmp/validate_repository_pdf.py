from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import pdfplumber
from pypdf import PdfReader


parser = argparse.ArgumentParser()
parser.add_argument(
    "pdf",
    nargs="?",
    type=Path,
    default=Path("docs/output/pdf/guia-estructura-repositorio-logixone.pdf"),
)
pdf_path = parser.parse_args().pdf
reader = PdfReader(str(pdf_path))
texts = [(page.extract_text() or "") for page in reader.pages]

with pdfplumber.open(str(pdf_path)) as document:
    pdfplumber_blank = [
        index + 1
        for index, page in enumerate(document.pages)
        if not (page.extract_text() or "").strip()
    ]

root = reader.trailer.get("/Root") or {}
names = root.get("/Names") or {}
page_actions = []
for index, page in enumerate(reader.pages, start=1):
    if page.get("/AA") or page.get("/A"):
        page_actions.append(index)
result = {
    "pages": len(reader.pages),
    "bytes": pdf_path.stat().st_size,
    "sha256": hashlib.sha256(pdf_path.read_bytes()).hexdigest().upper(),
    "encrypted": reader.is_encrypted,
    "metadata": {str(key): str(value) for key, value in (reader.metadata or {}).items()},
    "blank_pypdf": [index + 1 for index, text in enumerate(texts) if not text.strip()],
    "blank_pdfplumber": pdfplumber_blank,
    "replacement_pages": [index + 1 for index, text in enumerate(texts) if "\ufffd" in text],
    "control_codes": sorted(
        {
            ord(character)
            for text in texts
            for character in text
            if ord(character) < 32 and character not in "\n\r\t"
        }
    ),
    "acroform": bool(root.get("/AcroForm")),
    "javascript_names": bool(names.get("/JavaScript")),
    "open_action": bool(root.get("/OpenAction")),
    "page_actions": page_actions,
}

print(json.dumps(result, ensure_ascii=False, indent=2))
