package cn.iocoder.yudao.module.waste.controller.admin.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 付款状态变更历史创建 Request VO")
@Data
public class PaymentStatusHistoryCreateReqVO {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "付款记录ID", example = "2048")
    private Long paymentRecordId;

    @Schema(description = "原状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "原状态不能为空")
    private Integer fromStatus;

    @Schema(description = "新状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "新状态不能为空")
    private Integer toStatus;

    @Schema(description = "变更原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "用户确认付款")
    @NotBlank(message = "变更原因不能为空")
    @Size(max = 200, message = "变更原因长度不能超过200个字符")
    private String changeReason;

    @Schema(description = "操作人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    @Schema(description = "操作人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "操作人姓名不能为空")
    @Size(max = 50, message = "操作人姓名长度不能超过50个字符")
    private String operatorName;

    @Schema(description = "操作人类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "操作人类型不能为空")
    private Integer operatorType;

    @Schema(description = "变更时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2023-12-01 10:00:00")
    @NotNull(message = "变更时间不能为空")
    private LocalDateTime changeTime;

    @Schema(description = "是否系统自动变更", example = "false")
    private Boolean isSystemChange;

    @Schema(description = "关联业务数据", example = "{\"paymentAmount\":1000.00}")
    @Size(max = 2000, message = "关联业务数据长度不能超过2000个字符")
    private String businessData;

    @Schema(description = "备注", example = "状态变更备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

} 