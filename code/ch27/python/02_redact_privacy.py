"""示例 2：日志里留单号和工具名，糊掉手机、身份证、邮箱、卡号。"""

from kit import RAW_LOG
from minitrace import leak, redact

if __name__ == "__main__":
    print("该留：trace_id、工具名、ORDER-88、延迟、Token、错误类型。")
    print("不该留：手机、身份证、邮箱、银行卡、未脱敏的整段 Prompt。\n")
    print("原文：")
    print(" ", RAW_LOG)
    clean = redact(RAW_LOG)
    print("脱敏：")
    print(" ", clean)
    print(f"原文泄漏={leak(RAW_LOG) or '无'}")
    print(f"脱敏后泄漏={leak(clean) or '无'}")
    print()
    checks = [
        ("原文能抓到四类隐私", set(leak(RAW_LOG)) == {"phone", "id", "email", "bank"}),
        ("脱敏后无泄漏", leak(clean) == []),
        ("仍能看见订单", "ORDER-88" in clean),
        ("仍能看见人名业务键", "林秋" in clean),
        ("卡号已糊", "6222021234567890123" not in clean and "[BANK]" in clean),
    ]
    for name, ok in checks:
        print(f"验收 {name}: {'OK' if ok else 'FAIL'}")
        if not ok:
            raise SystemExit(1)
    print("读法：单号是排障钥匙，手机号是事故。林秋作为演示名可留，证件不能留。")
