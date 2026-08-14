import json
import os
import re
import sys
from pathlib import Path
from urllib.parse import unquote


ROOT = Path.cwd()
EXCLUDED = {".git", ".tools", "target", "tmp", "__pycache__"}
TEXT_EXTENSIONS = {
    ".md",
    ".java",
    ".xml",
    ".xhtml",
    ".css",
    ".sql",
    ".json",
    ".yaml",
    ".yml",
    ".properties",
    ".py",
    ".sh",
    ".cmd",
}


def maintained_files() -> list[Path]:
    files: list[Path] = []
    for current_root, directories, filenames in os.walk(ROOT):
        directories[:] = [
            directory for directory in directories if directory not in EXCLUDED
        ]
        current = Path(current_root)
        files.extend(current / filename for filename in filenames)
    return files


def markdown_files() -> list[Path]:
    return [path for path in maintained_files() if path.suffix.lower() == ".md"]


def maintained_text_files() -> list[Path]:
    return [
        path
        for path in maintained_files()
        if path.suffix.lower() in TEXT_EXTENSIONS
    ]


def validate() -> dict[str, object]:
    broken_links: list[list[str]] = []
    encoding_errors: list[list[str]] = []
    mojibake_files: list[str] = []
    markdown = markdown_files()

    for path in markdown:
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeError as error:
            encoding_errors.append([path.as_posix(), str(error)])
            continue
        if "\ufffd" in text or "Ãƒ" in text or "Ã¢â‚¬" in text:
            mojibake_files.append(path.relative_to(ROOT).as_posix())
        for target in re.findall(r"\[[^\]]*\]\(([^)]+)\)", text):
            target = target.strip().strip("<>")
            if not target or target.startswith(("http://", "https://", "mailto:", "#")):
                continue
            local = unquote(target.split("#", 1)[0])
            if not local:
                continue
            resolved = (path.parent / local).resolve()
            if not resolved.exists():
                broken_links.append([path.relative_to(ROOT).as_posix(), target])

    secret_leaks: list[list[str]] = []
    maintained_text = maintained_text_files()
    for secret_file in (ROOT / ".tools" / "secrets").glob("*.txt"):
        value = secret_file.read_text(encoding="utf-8").strip()
        if not value:
            continue
        for path in maintained_text:
            try:
                if value in path.read_text(encoding="utf-8"):
                    secret_leaks.append(
                        [secret_file.name, path.relative_to(ROOT).as_posix()]
                    )
            except UnicodeError:
                pass

    return {
        "markdown_files": len(markdown),
        "broken_links": broken_links,
        "encoding_errors": encoding_errors,
        "mojibake_files": mojibake_files,
        "secret_leaks": secret_leaks,
    }


def main() -> int:
    result = validate()
    print(json.dumps(result, ensure_ascii=False, indent=2))
    failures = any(
        result[key]
        for key in (
            "broken_links",
            "encoding_errors",
            "mojibake_files",
            "secret_leaks",
        )
    )
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
