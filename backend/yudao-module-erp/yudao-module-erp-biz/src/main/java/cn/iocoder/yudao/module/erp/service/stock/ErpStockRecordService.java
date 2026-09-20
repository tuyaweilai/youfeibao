package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;

import javax.validation.Valid;
import java.math.BigDecimal;

/**
 * ERP 品类库存明细 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpStockRecordService {

    /**
     * 获得品类库存明细
     *
     * @param id 编号
     * @return 品类库存明细
     */
    ErpStockRecordDO getStockRecord(Long id);

    /**
     * 获得品类库存明细分页
     *
     * @param pageReqVO 分页查询
     * @return 品类库存明细分页
     */
    PageResult<ErpStockRecordDO> getStockRecordPage(ErpStockRecordPageReqVO pageReqVO);

    /**
     * 创建库存明细
     *
     * @param createReqBO 创建库存明细 BO
     */
    void createStockRecord(@Valid ErpStockRecordCreateReqBO createReqBO);

    /**
     * 判断某业务项是否已经写过库存流水（用于入库 / 出库幂等）。
     *
     * @param bizType   业务类型
     * @param bizId     业务编号
     * @param bizItemId 业务项编号
     * @return 是否存在
     */
    boolean existsStockRecord(Integer bizType, Long bizId, Long bizItemId);

    /**
     * 累计某业务在某品类上的入库流水之和，用于校验「累计入库不得超过可入库量」。
     *
     * @param bizType       业务类型
     * @param bizId         业务编号
     * @param goodsConfigId 品类编号
     * @return 流水数量之和
     */
    BigDecimal getStockRecordSum(Integer bizType, Long bizId, Long goodsConfigId);

}