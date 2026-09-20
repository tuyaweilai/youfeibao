package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;

import java.math.BigDecimal;

/**
 * ERP 品类库存 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpStockService {

    /**
     * 获得品类库存
     *
     * @param id 编号
     * @return 库存
     */
    ErpStockDO getStock(Long id);

    /**
     * 基于品类 + 仓库，获得品类库存
     *
     * @param goodsConfigId 品类编号
     * @param warehouseId 仓库编号
     * @return 品类库存
     */
    ErpStockDO getStock(Long goodsConfigId, Long warehouseId);

    /**
     * 获得品类库存数量
     *
     * 如果不存在库存记录，则返回 0
     *
     * @param goodsConfigId 品类编号
     * @return 品类库存数量
     */
    BigDecimal getStockCount(Long goodsConfigId);

    /**
     * 基于品类 + 仓库，获得库存数量。不存在时返回 0。
     *
     * @param goodsConfigId 品类编号
     * @param warehouseId   仓库编号
     * @return 库存数量
     */
    BigDecimal getStockCount(Long goodsConfigId, Long warehouseId);

    /**
     * 获得品类库存分页
     *
     * @param pageReqVO 分页查询
     * @return 库存分页
     */
    PageResult<ErpStockDO> getStockPage(ErpStockPageReqVO pageReqVO);

    /**
     * 增量更新品类库存数量
     *
     * @param goodsConfigId 品类编号
     * @param warehouseId 仓库编号
     * @param count 增量数量：正数，表示增加；负数，表示减少
     * @return 更新后的库存
     */
    BigDecimal updateStockCountIncrement(Long goodsConfigId, Long warehouseId, BigDecimal count);

}