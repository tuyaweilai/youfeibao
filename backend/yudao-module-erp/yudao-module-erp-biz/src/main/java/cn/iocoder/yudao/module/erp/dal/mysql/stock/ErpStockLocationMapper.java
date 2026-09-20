package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.location.ErpStockLocationPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockLocationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 库位 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockLocationMapper extends BaseMapperX<ErpStockLocationDO> {

    default PageResult<ErpStockLocationDO> selectPage(ErpStockLocationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpStockLocationDO>()
                .eqIfPresent(ErpStockLocationDO::getWarehouseId, reqVO.getWarehouseId())
                .likeIfPresent(ErpStockLocationDO::getName, reqVO.getName())
                .eqIfPresent(ErpStockLocationDO::getStatus, reqVO.getStatus())
                .orderByAsc(ErpStockLocationDO::getSort)
                .orderByDesc(ErpStockLocationDO::getId));
    }

    default ErpStockLocationDO selectByWarehouseIdAndName(Long warehouseId, String name) {
        return selectOne(ErpStockLocationDO::getWarehouseId, warehouseId,
                ErpStockLocationDO::getName, name);
    }

    default List<ErpStockLocationDO> selectListByWarehouseId(Long warehouseId) {
        return selectList(ErpStockLocationDO::getWarehouseId, warehouseId);
    }

    default List<ErpStockLocationDO> selectListByStatus(Integer status) {
        return selectList(ErpStockLocationDO::getStatus, status);
    }

}
