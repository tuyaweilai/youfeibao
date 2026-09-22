package cn.iocoder.yudao.module.logistics.enums;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LogisticsPermissionMenuEnum} 的单元测试。
 *
 * <p>这张表是「权限行在菜单管理里长什么样」的单一来源，而它又是**手写的**，所以三件事必须有测试守着：
 * 权限清单里每一条都登记了归集方式（漏了就又出现 {@code logistics:xxx:delete} 这样的裸标识行）、
 * 同一父页面下菜单名不重（重了 {@code MenuServiceImpl} 会抛「菜单名重复」）、父页面组件名真的存在
 * （写错了同步只能回退到根节点）。
 */
public class LogisticsPermissionMenuEnumTest {

    /**
     * {@code logistics-menu.sql} 里页面菜单的组件名（第 9 列，形如 {@code 'LogisticsVehicle'}）
     */
    private static final Pattern COMPONENT_NAME = Pattern.compile("'(Logistics[A-Za-z]+)'");

    @Test
    public void testEveryPermissionHasPlacement() {
        for (String permission : LogisticsRoleEnum.allPermissions()) {
            assertNotNull(LogisticsPermissionMenuEnum.ofPermission(permission),
                    "权限没有登记归集方式，菜单管理里会显示成裸标识：" + permission);
        }
    }

    @Test
    public void testNoDuplicatePermission() {
        // 静态查表按权限标识建索引，有重复就会被后一条覆盖——用条数相等把它挡在测试里
        Set<String> permissions = new HashSet<>();
        for (LogisticsPermissionMenuEnum item : LogisticsPermissionMenuEnum.values()) {
            assertTrue(permissions.add(item.getPermission()), "权限标识重复登记：" + item.getPermission());
        }
        assertEquals(LogisticsPermissionMenuEnum.values().length,
                LogisticsRoleEnum.allPermissions().size(),
                "归集表的条数应与权限清单一致（既不漏也不多）");
    }

    @Test
    public void testNamesUniqueWithinParent() {
        Map<String, Set<String>> namesByParent = new HashMap<>();
        for (LogisticsPermissionMenuEnum item : LogisticsPermissionMenuEnum.values()) {
            // 菜单名不能等于权限标识，否则等于没归集
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
        // 模块在 backend/yudao-module-logistics/yudao-module-logistics-biz，SQL 在 backend/sql/mysql
        Path sqlFile = Path.of("..", "..", "sql", "mysql", "logistics-menu.sql");
        assertTrue(Files.isRegularFile(sqlFile), "找不到菜单 SQL：" + sqlFile.toAbsolutePath());

        Set<String> componentNames = new HashSet<>();
        Matcher matcher = COMPONENT_NAME.matcher(Files.readString(sqlFile));
        while (matcher.find()) {
            componentNames.add(matcher.group(1));
        }
        assertFalse(componentNames.isEmpty(), "菜单 SQL 里没解析到页面组件名，解析逻辑要跟着改");

        List<String> unknown = new ArrayList<>();
        for (LogisticsPermissionMenuEnum item : LogisticsPermissionMenuEnum.values()) {
            if (item.getParentComponentName() != null && !componentNames.contains(item.getParentComponentName())) {
                unknown.add(item.getPermission() + " → " + item.getParentComponentName());
            }
        }
        assertTrue(unknown.isEmpty(),
                "归集表指向了不存在的页面组件名（同步会回退到根节点）：\n" + String.join("\n", unknown));
    }

}
