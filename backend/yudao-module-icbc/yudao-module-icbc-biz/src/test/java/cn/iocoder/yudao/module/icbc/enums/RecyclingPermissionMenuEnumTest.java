package cn.iocoder.yudao.module.icbc.enums;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RecyclingPermissionMenuEnum} 的单元测试。
 *
 * <p>回收域菜单是**两处**定义的：按钮行绝大多数在 {@code icbc-menu.sql}，剩下几条由
 * {@link RecyclingPermissionMenuEnum} 兜底。两处定义最容易出的错是重叠与漏项——重叠了同一个权限就有两套说法，
 * 漏项了菜单管理里又会出现 {@code icbc:xxx:yyyy} 这样的裸标识行。所以这里把「分工」锁成不变式：
 *
 * <blockquote>
 * 角色清单里的权限 = {@code icbc-menu.sql} 里编排过的 ∪ 本枚举登记的
 * </blockquote>
 *
 * <p>换句话说：新加权限时若进了 SQL，就**不要**再登记到这里；若暂时没进 SQL，就必须登记到这里。
 */
public class RecyclingPermissionMenuEnumTest {

    /**
     * {@code icbc-menu.sql} 里的 icbc 权限标识（第 3 列）
     */
    private static final Pattern SQL_PERMISSION = Pattern.compile("'(icbc:[a-z0-9:\\-]+)'");

    @Test
    public void testEnumAndMenuSqlPartitionRolePermissions() throws IOException {
        Set<String> sqlPermissions = permissionsInMenuSql();
        Set<String> enumPermissions = new LinkedHashSet<>();
        for (RecyclingPermissionMenuEnum item : RecyclingPermissionMenuEnum.values()) {
            assertTrue(enumPermissions.add(item.getPermission()), "权限重复登记：" + item.getPermission());
        }

        Set<String> uncovered = new TreeSet<>();
        Set<String> overlapped = new TreeSet<>();
        for (String permission : RecyclingRoleEnum.allPermissions()) {
            boolean inSql = sqlPermissions.contains(permission);
            boolean inEnum = enumPermissions.contains(permission);
            if (!inSql && !inEnum) {
                uncovered.add(permission);
            }
            if (inSql && inEnum) {
                overlapped.add(permission);
            }
        }

        assertTrue(uncovered.isEmpty(),
                "这些权限既没进 icbc-menu.sql 也没登记归集方式，菜单管理里会显示成裸标识：\n"
                        + String.join("\n", uncovered));
        assertTrue(overlapped.isEmpty(),
                "这些权限已经由 icbc-menu.sql 编排，不该再登记归集方式（同一权限会变成两套说法）：\n"
                        + String.join("\n", overlapped));
        // 反向也要成立：枚举里不该有角色清单之外的权限（那是登记错了名字）
        for (String permission : enumPermissions) {
            assertTrue(RecyclingRoleEnum.allPermissions().contains(permission),
                    "归集表里登记了角色清单之外的权限：" + permission);
        }
    }

    @Test
    public void testNamesUniqueWithinParent() {
        Map<String, Set<String>> namesByParent = new HashMap<>();
        for (RecyclingPermissionMenuEnum item : RecyclingPermissionMenuEnum.values()) {
            assertFalse(item.getName().equals(item.getPermission()),
                    "菜单名不该等于权限标识：" + item.getPermission());
            assertFalse(item.getName().isBlank(), "菜单名不能为空：" + item.getPermission());
            assertTrue(item.getSort() > 0, "排序要有值才排得出来：" + item.getPermission());
            String parent = item.getParentComponentName() == null ? "<root>" : item.getParentComponentName();
            assertTrue(namesByParent.computeIfAbsent(parent, key -> new HashSet<>()).add(item.getName()),
                    "同一父菜单下菜单名重复（MenuServiceImpl 会判为非法）：" + parent + " / " + item.getName());
        }
    }

    @Test
    public void testParentComponentNamesExistInMenuSql() throws IOException {
        Set<String> componentNames = new HashSet<>();
        Matcher matcher = Pattern.compile("'(Icbc[A-Za-z]+|ErpStock)'").matcher(Files.readString(menuSqlFile()));
        while (matcher.find()) {
            componentNames.add(matcher.group(1));
        }
        assertFalse(componentNames.isEmpty(), "菜单 SQL 里没解析到页面组件名，解析逻辑要跟着改");

        for (RecyclingPermissionMenuEnum item : RecyclingPermissionMenuEnum.values()) {
            if (item.getParentComponentName() != null) {
                assertTrue(componentNames.contains(item.getParentComponentName()),
                        "归集表指向了不存在的页面组件名（同步会回退到根节点）："
                                + item.getPermission() + " → " + item.getParentComponentName());
            }
        }
    }

    private static Set<String> permissionsInMenuSql() throws IOException {
        Set<String> permissions = new HashSet<>();
        Matcher matcher = SQL_PERMISSION.matcher(Files.readString(menuSqlFile()));
        while (matcher.find()) {
            permissions.add(matcher.group(1));
        }
        return permissions;
    }

    /**
     * 模块在 {@code backend/yudao-module-icbc/yudao-module-icbc-biz}，SQL 在 {@code backend/sql/mysql}
     */
    private static Path menuSqlFile() {
        Path path = Path.of("..", "..", "sql", "mysql", "icbc-menu.sql");
        assertTrue(Files.isRegularFile(path), "找不到菜单 SQL：" + path.toAbsolutePath());
        return path;
    }

}
