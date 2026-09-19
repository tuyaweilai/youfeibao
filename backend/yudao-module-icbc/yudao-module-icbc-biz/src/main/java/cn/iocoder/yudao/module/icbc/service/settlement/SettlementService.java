package cn.iocoder.yudao.module.icbc.service.settlement;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo.*;
import cn.iocoder.yudao.module.icbc.controller.app.settlement.vo.SellerSettlementConfirmReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.settlement.vo.SellerSettlementDisputeReqVO;

import javax.validation.Valid;
import java.util.List;

/**
 * 结算单 Service（ADR 0018 / 0022 / 0024）。
 *
 * <p>在收购与开票之间补上「出售者的认可」：一次到场批次一张结算单，确认是预下单的硬前置。
 */
public interface SettlementService {

    // ==================== 企业侧 ====================

    /**
     * 收货员显式「结束本次收货」生成结算单：聚合同一出售者尚未归组的收购单。
     * 生成后不得再往里加收购单，要加只能新建。
     *
     * @param reqVO 生成参数（出售者 + 可选的离线批次键）
     * @return 结算单编号
     */
    Long generate(@Valid SettlementGenerateReqVO reqVO);

    PageResult<SettlementRespVO> getPage(SettlementPageReqVO reqVO);

    SettlementRespVO getDetail(Long id);

    List<SettlementRespVO> getListByPayeeId(Long payeeId);

    /**
     * 企业对异议的动作之一：改（新版本 + 原因 → 回到待确认）。
     */
    void changeByEnterprise(@Valid SettlementChangeReqVO reqVO);

    /**
     * 企业对异议的动作之二：不改但附说明（→ 回到待确认）。
     */
    void replyNoChange(@Valid SettlementReplyReqVO reqVO);

    /**
     * 线下签字逃生门：上传带签字的纸质确认书 + 办理人，等价于确认。
     */
    void offlineSign(@Valid SettlementOfflineSignReqVO reqVO);

    /**
     * 作废未开票的收购单：留原因、对自然人可见、作废后不可再开票付款。
     */
    void cancelAcquisition(@Valid SettlementAcquisitionCancelReqVO reqVO);

    // ==================== 自然人侧 ====================

    /**
     * 某一自然人主体名下的全部结算单（待我确认 / 历史）。
     */
    List<SettlementRespVO> getListForSeller(Long naturalPersonId);

    /**
     * 自然人本人查看某张结算单的明细（校验归属，不做静默推断）。
     */
    SettlementRespVO getDetailForSeller(Long naturalPersonId, Long id);

    /**
     * 确认结算：留痕时间 / IP / 设备 / 该版快照哈希（ADR 0024）。
     */
    void confirm(@Valid SellerSettlementConfirmReqVO reqVO, String ip, String device);

    /**
     * 提出异议：固定原因枚举 + 说明（ADR 0022）。
     */
    void raiseDispute(@Valid SellerSettlementDisputeReqVO reqVO);

    // ==================== 门禁与定时 ====================

    /**
     * 该收购单所属结算单是否已确认（含线下签字确认）。
     */
    boolean isSettlementConfirmed(Long acquisitionId);

    /**
     * 开票门禁：未确认抛 {@code SETTLEMENT_NOT_CONFIRMED}，不下发工行预下单。
     */
    void assertSettlementConfirmed(Long acquisitionId);

    /**
     * 超时处理（定时任务）：确认超期 → 升级为「需线下签字确认」，**不自动确认**。
     *
     * @return 本次升级的结算单数
     */
    int handleTimeout();

}
