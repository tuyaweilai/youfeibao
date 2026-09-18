package cn.iocoder.yudao.module.icbc.controller.admin.payer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工行付方信息 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PayerInfoRespVO extends PayerInfoSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "审核消息", example = "审核通过")
    private String auditMsg;

    @Schema(description = "工行付方状态：0-不可用，1-可用", example = "1")
    private String icbcPayerStatus;

    @Schema(description = "工行介质标识（mediumId）", example = "MEDIUM123456")
    private String icbcMediumId;

    @Schema(description = "工行侧开户状态（openacctStatus）：00-初始，01-开户中，02-开户成功，03-开户失败", example = "02")
    private String icbcOpenacctStatus;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

} 