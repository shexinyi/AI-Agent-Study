package com.agentbook.ch30;

import java.util.List;

public final class StarDemoScript {
    static final List<String> STAR = List.of(
            "情境：制度口口相传，同事把事假说成 5 天，实习生还能翻到废止稿。",
            "任务：两周内上线只读的制度问答，必须带引用和角色过滤。",
            "行动：关键词检索 + 引用 [id]；intern 只见 public；5 条金标进 CI；范围外【不足】。",
            "结果：实习生问「最多几天」稳定答 10 个工作日并指向 [leave10]；关 ACL 对照可复现 5 天泄漏。"
    );
    static final String SCRIPT = """
            90 秒演示：
            1. 用实习生身份问「事假每年上限几天？」——指向 [leave10]，10 个工作日。
            2. 问「年假呢？」「ORDER-88 能不能退？」——【不足】，说明范围裁剪。
            3. 把 apply_acl=False 再问「最多几天」——出现 5 天，说明门禁不在 Prompt 里。
            4. 打开金标表，5 条 PASS，强调没改 must。
            追问预案：
            - Embedding 呢？两周切片用关键词，生产再换向量，尺子不变。
            - 和 ChatGPT 比？闭卷会编 5 天；我们开卷且拒答范围外。
            - 退款呢？那是项目 2，写工具必须 HITL。
            """.strip();

    public static void main(String[] args) {
        System.out.println("简历四行（STAR）。数字来自示例 2 的金标，不是 99%。");
        System.out.println();
        for (int i = 0; i < STAR.size(); i++) {
            System.out.println((i + 1) + ". " + STAR.get(i));
        }
        System.out.println();
        System.out.println(SCRIPT);
        System.out.println();
        check("四行都有", STAR.size() == 4);
        check("有引用和 ACL", STAR.get(3).contains("leave10") && STAR.get(3).contains("ACL"));
        check("不写 99%", STAR.stream().noneMatch(x -> x.contains("99%")));
        check("演示含对照病", SCRIPT.contains("apply_acl=False") && SCRIPT.contains("5 天"));
        check("追问指向项目 2", SCRIPT.contains("项目 2"));
        System.out.println("读法：演示先成功路径，再主动打开失败对照。面试官最爱这一下。");
    }

    private static void check(String name, boolean ok) {
        System.out.println("验收 " + name + ": " + (ok ? "OK" : "FAIL"));
        if (!ok) {
            System.exit(1);
        }
    }
}
