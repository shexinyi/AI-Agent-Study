"""定位仓库根：含 docs/ 与 templates/。"""

from pathlib import Path


def repo_root() -> Path:
    here = Path(__file__).resolve()
    for p in [here, *here.parents]:
        if (p / "docs").is_dir() and (p / "templates").is_dir():
            return p
    raise SystemExit("找不到仓库根（需要 docs/ 与 templates/）")
