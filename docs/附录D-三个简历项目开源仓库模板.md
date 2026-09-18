# 附录 D 三个简历项目的开源仓库模板

> 把第 30–32 章收成三个 **可以公开** 的小仓库骨架。完整教具仍在 `code/ch30`–`code/ch32`。模板只保证：有 README、有 `.env.example`、无密钥、能跑一条最小演示。
>
> 路径：`templates/project1-kb/`、`templates/project2-workflow/`、`templates/project3-service/`
>
> 教学假设：林秋 / ORDER-88 是课堂名。扫描是正则教具。可退 ≠ 已退。不写 99%。

---

## D.1 三个仓库各卖一句

| 目录 | 简历项目 | 公开页第一句 |
| --- | --- | --- |
| `project1-kb` | 制度问答 | 带引用和 ACL 的只读 RAG，金标进 CI |
| `project2-workflow` | 跨系统办理 | 编排器分格，写退款 HITL，失败路径可演示 |
| `project3-service` | 生产门禁 | 鉴权 / 配额 / 脱敏审计，401 与 429 可复现 |

不要合成「超级智能体」一个仓库糊三锅。面试官要三张边界图。

---

## D.2 公开前清单（第 34 章尺子）

- [ ] README 写明 **教学 / 课堂**，并写不做清单  
- [ ] 只有 `.env.example`，没有 `.env`  
- [ ] 无 `sk-`、无明文手机号  
- [ ] 演示命令可复制；PowerShell 的 `-D` 加引号  
- [ ] STAR 四行；不写 99%  
- [ ] 项目 1 不调 `refund_order`；项目 2 可退未执行；项目 3 intern 第 4 次 429  

示例 3 会扫描三份模板，脏文件应不存在。

---

## D.3 怎么用

```bash
# 复制一份出去单独 git init，不要把全书密钥历史带上
cp -r templates/project1-kb ~/work/leave-kb-agent
```

Windows 用资源管理器复制，或：

```powershell
Copy-Item -Recurse templates\project1-kb D:\work\leave-kb-agent
```

然后只把 **这一层** 推到 GitHub。全书 `D:\code\ai_agent` 是教材，不是你的作品仓。

---

回到 [附录 C](./附录C-推荐阅读与官方文档导航.md) · 总目 [附录](./附录.md)
