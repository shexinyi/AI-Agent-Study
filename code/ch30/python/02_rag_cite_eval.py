"""示例 2：金标回归。5 条全过才算项目 1 能演示。"""

from evalkit import evaluate

if __name__ == "__main__":
    print("尺子：must / must_not / 引用 id。关 ACL 的泄漏不进这张发布门。")
    print("禁止看着本次输出改金标。\n")
    rows = evaluate()
    failed = 0
    for case, ok, text in rows:
        print(f"{case.id:12} {'PASS' if ok else 'FAIL'} | {text}")
        if not ok:
            failed += 1
    print()
    if failed:
        print(f"验收 金标全过: FAIL ({failed} 条)")
        raise SystemExit(1)
    print("验收 金标全过: OK")
    print("读法：简历写「5 条金标 CI 红灯拦截」，不写准确率 99%。")
