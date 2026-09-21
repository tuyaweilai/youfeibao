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
 * <p><b>比对范围</b>：只比**租户级唯一键**（列集合含 {@code tenant_id} 的唯一键），
 * 且只比两份脚本里都存在的 {@code icbc_*} 表。理由：
 * <ul>
 *   <li>本类 bug 的全部风险都在这一维——测试 schema 声称「租户内唯一」、生产没声称，或者反过来；
 *       不含 {@code tenant_id} 的键（我们生成的单号 {@code acquisition_no} / {@code station_code} /
 *       {@code payee_no}、工行的 {@code msg_id} / {@code notify_id}、{@code jti} 等）
 *       绝大多数是**合理的全局键**，混进来只会是噪音。</li>
 *   <li>H2 schema 是手工维护的精简版，非租户维度上本来就有大量合法差异（少建键、少列），
 *       全量对齐得给近十张表挂白名单，等于把噪音写死。</li>
 * </ul>
 *
 * <p><b>迁移脚本不算「建表脚本」</b>：迁移文件只有 {@code DROP INDEX} / {@code ADD UNIQUE KEY}、
 * 没有 {@code CREATE TABLE}，所以本测试不拿它来满足一份写错了的 {@code CREATE TABLE}
 * （新库只导建表脚本，建表脚本才是最终形状）。迁移脚本与建表脚本的一致性由
 * {@link #testPayeeUniqueKeyMigrationAddsExactlyTheCreateScriptKeys()} 单独钉。
 *
 * <p><b>白名单</b>：{@link #KNOWN_DRIFT} 是两处与本票无关的既有差异，方向都是「测试 schema 比生产松」，
 * 属测试 schema 精简，本票不动。白名单是**自清理**的：差异集合必须**恰好等于**白名单，
 * 所以要么修差异、要么加白名单，不能沉默漂移。
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

    /**
     * 已知的、与本票（{@code #97}）无关的既有差异。键是表名，值是差异说明。
     * 差异集合与它必须严格相等：修好一处就该删一条，新漂移一处就会红。
     */
    private static final Map<String, String> KNOWN_DRIFT = new LinkedHashMap<>();

    static {
        KNOWN_DRIFT.put("icbc_invoice_order",
                "测试 schema 把生产的全局 uk_partner_order_id (partner_order_id) 写成了 (tenant_id, partner_order_id)，"
                        + "测试更松。工行合作方订单号是全局的，本票不放宽全局键语义。");
        KNOWN_DRIFT.put("icbc_payment_order",
                "测试 schema 没建生产的 (partner_order_id, deleted, tenant_id) / (order_no, deleted, tenant_id)，"
                        + "测试更松。属测试 schema 精简（付款并发兜底靠 Service 层），与本票无关。");
    }

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
    public void testTenantScopedUniqueKeysMatchBetweenProductionAndTestSchema() throws IOException {
        Schema production = parseProduction();
        Schema test = parseTestSchema();

        // 只比两份脚本里都存在的 icbc_* 表
        Set<String> commonTables = new TreeSet<>(production.tables);
        commonTables.retainAll(test.tables);

        Map<String, String> drift = new TreeMap<>();
        for (String table : commonTables) {
            Set<Set<String>> productionKeys = tenantScopedKeys(production, table);
            Set<Set<String>> testKeys = tenantScopedKeys(test, table);
            if (!productionKeys.equals(testKeys)) {
                drift.put(table, String.format("生产=%s，测试=%s", describe(productionKeys), describe(testKeys)));
            }
        }

        String detail = drift.entrySet().stream()
                .map(e -> "  " + e.getKey() + "：" + e.getValue()
                        + (KNOWN_DRIFT.containsKey(e.getKey()) ? "（已在白名单）" : ""))
                .collect(Collectors.joining("\n"));
        assertEquals(new TreeSet<>(KNOWN_DRIFT.keySet()), new TreeSet<>(drift.keySet()),
                "生产建表脚本与测试 schema 的租户级唯一键不一致（只列了有差异的表）：\n" + detail
                        + "\n\n要求：两份脚本必须说同一件事。本票（#97）的教训是「测试 schema 更松 → "
                        + "用例在真实库上不成立」。修差异，或确认它是有意为之再写进 KNOWN_DRIFT 并说明理由。");
    }

    @Test
    public void testPayeeInfoTenantScopedUniqueKeysAreTheExpectedOnes() throws IOException {
        Schema production = parseProduction();
        assertEquals(PAYEE_TENANT_KEYS, tenantScopedKeys(production, "icbc_payee_info"),
                "icbc_payee_info 的租户级唯一键被改动了：收方档案是「自然人 × 回收企业」这一层（ADR 0017 / #97），"
                        + "身份证与手机号都只能在本租户内唯一。");
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

    private static Set<Set<String>> tenantScopedKeys(Schema schema, String table) {
        return schema.uniqueKeys.getOrDefault(table, Set.of()).stream()
                .filter(columns -> columns.contains("tenant_id"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
