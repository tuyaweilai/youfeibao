package cn.iocoder.yudao.module.system.api.permission;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.menu.MenuSaveVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.enums.permission.MenuTypeEnum;
import cn.iocoder.yudao.module.system.service.permission.MenuService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MenuApiImpl} 的单元测试。
 *
 * <p>重点守 {@link MenuApiImpl#ensurePermissionMenu} 的**「只管未归集的行」**这条边界：
 * 归集（把裸权限标识行改成中文名 + 挂到页面下）要生效，但已经由菜单 SQL / 后台编排好的行
 * 一行都不能动——{@code icbc-menu.sql} 里同一个权限会挂在多个按钮上，被同步过程按单值收敛就坏了。
 */
public class MenuApiImplTest extends BaseMockitoUnitTest {

    @Mock
    private MenuService menuService;

    @InjectMocks
    private MenuApiImpl menuApi;

    @Test
    public void testEnsurePermissionMenu_notExists_creates() {
        when(menuService.getMenuIdListByPermissionFromCache("logistics:vehicle:create"))
                .thenReturn(Collections.emptyList());
        when(menuService.createMenu(any())).thenReturn(99L);

        Long id = menuApi.ensurePermissionMenu("车辆新增", "logistics:vehicle:create", 5L, 1);

        assertEquals(99L, id);
        ArgumentCaptor<MenuSaveVO> captor = ArgumentCaptor.forClass(MenuSaveVO.class);
        verify(menuService).createMenu(captor.capture());
        MenuSaveVO reqVO = captor.getValue();
        assertEquals("车辆新增", reqVO.getName());
        assertEquals("logistics:vehicle:create", reqVO.getPermission());
        assertEquals(MenuTypeEnum.BUTTON.getType(), reqVO.getType());
        assertEquals(5L, reqVO.getParentId());
        assertEquals(1, reqVO.getSort());
        verify(menuService, never()).updateMenu(any());
    }

    @Test
    public void testEnsurePermissionMenu_unplacedRow_regroupsInPlace() {
        // 早期同步过程落下的行：菜单名就是权限标识、挂根节点
        MenuDO unplaced = menu(7L, "logistics:transport-cost:delete", "logistics:transport-cost:delete", 0L, 0);
        when(menuService.getMenuIdListByPermissionFromCache("logistics:transport-cost:delete"))
                .thenReturn(Collections.singletonList(7L));
        when(menuService.getMenu(7L)).thenReturn(unplaced);

        Long id = menuApi.ensurePermissionMenu("运输费用删除", "logistics:transport-cost:delete", 5L, 9);

        // 行 id 不变（角色-菜单授权与租户套餐都挂在 id 上），只改名称与位置
        assertEquals(7L, id);
        ArgumentCaptor<MenuSaveVO> captor = ArgumentCaptor.forClass(MenuSaveVO.class);
        verify(menuService).updateMenu(captor.capture());
        MenuSaveVO reqVO = captor.getValue();
        assertEquals(7L, reqVO.getId());
        assertEquals("运输费用删除", reqVO.getName());
        assertEquals(5L, reqVO.getParentId());
        assertEquals(9, reqVO.getSort());
        verify(menuService, never()).createMenu(any());
    }

    @Test
    public void testEnsurePermissionMenu_alreadyPlaced_doesNothing() {
        // 已由菜单 SQL / 后台编排好的行：同一个权限可以有很多行，同步过程不该动它
        MenuDO placed = menu(5102L, "出售者新增", "icbc:payee-info:create", 5101L, 1);
        when(menuService.getMenuIdListByPermissionFromCache("icbc:payee-info:create"))
                .thenReturn(Collections.singletonList(5102L));
        when(menuService.getMenu(5102L)).thenReturn(placed);

        Long id = menuApi.ensurePermissionMenu("icbc:payee-info:create", "icbc:payee-info:create", null, 0);

        assertEquals(5102L, id);
        verify(menuService, never()).createMenu(any());
        verify(menuService, never()).updateMenu(any());
    }

    @Test
    public void testGetMenuIdByComponentName() {
        MenuDO page = menu(5101L, "收方档案", "icbc:payee-info:query", 5202L, 1);
        page.setComponentName("IcbcPayee");
        when(menuService.getMenuByComponentName("IcbcPayee")).thenReturn(page);
        when(menuService.getMenuByComponentName("NotExists")).thenReturn(null);

        assertEquals(5101L, menuApi.getMenuIdByComponentName("IcbcPayee"));
        assertNull(menuApi.getMenuIdByComponentName("NotExists"));
    }

    @Test
    public void testGetAncestorMenuIds_walksUpToRoot() {
        // 5479(司机端-交接查询) → 5444(司机档案) → 5441(物流管理) → 根
        when(menuService.getMenu(5479L)).thenReturn(menu(5479L, "司机端-交接查询", null, 5444L, 0));
        when(menuService.getMenu(5444L)).thenReturn(menu(5444L, "司机档案", null, 5441L, 1));
        when(menuService.getMenu(5441L)).thenReturn(menu(5441L, "物流管理", null, 0L, 1));

        Set<Long> ancestors = menuApi.getAncestorMenuIds(List.of(5479L));

        assertEquals(Set.of(5444L, 5441L), ancestors);
    }

    @Test
    public void testGetAncestorMenuIds_emptyInputOrMissingParent() {
        assertEquals(Collections.emptySet(), menuApi.getAncestorMenuIds(Collections.emptyList()));
        // 父菜单不存在时停下来，不把幽灵 id 加进授权集合
        when(menuService.getMenu(700L)).thenReturn(menu(700L, "孤儿行", null, 999L, 0));
        when(menuService.getMenu(999L)).thenReturn(null);
        assertEquals(Collections.emptySet(), menuApi.getAncestorMenuIds(List.of(700L)));
    }

    private static MenuDO menu(Long id, String name, String permission, Long parentId, Integer sort) {
        MenuDO menu = new MenuDO();
        menu.setId(id);
        menu.setName(name);
        menu.setPermission(permission);
        menu.setParentId(parentId);
        menu.setSort(sort);
        menu.setType(MenuTypeEnum.BUTTON.getType());
        return menu;
    }

}
