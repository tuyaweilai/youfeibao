package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.batch.ErpStockBatchPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.batch.ErpStockBatchSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockBatchDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockBatchMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 批次 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpStockBatchServiceImpl implements ErpStockBatchService {

    @Resource
    private ErpStockBatchMapper batchMapper;
    @Resource
    private ErpStockMapper stockMapper;

    @Override
    public Long createStockBatch(ErpStockBatchSaveReqVO createReqVO) {
        validateBatchNoUnique(null, createReqVO.getBatchNo());
        ErpStockBatchDO batch = BeanUtils.toBean(createReqVO, ErpStockBatchDO.class);
        batchMapper.insert(batch);
        return batch.getId();
    }

    @Override
    public void updateStockBatch(ErpStockBatchSaveReqVO updateReqVO) {
        validateStockBatchExists(updateReqVO.getId());
        validateBatchNoUnique(updateReqVO.getId(), updateReqVO.getBatchNo());
        batchMapper.updateById(BeanUtils.toBean(updateReqVO, ErpStockBatchDO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStockBatch(Long id) {
        ErpStockBatchDO batch = validateStockBatchExists(id);
        if (stockMapper.selectCountByBatchId(id) > 0) {
            throw exception(STOCK_BATCH_HAS_STOCK, batch.getBatchNo());
        }
        batchMapper.deleteById(id);
    }

    private void validateBatchNoUnique(Long id, String batchNo) {
        ErpStockBatchDO batch = batchMapper.selectByBatchNo(batchNo);
        if (batch == null) {
            return;
        }
        if (id == null || !batch.getId().equals(id)) {
            throw exception(STOCK_BATCH_NO_DUPLICATE, batchNo);
        }
    }

    private ErpStockBatchDO validateStockBatchExists(Long id) {
        ErpStockBatchDO batch = batchMapper.selectById(id);
        if (batch == null) {
            throw exception(STOCK_BATCH_NOT_EXISTS);
        }
        return batch;
    }

    @Override
    public ErpStockBatchDO getStockBatch(Long id) {
        return batchMapper.selectById(id);
    }

    @Override
    public List<ErpStockBatchDO> getStockBatchList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return batchMapper.selectBatchIds(ids);
    }

    @Override
    public List<ErpStockBatchDO> getStockBatchListByStatus(Integer status) {
        return batchMapper.selectListByStatus(status);
    }

    @Override
    public PageResult<ErpStockBatchDO> getStockBatchPage(ErpStockBatchPageReqVO pageReqVO) {
        return batchMapper.selectPage(pageReqVO);
    }

    @Override
    public Long getOrCreateStockBatch(String batchNo, Long goodsConfigId, LocalDateTime inTime) {
        // 1. 已有则复用：同一批次号只有一条，保证入库按批次号幂等
        ErpStockBatchDO existing = batchMapper.selectByBatchNo(batchNo);
        if (existing != null) {
            return existing.getId();
        }
        // 2. 没有则按最小信息自动生成
        ErpStockBatchDO batch = ErpStockBatchDO.builder()
                .batchNo(batchNo).goodsConfigId(goodsConfigId).inTime(inTime)
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        try {
            batchMapper.insert(batch);
            return batch.getId();
        } catch (DuplicateKeyException ex) {
            // 并发下另一个请求已经建过：唯一约束兜底，回读既有批次
            return batchMapper.selectByBatchNo(batchNo).getId();
        }
    }

}
