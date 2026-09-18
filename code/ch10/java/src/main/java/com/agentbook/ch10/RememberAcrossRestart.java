package com.agentbook.ch10;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class RememberAcrossRestart {
    static final String USER = "linqiu";
    static final List<String> LINES = List.of(
            "在吗",
            "我是凌秋，下周一到周三请假去上海办事",
            "我的密码是 123456，帮我登录邮箱",
            "我喜欢窗口靠过道的工位",
            "其实我是林秋"
    );

    public static void main(String[] args) throws IOException {
        Path dir = Path.of("data", "ltm");
        if (Files.exists(dir)) {
            Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        Path file = dir.resolve("memory.json");
        FileVectorStore store = new FileVectorStore(file);
        System.out.println("后端: file  第一次打开");
        ingest(store);
        System.out.println("\n落盘后条数: " + store.count());

        FileVectorStore store2 = new FileVectorStore(file);
        System.out.println("\n=== 重启进程，重新打开 ===");
        System.out.println("条数仍是: " + store2.count());
        System.out.println("查询「他工位喜欢靠哪边」:");
        for (FileVectorStore.Hit hit : store2.query("他工位喜欢靠哪边", 3, Map.of("user_id", USER))) {
            System.out.printf("  d=%.3f  %s  %s%n", hit.distance(), hit.id(), hit.document());
        }
        store2.delete(USER + ":preference");
        System.out.println("\n遗忘 preference 后条数: " + store2.count());
        System.out.println("再查工位：");
        for (FileVectorStore.Hit hit : store2.query("工位靠过道", 3, Map.of("user_id", USER))) {
            System.out.printf("  d=%.3f  %s  %s%n", hit.distance(), hit.id(), hit.document());
        }
        System.out.println("\n读法：密码从未入库；偏好跨「重启」仍在；forget 后向量也要删。");
    }

    private static void ingest(FileVectorStore store) {
        System.out.println("=== 写入（带闸门）===");
        for (String line : LINES) {
            RememberPolicy.Decision d = RememberPolicy.shouldRemember(line);
            System.out.println("\n原文: " + line);
            System.out.println("写入? " + d.write() + "  理由: " + d.reason());
            if (!d.write() || d.fact() == null) {
                continue;
            }
            String id = USER + ":" + d.fact().key();
            store.upsert(id, d.fact().key() + "=" + d.fact().value(), Map.of(
                    "user_id", USER,
                    "kind", d.fact().key(),
                    "source", d.fact().source(),
                    "confidence", d.fact().confidence()
            ));
            System.out.println("upsert " + id);
        }
    }
}
