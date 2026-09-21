package cn.iocoder.yudao.module.icbc.service.payee;

import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.PayeeBankCardChangeSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.IcbcPayeeBankCardChangeDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 出售者换银行卡（收款账户变更）Service（#37，ADR 0010）。
 *
 * <p>三条硬规则都落在这里，别的调用方不要各写一套判断：
 * <ol>
 *   <li><b>不允许多张卡</b>：收方档案里的 {@code bankCardNo} 是唯一生效中的卡；新卡审核通过才搬过去。</li>
 *   <li><b>审核期间新交易的付款挂起</b>：{@link #assertPaymentNotSuspended} 是付款发起前的唯一门禁。</li>
 *   <li><b>不新造流程</b>：换卡走工行的收方修改接口（数据接口），新卡审核结果通过
 *       {@link #applyOnboardingResult} 收敛回本表与收方档案。</li>
 * </ol>
 */
public interface PayeeBankCardChangeService {

    /**
     * 发起一次换卡：落一条「银行审核中」的变更单。
     *
     * <p>要求收方已完成首次收方入驻（未完成属于「建档」而不是「换卡」），且当前没有在途变更。
     * 新卡只写在变更单上，**收方档案不动**。
     */
    IcbcPayeeBankCardChangeDO requestChange(PayeeBankCardChangeSaveReqVO reqVO);

    /**
     * 取某收方正在「银行审核中」的那条变更；没有则返回 {@code null}。
     */
    IcbcPayeeBankCardChangeDO getPending(Long payeeId);

    /**
     * 某收方是否有在途变更。
     */
    boolean hasPending(Long payeeId);

    /**
     * 批量取在途变更，key = 收方编号。
     */
    Map<Long, IcbcPayeeBankCardChangeDO> pendingMap(Collection<Long> payeeIds);

    /**
     * 付款门禁：该收方正在换卡时拒绝发起付款，并明确说明是「挂起」而不是「失败」。
     */
    void assertPaymentNotSuspended(Long payeeId);

    /**
     * 把收方入驻的结果收敛到在途变更上（无在途变更时返回 {@code null}，不动收方档案）。
     *
     * <p>通过 → 新卡生效（搬到收方档案）并留痕为「已生效」；拒绝 → 变更单记「已拒绝」，
     * <b>原卡继续有效</b>——所以他不会因为一次换卡失败而付不出钱。
     *
     * @param payeeId      收方编号
     * @param result       工行审核结果 pass / reject（原样透传）
     * @param rejectReason 拒绝原因
     * @return 收敛后的变更单；无在途变更返回 {@code null}
     */
    IcbcPayeeBankCardChangeDO applyOnboardingResult(Long payeeId, String result, String rejectReason);

    /**
     * 取消在途变更（企业侧人工清障）：只有「银行审核中」可取消，原卡继续有效。
     */
    IcbcPayeeBankCardChangeDO cancelChange(Long changeId, String reason);

    /**
     * 某收方的变更历史（倒序）。
     */
    List<IcbcPayeeBankCardChangeDO> listByPayeeId(Long payeeId);

}
