package cn.iocoder.yudao.module.waste.controller.admin.quotation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 预约报价记录 Response VO")
@Data
public class AppointmentQuotationRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "预约单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long appointmentId;

    @Schema(description = "回收企业ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long recyclingEnterpriseId;

    @Schema(description = "报价金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    private BigDecimal quotedPrice;

    @Schema(description = "报价备注", example = "报价说明")
    private String quotationRemark;

    @Schema(description = "有效期至", example = "2023-12-31 23:59:59")
    private LocalDateTime validUntil;

    @Schema(description = "报价时间", example = "2023-12-01 10:00:00")
    private LocalDateTime quotationTime;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "接受人", example = "张三")
    private String acceptedBy;

    @Schema(description = "接受时间", example = "2023-12-01 11:00:00")
    private LocalDateTime acceptTime;

    @Schema(description = "接受原因", example = "价格合理")
    private String acceptReason;

    @Schema(description = "拒绝人", example = "李四")
    private String rejectedBy;

    @Schema(description = "拒绝时间", example = "2023-12-01 12:00:00")
    private LocalDateTime rejectTime;

    @Schema(description = "拒绝原因", example = "价格过高")
    private String rejectReason;

    @Schema(description = "撤回人", example = "王五")
    private String withdrawnBy;

    @Schema(description = "撤回时间", example = "2023-12-01 13:00:00")
    private LocalDateTime withdrawTime;

    @Schema(description = "撤回原因", example = "报价有误")
    private String withdrawReason;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

} 