package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 收方入驻结果查询
 *
 * <p>对应工行 `/api/jft/api/user/edpopenacct/query/V1`。收方入驻只绑**一张卡**、只有**一条审核线**
 * （ADR 0035）：没有电子钱包，所以这里不带开户状态与电子账户账号。
 */
@Data
@Builder
public class PayeeOnboardingStatus {

    private String outUserId;
    /**
     * 收方状态：0-不可用，1-可用
     */
    private String receiverStatus;
    /**
     * 审核状态：1-审核通过，2-新增审核中，3-修改审核中，4-删除审核中
     */
    private String auditStatus;
    /**
     * 冻结状态：0-已冻结，1-未冻结，2-解冻审核中
     */
    private String freezeStatus;
    /**
     * 收方审核结果（result，原样透传）：pass-审核通过，reject-审核拒绝
     */
    private String result;
    /**
     * 审核拒绝原因
     */
    private String rejectReason;
    /**
     * 明细信息
     */
    private String custStatusDetail;

}
