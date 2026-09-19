package cn.iocoder.yudao.module.icbc.controller.admin.payee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 换银行卡（收款账户变更）记录 Response VO（#37）。
 *
 * <p>给企业侧看的：谁在换卡、换到哪张（只给尾号）、审核到哪一步、原卡是否还在用。
 */
@Schema(description = "管理后台 - 收款账户变更记录")
@Data
public class PayeeBankCardChangeRespVO {

    @Schema(description = "变更编号", example = "1024")
    private Long id;

    @Schema(description = "变更单号", example = "BC20260101120000ABCD")
    private String changeNo;

    @Schema(description = "收方（出售者）档案编号", example = "1024")
    private Long payeeId;

    @Schema(description = "自然人主体编号", example = "2048")
    private Long naturalPersonId;

    @Schema(description = "状态：0-银行审核中，1-已生效，2-已拒绝，9-已取消", example = "0")
    private Integer status;

    @Schema(description = "状态名", example = "银行审核中")
    private String statusName;

    @Schema(description = "原卡尾号", example = "1234")
    private String oldCardTail;

    @Schema(description = "新卡尾号", example = "5678")
    private String newCardTail;

    @Schema(description = "新卡开户银行", example = "中国工商银行")
    private String newBankName;

    @Schema(description = "工行侧开户状态（原样透传）", example = "02")
    private String icbcOpenacctStatus;

    @Schema(description = "工行审核结果（原样透传）：pass / reject", example = "pass")
    private String auditResult;

    @Schema(description = "拒绝原因 / 取消原因")
    private String rejectReason;

    @Schema(description = "发起来源", example = "SELLER_PORTAL")
    private String requestSource;

    @Schema(description = "发起时间")
    private LocalDateTime requestedAt;

    @Schema(description = "有结果时间")
    private LocalDateTime resolvedAt;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
