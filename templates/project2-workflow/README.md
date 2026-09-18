# 跨系统办理工作流（教学作品）

课堂教具：林秋事假 + ORDER-88。编排器分格，不是三个机器人聊天。**可退 ≠ 已退。** intern 不在 `refund_order` 白名单。

**STAR：** 旧客服只答一句或谎称已退。分格查制度与订单；ORDER-99 不建写单；食堂超时失败隔离。未确认只有 pending。

## 运行

```bash
python python/run.py
```

```powershell
cd java
mvn -q -DskipTests compile exec:java "-Dexec.mainClass=com.demo.Flow"
```

完整失败四件套见教程第 31 章。
