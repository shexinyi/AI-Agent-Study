package com.agentbook.ch10;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CrudDemo {
    public static void main(String[] args) throws IOException {
        Path dir = Path.of("data", "crud");
        if (Files.exists(dir)) {
            Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        FileVectorStore store = new FileVectorStore(dir.resolve("memory.json"));
        System.out.println("后端: file  目录: " + dir.toAbsolutePath());
        store.upsert("linqiu-pref", "我喜欢窗口靠过道的工位",
                Map.of("user_id", "linqiu", "kind", "preference"));
        store.upsert("linqiu-order", "ORDER-88 未发货，用户询问能否退款",
                Map.of("user_id", "linqiu", "kind", "episode"));
        store.upsert("zhangsan-pref", "张三喜欢靠墙的工位，不要过道",
                Map.of("user_id", "zhangsan", "kind", "preference"));
        store.upsert("linqiu-secret-should-not", "我的密码是 123456",
                Map.of("user_id", "linqiu", "kind", "secret"));
        System.out.println("写入 " + store.count() + " 条（演示里故意把密码也写进去，下一例会在闸门拦住）");
        dump("查询「工位靠哪边」Top-3，不过滤", store.query("工位靠哪边", 3, null));
        dump("同一查询 + where user_id=linqiu", store.query("工位靠哪边", 3, Map.of("user_id", "linqiu")));
        dump("同一查询 + where user_id=zhangsan", store.query("工位靠哪边", 3, Map.of("user_id", "zhangsan")));
        store.delete("linqiu-secret-should-not");
        System.out.println("\n删除密码条后 count=" + store.count());
        dump("再查「密码」", store.query("密码 123456", 2, null));
        System.out.println("\n读法：相似度会跨用户；租户隔离靠 metadata，不靠「模型自己知道」。");
    }

    private static void dump(String title, List<FileVectorStore.Hit> hits) {
        System.out.println("\n=== " + title + " ===");
        if (hits.isEmpty()) {
            System.out.println("  （空）");
            return;
        }
        for (FileVectorStore.Hit hit : hits) {
            System.out.printf("  d=%.3f  %-22s  %s  %s%n",
                    hit.distance(), hit.id(), hit.metadata().get("user_id"), hit.document());
        }
    }
}
