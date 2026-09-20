package cn.iocoder.yudao.module.icbc.service.handover;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverBatchUpdateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingAddReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingEffectiveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.handover.vo.HandoverWeighingRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.handover.IcbcHandoverBatchDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.handover.IcbcWeighingDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 交接批次与有效磅次 Service（#50 T12）。
 *
 * <p>一个交易对方的一次**物理交接**记为一个交接批次；过磅保留每一次原始读数（磅次），
 * **只有被选定的那一次参与计量**，其余留档不参与（见 CONTEXT「交接批次」「有效磅次」）。
 *
 * <p>三条边界（守住就不会退回旧口径）：
 * <ul>
 *   <li>**同一车同一天两次送货就是两个批次**：不做车牌 + 日期的去重，磅单与收购单各归各；</li>
 *   <li>**没有预约、没有采购订单也能建批次**：两者只是可选的关联；</li>
 *   <li>**计量引用有效磅次的值与版本**：批次一旦产生收购单，就不能再改有效磅次。</li>
 * </ul>
 */
public interface HandoverBatchService {

    // ==================== 批次 ====================

    /**
     * 登记一个交接批次（交易对方、场站或上门地址、时间、来源方式、司机与车牌）。
     */
    Long createBatch(@Valid HandoverBatchCreateReqVO reqVO);

    /**
     * 补录现场当时没填全的几项；交易对方不在这里改。
     */
    void updateBatch(@Valid HandoverBatchUpdateReqVO reqVO);

    /**
     * 批次详情：含全部磅次（含留档不参与的那些）与有效磅次是哪一次。
     */
    HandoverBatchRespVO getBatch(Long id);

    PageResult<HandoverBatchRespVO> getPage(@Valid HandoverBatchPageReqVO reqVO);

    // ==================== 磅次 ====================

    /**
     * 新增一次磅次。第一次磅次自动成为有效磅次，之后要改由 {@link #selectEffectiveWeighing} 指定。
     */
    Long addWeighing(@Valid HandoverWeighingAddReqVO reqVO);

    /**
     * 指定哪一次磅次有效；同一批次其余磅次自动转为留档不参与。
     */
    void selectEffectiveWeighing(@Valid HandoverWeighingEffectiveReqVO reqVO);

    List<HandoverWeighingRespVO> listWeighings(Long batchId);

    // ==================== 供收购登记使用 ====================

    /**
     * 按编号取批次（带租户条件）；不存在就报错。
     */
    IcbcHandoverBatchDO getBatchDO(Long id);

    /**
     * 该批次的有效磅次；没有指定过则返回 null——调用方**必须拦住计量**，不能猜。
     */
    IcbcWeighingDO getEffectiveWeighing(Long batchId);

    /**
     * 本批次已产生的收购单张数（>0 表示有效磅次已锁定）。
     */
    Long countAcquisitions(Long batchId);

}
