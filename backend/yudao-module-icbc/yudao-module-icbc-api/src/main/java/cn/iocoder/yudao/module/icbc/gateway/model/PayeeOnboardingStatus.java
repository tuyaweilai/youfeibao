package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 收方入驻结果查询
 *
 * 对应工行 `/api/jft/api/user/edpopenacct/query/V1`。
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
     * 工行侧开户状态（openacctStatus，原样透传）
     */
    private String openacctStatus;
    /**
     * 工行返回的账户标识（mediumId，原样透传）
     */
    private String mediumId;
    /**
     * 明细信息
     */
    private String custStatusDetail;

}
