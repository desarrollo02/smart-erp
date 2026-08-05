import json
from pathlib import Path
from pypdf import PdfReader


pdf = Path(__file__).resolve().parents[3] / "docs" / "output" / "pdf" / "guia-estructura-repositorio-logixone.pdf"
reader = PdfReader(str(pdf))
texts = [(page.extract_text() or "").strip() for page in reader.pages]
result = {
    "pages": len(reader.pages),
    "empty_pages": [index + 1 for index, text in enumerate(texts) if not text],
    "replacement_char_pages": [index + 1 for index, text in enumerate(texts) if "\ufffd" in text],
    "has_installer_section": any("Instalador Windows" in text for text in texts),
    "has_internal_restriction": any("INTERNAL_UNSIGNED" in text for text in texts),
    "title": (reader.metadata or {}).get("/Title"),
    "subject": (reader.metadata or {}).get("/Subject"),
    "text_characters": sum(len(text) for text in texts),
}
print(json.dumps(result, ensure_ascii=False, indent=2))
