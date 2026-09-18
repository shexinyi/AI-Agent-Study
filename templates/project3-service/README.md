# 可观测可限流的 Agent 服务（教学作品）

课堂教具：把办理流收成服务。Bearer 鉴权、实习生日配额 3 次、审计脱敏。不拉端口。教学币不是官网价。

**STAR：** 脚本泄漏手机号、配额可打穿。无 token → 401；intern 第 4 次 → 429；审计只留 `[PHONE]`。写退款仍须 HITL。框架二选一做深。

## 运行

```bash
python python/run.py
```

```powershell
cd java
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.demo.Svc"
```

完整看板与金标见教程第 32 章。
