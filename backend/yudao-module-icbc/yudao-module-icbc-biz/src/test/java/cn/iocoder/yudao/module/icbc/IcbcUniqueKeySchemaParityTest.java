package cn.iocoder.yudao.module.icbc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 生产建表脚本（{@code backend/sql/mysql/*.sql}）与测试建表脚本
 * （{@code src/test/resources/sql/create_tables.sql}）的「租户级唯一键」必须一致。
 *
 * <p><b>为什么需要它</b>：单测跑在 H2 的 {@code create_tables.sql} 上，生产跑在 MySQL 的建表脚本上；
 * 两份脚本是**手写维护的两份**，没有任何机制保证它们说同一件事。一旦测试 schema 比生产更松，
 * 用例就是「在一个生产上不存在的宽容世界里全绿」——{@code #97} 正是这样：测试 schema 写的是
 * {@code UNIQUE (tenant_id, id_card_no)}，生产写的是 {@code UNIQUE (id_card_no, deleted)}，
 * 于是「同一身份证跨企业建档」的用例一直绿，真实库上第二家企业建档却直接 500。
 *
 * <p>所以这里不测行为（行为在测试 schema 上测，见 {@code IcbcTenantIsolationTest}），
 * 而是直接**比对两份建表脚本**，把「测试 schema 里声称的租户内唯一」钉到生产脚本上。
 *
 * <p><b>比对范围</b>：比两份脚本里都存在的 {@code icbc_*} 表的**全部唯一键**（不只含
 * {@code tenant_id} 的）。{@code #97} 的教训是「测试 schema 比生产松」，而「松」不限于租户维度——
 * 全局单号（{@code payee_no} / {@code partner_order_id} / {@code msg_id} 等）少建一个唯一键，
 * 同样会让用例在真实库上不成立。所以本测试不再按维度挑键，两侧全对齐、不留白名单。
 *
 * <p><b>唯一一处排除</b>：{@link #KEY_DECISION_PENDING} 里的表，其唯一键属于哪一层由别的票决定，
 * 本票不碰、比对也不纳入。它不是「差异白名单」——不描述任何一处具体差异，只划出本测试暂不拥有的表。
 *
 * <p><b>迁移脚本不算「建表脚本」</b>：迁移文件只有 {@code DROP INDEX} / {@code ADD UNIQUE KEY}、
 * 没有 {@code CREATE TABLE}，所以本测试不拿它来满足一份写错了的 {@code CREATE TABLE}
 * （新库只导建表脚本，建表脚本才是最终形状）。迁移脚本与建表脚本的一致性由
 * {@link #testPayeeUniqueKeyMigrationAddsExactlyTheCreateScriptKeys()} 单独钉。
 *
 * <p><b>没有白名单</b>：{@code #99} 把两侧键全对齐后，差异集合必须为空。留白名单等于
 * 「下一个同类差异只需再加一行」，{@code #97} 的 bug 就是这样藏了很久。
 */
public class IcbcUniqueKeySchemaParityTest {

    /** 生产建表脚本目录（相对本模块：backend/yudao-module-icbc/yudao-module-icbc-biz）。 */
    private static final Path MYSQL_SQL_DIR = Path.of("..", "..", "sql", "mysql");
    /** 测试建表脚本。 */
    private static final Path TEST_SCHEMA = Path.of("src", "test", "resources", "sql", "create_tables.sql");
    /** 收方档案唯一键迁移脚本（#97）。 */
    private static final Path PAYEE_MIGRATION = MYSQL_SQL_DIR.resolve("icbc-payee-unique-key.sql");

    /** #97 定下来的两个键：收方档案是「自然人 × 回收企业」这一层，唯一键必须含 tenant_id。 */
    private static final Set<Set<String>> PAYEE_TENANT_KEYS = Set.of(
            Set.of("tenant_id", "id_card_no", "deleted"),
            Set.of("tenant_id", "mobile", "deleted"));

    /** 收方档案的完整唯一键集合：#97 的两个租户级键 + #99 对齐的两个全局键。 */
    private static final Set<Set<String>> PAYEE_KEYS = Set.of(
            Set.of("tenant_id", "id_card_no", "deleted"),
            Set.of("tenant_id", "mobile", "deleted"),
            Set.of("payee_no"),
            Set.of("partner_payee_id"));

    /**
     * 键层归属由别的票决定、本票（{@code #99}）不碰的表：整体排除，等领域答复落定后再纳入。
     * 这不是「差异白名单」——它不描述任何一处具体差异，只划出本测试暂不拥有的表；
     * 且它**自清理**：被排除的表必须仍与生产有差异，若哪天两侧一致了，下面会断言失败、请把它删掉。
     * 目前只有 {@code icbc_payer_info}（#100：付方档案四个全局唯一键属于哪一层）。
     */
    private static final Set<String> KEY_DECISION_PENDING = Set.of("icbc_payer_info");

    private static final Pattern CREATE_TABLE = Pattern.compile(
            "CREATE TABLE(?: IF NOT EXISTS)?\\s+`?(\\w+)`?\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern MYSQL_UNIQUE_KEY = Pattern.compile(
            "UNIQUE KEY\\s+`?(\\w+)`?\\s*\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern H2_CONSTRAINT_UNIQUE = Pattern.compile(
            "CONSTRAINT\\s+\\w+\\s+UNIQUE\\s*\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATE_UNIQUE_INDEX = Pattern.compile(
            "CREATE UNIQUE INDEX(?: IF NOT EXISTS)?\\s+`?(\\w+)`?\\s+ON\\s+`?(\\w+)`?\\s*\\(([^)]*)\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ALTER_TABLE = Pattern.compile(
            "ALTER TABLE\\s+`?(\\w+)`?", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADD_UNIQUE_KEY = Pattern.compile(
            "ADD UNIQUE KEY\\s+`?(\\w+)`?\\s*\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);

    @Test
    public void testUniqueKeysMatchBetweenProductionAndTestSchema() throws IOException {
        Schema production = parseProduction();
        Schema test = parseTestSchema();

        // 只比两份脚本里都存在的 icbc_* 表
        Set<String> commonTables = new TreeSet<>(production.tables);
        commonTables.retainAll(test.tables);

        Map<String, String> drift = new TreeMap<>();
        for (String table : commonTables) {
            if (KEY_DECISION_PENDING.contains(table)) {
                // 排除项自清理：它必须真的还没对齐，否则就是白名单式的黑箱
                assertNotEquals(allKeys(production, table), allKeys(test, table),
                        table + " 已在两份脚本里一致，应从 KEY_DECISION_PENDING 移除");
                continue;
            }
            Set<Set<String>> productionKeys = allKeys(production, table);
            Set<Set<String>> testKeys = allKeys(test, table);
            if (!productionKeys.equals(testKeys)) {
                drift.put(table, String.format("生产=%s，测试=%s", describe(productionKeys), describe(testKeys)));
            }
        }

        String detail = drift.entrySet().stream()
                .map(e -> "  " + e.getKey() + "：" + e.getValue())
                .collect(Collectors.joining("\n"));
        assertTrue(drift.isEmpty(),
                "生产建表脚本与测试 schema 的唯一键不一致（只列了有差异的表）：\n" + detail
                        + "\n\n要求：两份脚本必须说同一件事。本票（#99）的教训是「测试 schema 比生产松 → "
                        + "用例在真实库上不成立」。修测试建表向生产看齐，不要放宽生产 schema 迁就测试。");
    }

    @Test
    public void testPayeeInfoUniqueKeysAreTheExpectedOnes() throws IOException {
        Schema production = parseProduction();
        assertEquals(PAYEE_KEYS, allKeys(production, "icbc_payee_info"),
                "icbc_payee_info 的唯一键被改动了：收方档案是「自然人 × 回收企业」这一层（ADR 0017 / #97），"
                        + "身份证与手机号只能在本租户内唯一；payee_no / partner_payee_id 是全局的（#99）。");
    }

    @Test
    public void testPayeeUniqueKeyMigrationAddsExactlyTheCreateScriptKeys() throws IOException {
        // 迁移脚本的 ADD UNIQUE KEY 必须与建表脚本的租户级键逐字一致：
        // 否则「新库」与「存量库迁移后」是两种形状，而单测只跑前者。
        Set<Set<String>> added = addUniqueKeysByTable(String.join("\n", readWithoutComments(PAYEE_MIGRATION)))
                .getOrDefault("icbc_payee_info", Set.of());
        assertEquals(PAYEE_TENANT_KEYS, added,
                "icbc-payee-unique-key.sql 里 icbc_payee_info 的 ADD UNIQUE KEY 与建表脚本对不上。");
        // 迁移脚本不建表（它是「存量库怎么换键」，不是最终形状）：所以上面那条比对把它排除在外，
        // 它自己的一致性由本用例负责。
        assertFalse(String.join("\n", readWithoutComments(PAYEE_MIGRATION)).toUpperCase().contains("CREATE TABLE"),
                "icbc-payee-unique-key.sql 不应该含 CREATE TABLE：最终形状只在 icbc_payee_info.sql 里。");
    }

    private static Set<Set<String>> allKeys(Schema schema, String table) {
        return new LinkedHashSet<>(schema.uniqueKeys.getOrDefault(table, Set.of()));
    }

    private static String describe(Set<Set<String>> keys) {
        return keys.stream()
                .map(columns -> columns.stream().sorted().collect(Collectors.joining(", ", "(", ")")))
                .sorted()
                .collect(Collectors.joining(" ", "[", "]"));
    }

    private Schema parseProduction() throws IOException {
        Schema schema = new Schema();
        try (Stream<Path> paths = Files.walk(MYSQL_SQL_DIR, 1)) {
            List<Path> files = paths.filter(p -> p.getFileName().toString().startsWith("icbc"))
                    .filter(p -> p.toString().endsWith(".sql"))
                    .sorted().collect(Collectors.toList());
            for (Path file : files) {
                parse(file, schema, MYSQL_UNIQUE_KEY, 2, true);
            }
        }
        return schema;
    }

    private Schema parseTestSchema() throws IOException {
        Schema schema = new Schema();
        parse(TEST_SCHEMA, schema, H2_CONSTRAINT_UNIQUE, 1, false);
        return schema;
    }

    /**
     * 解析一个 SQL 文件里的表与唯一键。
     *
     * @param insideTablePattern   CREATE TABLE 块内的唯一键写法（MySQL 是 {@code UNIQUE KEY}，H2 是 {@code CONSTRAINT ... UNIQUE}）
     * @param columnsGroup         上述正则里捕获列清单的分组号
     * @param parseAlterUniqueKey  是否解析 {@code ALTER TABLE ... ADD UNIQUE KEY}（如 {@code icbc-stock-ops.sql} 的存储过程）
     */
    private void parse(Path file, Schema schema, Pattern insideTablePattern, int columnsGroup,
                       boolean parseAlterUniqueKey) throws IOException {
        List<String> lines = readWithoutComments(file);

        // 1. CREATE TABLE 块：表名 + 块内的唯一键
        Set<String> tablesCreatedInFile = new LinkedHashSet<>();
        List<String> tableBlocks = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            Matcher matcher = CREATE_TABLE.matcher(lines.get(i));
            if (!matcher.find()) {
                continue;
            }
            schema.tables.add(matcher.group(1));
            tablesCreatedInFile.add(matcher.group(1));
            StringBuilder block = new StringBuilder(lines.get(i));
            int j = i + 1;
            while (j < lines.size()) {
                block.append('\n').append(lines.get(j));
                boolean end = lines.get(j).stripLeading().startsWith(")");
                j++;
                if (end) {
                    break;
                }
            }
            tableBlocks.add(block.toString());
            i = j - 1;
        }
        for (String block : tableBlocks) {
            String table = tableOfCreateTable(block);
            Matcher matcher = insideTablePattern.matcher(block);
            while (matcher.find()) {
                schema.addKey(table, columns(matcher.group(columnsGroup)));
            }
        }

        // 2. 独立语句：CREATE UNIQUE INDEX ... ON <表>(列)（H2 脚本用这种写法建唯一索引）
        String text = String.join("\n", lines);
        Matcher indexMatcher = CREATE_UNIQUE_INDEX.matcher(text);
        while (indexMatcher.find()) {
            if (tablesCreatedInFile.contains(indexMatcher.group(2))) {
                schema.addKey(indexMatcher.group(2), columns(indexMatcher.group(3)));
            }
        }

        // 3. ALTER TABLE ... ADD UNIQUE KEY
        //    只认本文件里 CREATE TABLE 过的表：迁移脚本（只有 DROP / ADD，没有建表）不是「建表脚本」，
        //    不能拿它来满足一份写错了的 CREATE TABLE。
        if (parseAlterUniqueKey) {
            addUniqueKeysByTable(text).forEach((table, keys) -> {
                if (tablesCreatedInFile.contains(table)) {
                    keys.forEach(key -> schema.addKey(table, key));
                }
            });
        }
    }

    /** 收集 {@code ALTER TABLE <表> ... ADD UNIQUE KEY ... (列)}：把每条 ADD 归到它前面最近的那条 ALTER TABLE。 */
    private static Map<String, Set<Set<String>>> addUniqueKeysByTable(String text) {
        List<Integer> alterPositions = new ArrayList<>();
        List<String> alterTables = new ArrayList<>();
        Matcher alterMatcher = ALTER_TABLE.matcher(text);
        while (alterMatcher.find()) {
            alterPositions.add(alterMatcher.start());
            alterTables.add(alterMatcher.group(1));
        }
        Map<String, Set<Set<String>>> result = new LinkedHashMap<>();
        Matcher addMatcher = ADD_UNIQUE_KEY.matcher(text);
        while (addMatcher.find()) {
            for (int i = alterPositions.size() - 1; i >= 0; i--) {
                if (alterPositions.get(i) < addMatcher.start()) {
                    result.computeIfAbsent(alterTables.get(i), key -> new LinkedHashSet<>())
                            .add(columns(addMatcher.group(2)));
                    break;
                }
            }
        }
        return result;
    }

    /** 去掉整行 `--` 注释：避免注释里的示例 DDL 被当成本文。 */
    private static List<String> readWithoutComments(Path file) throws IOException {
        return Files.readAllLines(file).stream()
                .filter(line -> !line.stripLeading().startsWith("--"))
                .collect(Collectors.toList());
    }

    private static String tableOfCreateTable(String block) {
        Matcher matcher = CREATE_TABLE.matcher(block);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** 把 `` `a`, `b` `` / `a, b` 解析成有序的列集合。 */
    private static Set<String> columns(String raw) {
        return Stream.of(raw.split(","))
                .map(column -> column.trim().replace("`", ""))
                .filter(column -> !column.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** 逐文件的表清单与唯一键清单。 */
    private static final class Schema {

        private final Set<String> tables = new LinkedHashSet<>();
        private final Map<String, Set<Set<String>>> uniqueKeys = new LinkedHashMap<>();

        private void addKey(String table, Set<String> columns) {
            uniqueKeys.computeIfAbsent(table, key -> new LinkedHashSet<>()).add(columns);
        }

    }

}
