package cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * 企业资质 Base VO，提供给添加、修改、详细的子 VO 使用
 *
 * @author 芋道源码
 */
@Data
public class EnterpriseQualificationBaseVO {

    @Schema(description = "企业ID", required = true, example = "1024")
    @NotNull(message = "企业ID不能为空")
    private Long enterpriseId;

    @Schema(description = "资质类型", required = true, example = "1")
    @NotNull(message = "资质类型不能为空")
    private Integer qualificationType;

    @Schema(description = "资质名称", required = true, example = "危废经营许可证")
    @NotBlank(message = "资质名称不能为空")
    @Size(max = 100, message = "资质名称长度不能超过 100 个字符")
    private String qualificationName;

    @Schema(description = "资质编号", example = "HW12345678")
    @Size(max = 50, message = "资质编号长度不能超过 50 个字符")
    private String qualificationCode;

    @Schema(description = "发证日期", example = "2020-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate issueDate;

    @Schema(description = "到期日期", example = "2025-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expiryDate;

    @Schema(description = "发证机关", example = "北京市生态环境局")
    @Size(max = 100, message = "发证机关长度不能超过 100 个字符")
    private String issuingAuthority;

    @Schema(description = "资质文件附件ID", required = true, example = "1024")
    @NotNull(message = "资质文件附件ID不能为空")
    private Long qualificationFileId;

} 