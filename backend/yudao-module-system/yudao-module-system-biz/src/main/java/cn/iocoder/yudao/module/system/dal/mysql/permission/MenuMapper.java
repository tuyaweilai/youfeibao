package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.menu.MenuListReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapperX<MenuDO> {

    default MenuDO selectByParentIdAndName(Long parentId, String name) {
        return selectOne(MenuDO::getParentId, parentId, MenuDO::getName, name);
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(MenuDO::getParentId, parentId);
    }

    default List<MenuDO> selectList(MenuListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<MenuDO>()
                .likeIfPresent(MenuDO::getName, reqVO.getName())
                .eqIfPresent(MenuDO::getStatus, reqVO.getStatus()));
    }

    default List<MenuDO> selectListByPermission(String permission) {
        return selectList(MenuDO::getPermission, permission);
    }

    default MenuDO selectByComponentName(String componentName) {
        return selectOne(MenuDO::getComponentName, componentName);
    }

    /**
     * 按组件名查菜单（按 id 升序）。
     *
     * <p>不用 {@link #selectByComponentName}：组件名只在走 {@code MenuService} 的校验时全局唯一，
     * 菜单 SQL 直接插库可以绕过校验。实测 {@code ErpStock} / {@code ErpSupplier} 各两行
     *（已停用的 ERP 那一棵与 icbc 页面各一行），用 {@code selectOne} 会直接抛 TooManyResults。
     */
    default List<MenuDO> selectListByComponentName(String componentName) {
        return selectList(new LambdaQueryWrapperX<MenuDO>()
                .eq(MenuDO::getComponentName, componentName)
                .orderByAsc(MenuDO::getId));
    }

}
