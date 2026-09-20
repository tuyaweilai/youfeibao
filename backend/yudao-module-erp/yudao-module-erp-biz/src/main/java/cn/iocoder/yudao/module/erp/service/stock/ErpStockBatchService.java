package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.batch.ErpStockBatchPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.batch.ErpStockBatchSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockBatchDO;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * ERP 批次 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpStockBatchService {

    /**
     * 创建批次
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStockBatch(@Valid ErpStockBatchSaveReqVO createReqVO);

    /**
     * 更新批次
     *
     * @param updateReqVO 更新信息
     */
    void updateStockBatch(@Valid ErpStockBatchSaveReqVO updateReqVO);

    /**
     * 删除批次
     *
     * @param id 编号
     */
    void deleteStockBatch(Long id);

    /**
     * 获得批次
     *
     * @param id 编号
     * @return 批次
     */
    ErpStockBatchDO getStockBatch(Long id);

    /**
     * 获得批次列表
     *
     * @param ids 编号数组
     * @return 批次列表
     */
    List<ErpStockBatchDO> getStockBatchList(Collection<Long> ids);

    /**
     * 获得指定状态的批次列表
     *
     * @param status 状态
     * @return 批次列表
     */
    List<ErpStockBatchDO> getStockBatchListByStatus(Integer status);

    /**
     * 获得批次分页
     *
     * @param pageReqVO 分页查询
     * @return 批次分页
     */
    PageResult<ErpStockBatchDO> getStockBatchPage(ErpStockBatchPageReqVO pageReqVO);

    /**
     * 按批次号获得批次；不存在时创建（收货入库自动生成）。
     *
     * <p>同一批次号只会有一条记录，重复调用返回同一个编号，供入库按批次号幂等归集。
     *
     * @param batchNo       批次号
     * @param goodsConfigId 品类编号（可空）
     * @param inTime        入库时间（可空）
     * @return 批次编号
     */
    Long getOrCreateStockBatch(String batchNo, Long goodsConfigId, LocalDateTime inTime);

}
