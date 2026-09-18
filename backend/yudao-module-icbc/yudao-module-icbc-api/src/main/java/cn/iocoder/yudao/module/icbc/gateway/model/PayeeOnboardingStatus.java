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
     * 开户状态
     */
    private String openacctStatus;
    /**
     * 电子钱包介质号
     */
    private String mediumId;
    /**
     * 子钱包编号
     */
    private String subWalletId;
    /**
     * 子钱包状态
     */
    private String subWalletStatus;
    /**
     * 子钱包名称
     */
    private String subWalletName;
    /**
     * 明细信息
     */
    private String custStatusDetail;

}
