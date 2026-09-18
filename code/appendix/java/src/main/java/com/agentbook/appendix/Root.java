package com.agentbook.appendix;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Root {
    static Path repo() {
        Path here = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = here; p != null; p = p.getParent()) {
            if (Files.isDirectory(p.resolve("docs")) && Files.isDirectory(p.resolve("templates"))) {
                return p;
            }
        }
        throw new IllegalStateException("找不到仓库根（需要 docs/ 与 templates/）");
    }
}
