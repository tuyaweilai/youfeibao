package cn.iocoder.yudao.module.icbc.controller.admin.qualification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "管理后台 - 租户资质新增/修改 Request VO")
@Data
public class IcbcQualificationSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "资质层：TAX / INDUSTRY / PUBLIC_SECURITY", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "资质层不能为空")
    private String type;

    @Schema(description = "资质名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "资质名称不能为空")
    private String name;

    @Schema(description = "发证机关")
    private String issuingAuthority;

    @Schema(description = "证书编号")
    private String certNo;

    @Schema(description = "有效期起")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate validFrom;

    @Schema(description = "有效期止")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate validTo;

    @Schema(description = "证照扫描件")
    private String fileUrl;

    @Schema(description = "状态：0-待核实，1-有效，2-失效，3-吊销", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "核实意见")
    private String auditRemark;

}
