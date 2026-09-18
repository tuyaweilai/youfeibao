package cn.iocoder.yudao.module.contract.controller.admin.version.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 合同版本 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractVersionRespVO extends ContractVersionBaseVO {

    @Schema(description = "版本ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long contractId;

    @Schema(description = "版本号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1.0")
    private String versionNumber;

    @Schema(description = "生效日期", example = "2023-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2023-12-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expiryDate;

    @Schema(description = "签署完成日期", example = "2023-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate signingDate;

    @Schema(description = "终止日期", example = "2023-06-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate terminationDate;

    @Schema(description = "终止原因", example = "合同到期")
    private String reasonForTermination;

    @Schema(description = "版本变更说明", example = "初始版本")
    private String descriptionOfChanges;

    @Schema(description = "合同内容", example = "合同内容...")
    private String contractContent;

    @Schema(description = "合同文件URL", example = "https://example.com/contract.pdf")
    private String contractFileUrl;

    @Schema(description = "电子签章服务商", example = "esign")
    private String esignatureProvider;

    @Schema(description = "第三方签署流程ID", example = "PROCESS_123")
    private String esignatureProcessId;

    @Schema(description = "电子签章状态(0:未发起,1:进行中,2:已完成,3:已失败)", example = "0")
    private Integer esignatureStatus;

    @Schema(description = "自动提醒天数", example = "30,15,7")
    private String autoRemindDays;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

    @Schema(description = "创建者", example = "1")
    private String creator;

    @Schema(description = "更新者", example = "1")
    private String updater;
} 