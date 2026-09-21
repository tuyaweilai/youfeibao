package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 收方入驻受理回执
 *
 * <p>数据接口是同步受理、异步审核：这一次调用只代表工行**收下了申请**，结论要等审核回调或主动查询
 * （见 {@link PayeeOnboardingStatus}）。所以这里没有"成功/失败"的业务结论，只有受理事实。
 */
@Data
@Builder
public class PayeeOnboardingReceipt {

    /**
     * 外部用户编号（自然人主体），原样回显
     */
    private String outUserId;
    /**
     * 申请时间
     */
    private String applyTime;
    /**
     * 审核时间；尚未审核时为空
     */
    private String auditTime;

}
