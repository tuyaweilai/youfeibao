package cn.iocoder.yudao.module.icbc.controller.admin.quota.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 出售者额度超限引导 Response VO")
@Data
public class SellerQuotaGuidanceRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "出售者档案编号", example = "2048")
    private Long payeeId;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "脱敏身份证号", example = "110101********1234")
    private String idCardMasked;

    @Schema(description = "触发场景：INVOICE_APPLICATION-开票申请被拒，ACQUISITION-收购登记时已超")
    private String triggerScene;

    @Schema(description = "触发场景名称", example = "开票申请被拒")
    private String triggerSceneName;

    @Schema(description = "触发业务单号（收购单号 / 合作方订单号）")
    private String triggerBizNo;

    @Schema(description = "触发时已用额度（元）")
    private BigDecimal usedAmount;

    @Schema(description = "触发时窗口上限（元）")
    private BigDecimal capAmount;

    @Schema(description = "引导状态：0-待引导，1-已引导，2-已办结")
    private Integer status;

    @Schema(description = "引导状态名称", example = "待引导")
    private String statusName;

    @Schema(description = "下一步动作")
    private String nextAction;

    @Schema(description = "首次触发时间")
    private LocalDateTime triggeredAt;

    @Schema(description = "最近触发时间")
    private LocalDateTime lastTriggeredAt;

    @Schema(description = "处理时间")
    private LocalDateTime handledAt;

    @Schema(description = "处理说明")
    private String handleRemark;

    @Schema(description = "备注")
    private String remark;

}
