package cn.iocoder.yudao.module.system.api.permission;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.menu.MenuSaveVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.enums.permission.MenuTypeEnum;
import cn.iocoder.yudao.module.system.service.permission.MenuService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

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
    public Long createPermissionMenu(String name, String permission) {
        MenuSaveVO createReqVO = new MenuSaveVO();
        createReqVO.setName(name);
        createReqVO.setPermission(permission);
        createReqVO.setType(MenuTypeEnum.BUTTON.getType());
        createReqVO.setSort(0);
        createReqVO.setParentId(MenuDO.ID_ROOT);
        createReqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return menuService.createMenu(createReqVO);
    }

}
