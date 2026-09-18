package cn.iocoder.yudao.module.contract.controller.admin.version.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 合同版本创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractVersionCreateReqVO extends ContractVersionBaseVO {

    @Schema(description = "合同ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    @Schema(description = "版本号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1.0")
    @NotEmpty(message = "版本号不能为空")
    @Size(max = 20, message = "版本号长度不能超过 20 个字符")
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

    @Schema(description = "版本变更说明", example = "初始版本")
    private String descriptionOfChanges;

    @Schema(description = "合同内容", example = "合同内容...")
    private String contractContent;

    @Schema(description = "合同文件URL", example = "https://example.com/contract.pdf")
    private String contractFileUrl;

    @Schema(description = "电子签章服务商", example = "esign")
    private String esignatureProvider;

    @Schema(description = "自动提醒天数", example = "30,15,7")
    private String autoRemindDays;
} 