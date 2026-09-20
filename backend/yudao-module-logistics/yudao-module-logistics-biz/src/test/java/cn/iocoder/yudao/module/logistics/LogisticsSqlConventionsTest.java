package cn.iocoder.yudao.module.logistics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 物流域 SQL 文件的写作纪律测试。
 *
 * <p><b>为什么需要它</b>：MySQL 只把 `-- ` 后面跟**空格**的行当注释；`--（` 这种（`--` 紧跟中文全角
 * 括号）会被当成语句解析，导入时报 `ERROR 1064`，而报错位置指向**文件头几行**，看起来完全莫名其妙。
 * 这个坑在 V2c 与 V3 两票里各踩过一次，写进了注释还是再犯——所以用测试守住，不再靠记性。
 *
 * <p>另外顺手守住第二条：SQL 里不要出现 `-- ` 后紧跟制表符的写法（同样不是合法注释）。
 */
public class LogisticsSqlConventionsTest {

    /**
     * `--` 后面不是空格（行尾也算合法：单独一行 `--`）的行
     */
    private static final Pattern BAD_COMMENT = Pattern.compile("^--[^ \\t\\r\\n]");

    @Test
    public void testNoCommentWithoutSpaceAfterDoubleDash() throws IOException {
        // 模块在 backend/yudao-module-logistics/yudao-module-logistics-biz，SQL 在 backend/sql/mysql
        Path sqlDir = Path.of("..", "..", "sql", "mysql");
        assertTrue(Files.isDirectory(sqlDir), "找不到 SQL 目录：" + sqlDir.toAbsolutePath());

        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(sqlDir, 1)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().startsWith("logistics-"))
                    .filter(p -> p.toString().endsWith(".sql"))
                    .collect(Collectors.toList())) {
                List<String> lines = Files.readAllLines(path);
                for (int i = 0; i < lines.size(); i++) {
                    if (BAD_COMMENT.matcher(lines.get(i)).find()) {
                        violations.add(path.getFileName() + ":" + (i + 1) + " → " + lines.get(i));
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "MySQL 只认「--」后跟空格的注释，下面这些行会被当成语句、导入即报 1064：\n"
                        + String.join("\n", violations));
    }

}
