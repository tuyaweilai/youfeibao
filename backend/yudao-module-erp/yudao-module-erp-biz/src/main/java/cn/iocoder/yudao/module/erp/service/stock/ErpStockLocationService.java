package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.location.ErpStockLocationPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.location.ErpStockLocationSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockLocationDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 库位 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpStockLocationService {

    /**
     * 创建库位
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStockLocation(@Valid ErpStockLocationSaveReqVO createReqVO);

    /**
     * 更新库位
     *
     * @param updateReqVO 更新信息
     */
    void updateStockLocation(@Valid ErpStockLocationSaveReqVO updateReqVO);

    /**
     * 删除库位
     *
     * @param id 编号
     */
    void deleteStockLocation(Long id);

    /**
     * 获得库位
     *
     * @param id 编号
     * @return 库位
     */
    ErpStockLocationDO getStockLocation(Long id);

    /**
     * 获得库位列表
     *
     * @param ids 编号数组
     * @return 库位列表
     */
    List<ErpStockLocationDO> getStockLocationList(Collection<Long> ids);

    /**
     * 获得指定仓库的库位列表
     *
     * @param warehouseId 仓库编号
     * @return 库位列表
     */
    List<ErpStockLocationDO> getStockLocationListByWarehouseId(Long warehouseId);

    /**
     * 获得指定状态的库位列表
     *
     * @param status 状态
     * @return 库位列表
     */
    List<ErpStockLocationDO> getStockLocationListByStatus(Integer status);

    /**
     * 获得库位分页
     *
     * @param pageReqVO 分页查询
     * @return 库位分页
     */
    PageResult<ErpStockLocationDO> getStockLocationPage(ErpStockLocationPageReqVO pageReqVO);

    /**
     * 校验库位列表的有效性（存在且启用）
     *
     * @param ids 编号数组
     * @return 库位列表
     */
    List<ErpStockLocationDO> validStockLocationList(Collection<Long> ids);

}
