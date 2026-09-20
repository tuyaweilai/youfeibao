package cn.iocoder.yudao.module.waste.controller.admin.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 付款状态变更历史 Response VO")
@Data
public class PaymentStatusHistoryRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long orderId;

    @Schema(description = "付款记录ID", example = "2048")
    private Long paymentRecordId;

    @Schema(description = "原状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer fromStatus;

    @Schema(description = "新状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer toStatus;

    @Schema(description = "变更原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "用户确认付款")
    private String changeReason;

    @Schema(description = "操作人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long operatorId;

    @Schema(description = "操作人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String operatorName;

    @Schema(description = "操作人类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer operatorType;

    @Schema(description = "变更时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-12-01 10:00:00")
    private LocalDateTime changeTime;

    @Schema(description = "是否系统自动变更", example = "false")
    private Boolean isSystemChange;

    @Schema(description = "关联业务数据", example = "{\"paymentAmount\":1000.00}")
    private String businessData;

    @Schema(description = "备注", example = "状态变更备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

} 