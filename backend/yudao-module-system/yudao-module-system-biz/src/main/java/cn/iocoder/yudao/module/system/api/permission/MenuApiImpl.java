package cn.iocoder.yudao.module.system.api.permission;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.menu.MenuSaveVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.enums.permission.MenuTypeEnum;
import cn.iocoder.yudao.module.system.service.permission.MenuService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 菜单 API 实现类
 *
 * @author 芋道源码
 */
@Service
public class MenuApiImpl implements MenuApi {

    @Resource
    private MenuService menuService;

    @Override
    public Long getMenuIdByPermission(String permission) {
        List<Long> menuIds = menuService.getMenuIdListByPermissionFromCache(permission);
        return CollUtil.isEmpty(menuIds) ? null : menuIds.get(0);
    }

    @Override
    public Long getMenuIdByComponentName(String componentName) {
        MenuDO menu = menuService.getMenuByComponentName(componentName);
        return menu == null ? null : menu.getId();
    }

    @Override
    public Long ensurePermissionMenu(String name, String permission, Long parentId, Integer sort) {
        Long menuId = getMenuIdByPermission(permission);
        MenuDO menu = menuId == null ? null : menuService.getMenu(menuId);
        // 情况一：还没有菜单行 → 建
        if (menu == null) {
            MenuSaveVO createReqVO = new MenuSaveVO();
            createReqVO.setName(name);
            createReqVO.setPermission(permission);
            createReqVO.setType(MenuTypeEnum.BUTTON.getType());
            createReqVO.setSort(sort != null ? sort : 0);
            createReqVO.setParentId(parentId != null ? parentId : MenuDO.ID_ROOT);
            createReqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
            return menuService.createMenu(createReqVO);
        }
        // 情况三：已经归集过（或由菜单 SQL / 后台人工编排）→ 不动，手工编排优先于代码兜底
        if (!isUnplaced(menu, permission)) {
            return menuId;
        }
        // 情况二：还是「裸标识 + 挂根」的形态 → 就地改名改父，行 id 不变，授权与套餐不受影响
        Long targetParentId = parentId != null ? parentId : MenuDO.ID_ROOT;
        Integer targetSort = sort != null ? sort : 0;
        MenuSaveVO updateReqVO = new MenuSaveVO();
        updateReqVO.setId(menuId);
        updateReqVO.setName(name);
        updateReqVO.setPermission(permission);
        updateReqVO.setType(MenuTypeEnum.BUTTON.getType());
        updateReqVO.setSort(targetSort);
        updateReqVO.setParentId(targetParentId);
        // 只归集名称与位置：状态与可见性等由后台的人决定，同步过程不翻回来
        updateReqVO.setStatus(menu.getStatus());
        updateReqVO.setVisible(menu.getVisible());
        updateReqVO.setKeepAlive(menu.getKeepAlive());
        updateReqVO.setAlwaysShow(menu.getAlwaysShow());
        menuService.updateMenu(updateReqVO);
        return menuId;
    }

    /**
     * 这一行是否还是同步过程最早落下的「未归集」形态：菜单名称就是权限标识、且挂在根节点。
     *
     * <p>用它而不是「无条件按参数收敛」，是为了让同步过程只碰自己造出来的行：{@code icbc-menu.sql}
     * 与后台「菜单管理」编排过的行（有中文名或已有父菜单）一律不动。
     */
    private static boolean isUnplaced(MenuDO menu, String permission) {
        return Objects.equals(menu.getPermission(), permission)
                && Objects.equals(menu.getName(), permission)
                && MenuDO.ID_ROOT.equals(menu.getParentId());
    }

    @Override
    public Set<Long> getAncestorMenuIds(Collection<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return Collections.emptySet();
        }
        Set<Long> ancestors = new LinkedHashSet<>();
        for (Long menuId : menuIds) {
            appendAncestors(menuId, ancestors);
        }
        return ancestors;
    }

    /**
     * 从某个菜单往上走，把沿途的父菜单都收进 {@code ancestors}。
     *
     * <p>{@code ancestors} 既作去重、也兼作“已访问”标记，所以脏数据成环也不会死循环；
     * 再加一层深度上限兜底。
     */
    private void appendAncestors(Long menuId, Set<Long> ancestors) {
        Long parentId = parentIdOf(menuId);
        int depth = 0;
        while (parentId != null && !MenuDO.ID_ROOT.equals(parentId) && depth++ < MAX_MENU_DEPTH) {
            MenuDO parent = menuService.getMenu(parentId);
            // 父菜单不存在（脏数据）就停下，不把一个幽灵 id 加进授权集合
            if (parent == null || !ancestors.add(parent.getId())) {
                break;
            }
            parentId = parent.getParentId();
        }
    }

    private Long parentIdOf(Long menuId) {
        MenuDO menu = menuService.getMenu(menuId);
        return menu == null ? null : menu.getParentId();
    }

    /** 菜单树正常不会这么深；只用于防止脏数据成环死循环。 */
    private static final int MAX_MENU_DEPTH = 64;

}
