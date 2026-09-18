"""示例 3：三份公开模板无密钥、有教学标记。"""

import re
from pathlib import Path

from root import repo_root

PHONE = re.compile(r"1\d{10}")
KEY = re.compile(r"sk-[A-Za-z0-9]{8,}")
SKIP = {".git", "target", "__pycache__", ".venv"}


def iter_files(root: Path):
    for p in root.rglob("*"):
        if not p.is_file():
            continue
        if any(part in SKIP for part in p.parts):
            continue
        yield p


if __name__ == "__main__":
    templates = repo_root() / "templates"
    projects = ["project1-kb", "project2-workflow", "project3-service"]
    print("扫描 templates/（教学正则，不是生产扫描器）\n")
    hits: list[str] = []
    missing_meta: list[str] = []
    for name in projects:
        folder = templates / name
        if not (folder / ".env.example").is_file():
            missing_meta.append(f"{name} 缺 .env.example")
        if not (folder / ".gitignore").is_file():
            missing_meta.append(f"{name} 缺 .gitignore")
        readme = (folder / "README.md").read_text(encoding="utf-8") if (folder / "README.md").is_file() else ""
        if "教学" not in readme:
            missing_meta.append(f"{name} README 未标明教学")
        if (folder / ".env").exists():
            hits.append(f"{name}/.env 不应存在")
        for f in iter_files(folder):
            text = f.read_text(encoding="utf-8", errors="ignore")
            rel = f.relative_to(templates).as_posix()
            if PHONE.search(text):
                hits.append(f"{rel}: 明文手机号")
            if KEY.search(text):
                hits.append(f"{rel}: 疑似 API Key")
            if f.name == ".env":
                hits.append(f"{rel}: 提交了 .env")
    print("泄漏:", hits or "无")
    print("缺元数据:", missing_meta or "无")
    print()
    checks = [
        ("三份模板都在", all((templates / n).is_dir() for n in projects)),
        ("无泄漏命中", hits == []),
        ("元数据齐全", missing_meta == []),
        ("总 README 有教学", "教学" in (templates / "README.md").read_text(encoding="utf-8")),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：作品仓单独 git init，不要推整个教程仓。")
