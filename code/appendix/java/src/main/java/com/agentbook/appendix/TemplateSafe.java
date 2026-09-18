package com.agentbook.appendix;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class TemplateSafe {
    static final Pattern PHONE = Pattern.compile("1\\d{10}");
    static final Pattern KEY = Pattern.compile("sk-[A-Za-z0-9]{8,}");
    static final Set<String> SKIP = Set.of(".git", "target", "__pycache__", ".venv");

    public static void main(String[] args) throws Exception {
        Path templates = Root.repo().resolve("templates");
        List<String> projects = List.of("project1-kb", "project2-workflow", "project3-service");
        System.out.println("扫描 templates/（教学正则，不是生产扫描器）\n");
        List<String> hits = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String name : projects) {
            Path folder = templates.resolve(name);
            if (!Files.isRegularFile(folder.resolve(".env.example"))) {
                missing.add(name + " 缺 .env.example");
            }
            if (!Files.isRegularFile(folder.resolve(".gitignore"))) {
                missing.add(name + " 缺 .gitignore");
            }
            Path readme = folder.resolve("README.md");
            String readmeText = Files.isRegularFile(readme) ? Files.readString(readme, StandardCharsets.UTF_8) : "";
            if (!readmeText.contains("教学")) {
                missing.add(name + " README 未标明教学");
            }
            if (Files.exists(folder.resolve(".env"))) {
                hits.add(name + "/.env 不应存在");
            }
            try (Stream<Path> walk = Files.walk(folder)) {
                walk.filter(Files::isRegularFile).forEach(f -> {
                    if (f.getNameCount() > 0 && SKIP.stream().anyMatch(s -> f.toString().contains(s))) {
                        return;
                    }
                    try {
                        String text = Files.readString(f, StandardCharsets.UTF_8);
                        String rel = templates.relativize(f).toString().replace('\\', '/');
                        if (PHONE.matcher(text).find()) {
                            hits.add(rel + ": 明文手机号");
                        }
                        if (KEY.matcher(text).find()) {
                            hits.add(rel + ": 疑似 API Key");
                        }
                    } catch (Exception ignored) {
                        // 二进制跳过
                    }
                });
            }
        }
        System.out.println("泄漏: " + (hits.isEmpty() ? "无" : hits));
        System.out.println("缺元数据: " + (missing.isEmpty() ? "无" : missing));
        System.out.println();
        String top = Files.readString(templates.resolve("README.md"), StandardCharsets.UTF_8);
        Object[][] checks = {
                {"三份模板都在", projects.stream().allMatch(n -> Files.isDirectory(templates.resolve(n)))},
                {"无泄漏命中", hits.isEmpty()},
                {"元数据齐全", missing.isEmpty()},
                {"总 README 有教学", top.contains("教学")},
        };
        for (Object[] c : checks) {
            boolean ok = (Boolean) c[1];
            System.out.println("验收 " + c[0] + ": " + (ok ? "OK" : "FAIL"));
            if (!ok) {
                System.exit(1);
            }
        }
        System.out.println("读法：作品仓单独 git init，不要推整个教程仓。");
    }
}
