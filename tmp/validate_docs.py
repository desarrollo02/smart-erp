import json
import re
from pathlib import Path
from urllib.parse import unquote


root = Path.cwd()
excluded = {".git", ".tools", "target", "tmp", "__pycache__"}
markdown = [
    path
    for path in root.rglob("*.md")
    if not any(part in excluded for part in path.relative_to(root).parts)
]
broken = []
encoding_errors = []
mojibake = []
for path in markdown:
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeError as error:
        encoding_errors.append([path.as_posix(), str(error)])
        continue
    if "\ufffd" in text or "Ã" in text or "â€" in text:
        mojibake.append(path.as_posix())
    for target in re.findall(r"\[[^\]]*\]\(([^)]+)\)", text):
        target = target.strip().strip("<>")
        if not target or target.startswith(("http://", "https://", "mailto:", "#")):
            continue
        local = unquote(target.split("#", 1)[0])
        if not local:
            continue
        resolved = (path.parent / local).resolve()
        if not resolved.exists():
            broken.append([path.relative_to(root).as_posix(), target])

secret_leaks = []
text_extensions = {".md", ".java", ".xml", ".xhtml", ".css", ".sql", ".json", ".yaml", ".yml", ".properties", ".py", ".sh", ".cmd"}
maintained_text = [
    path
    for path in root.rglob("*")
    if path.is_file()
    and path.suffix.lower() in text_extensions
    and not any(part in excluded for part in path.relative_to(root).parts)
]
for secret_file in (root / ".tools" / "secrets").glob("*.txt"):
    value = secret_file.read_text(encoding="utf-8").strip()
    if not value:
        continue
    for path in maintained_text:
        try:
            if value in path.read_text(encoding="utf-8"):
                secret_leaks.append([secret_file.name, path.relative_to(root).as_posix()])
        except UnicodeError:
            pass

print(json.dumps({
    "markdown_files": len(markdown),
    "broken_links": broken,
    "encoding_errors": encoding_errors,
    "mojibake_files": mojibake,
    "secret_leaks": secret_leaks,
}, ensure_ascii=False, indent=2))
