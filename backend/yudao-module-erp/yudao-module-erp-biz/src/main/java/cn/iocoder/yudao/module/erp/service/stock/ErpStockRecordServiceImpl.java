package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record.ErpStockRecordPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * ERP 品类库存明细 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpStockRecordServiceImpl implements ErpStockRecordService {

    @Resource
    private ErpStockRecordMapper stockRecordMapper;

    @Resource
    private ErpStockService stockService;

    @Override
    public ErpStockRecordDO getStockRecord(Long id) {
        return stockRecordMapper.selectById(id);
    }

    @Override
    public PageResult<ErpStockRecordDO> getStockRecordPage(ErpStockRecordPageReqVO pageReqVO) {
        return stockRecordMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createStockRecord(ErpStockRecordCreateReqBO createReqBO) {
        // 1. 更新库存（按品类 + 仓库 + 库位 + 批次四个维度）
        BigDecimal totalCount = stockService.updateStockCountIncrement(
                createReqBO.getGoodsConfigId(), createReqBO.getWarehouseId(),
                createReqBO.getLocationId(), createReqBO.getBatchId(), createReqBO.getCount());
        // 2. 创建库存明细
        ErpStockRecordDO stockRecord = BeanUtils.toBean(createReqBO, ErpStockRecordDO.class)
                .setTotalCount(totalCount);
        stockRecordMapper.insert(stockRecord);
    }

    @Override
    public boolean existsStockRecord(Integer bizType, Long bizId, Long bizItemId) {
        return stockRecordMapper.selectCountByBizTypeAndBizIdAndBizItemId(bizType, bizId, bizItemId) > 0;
    }

    @Override
    public BigDecimal getStockRecordSum(Integer bizType, Long bizId, Long goodsConfigId) {
        return stockRecordMapper.selectListByBizTypeAndBizIdAndGoodsConfigId(bizType, bizId, goodsConfigId).stream()
                .map(ErpStockRecordDO::getCount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}