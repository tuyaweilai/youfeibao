package cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Schema(description = "管理后台 - 发起工行企业授权 Request VO")
@Data
public class IcbcEnterpriseAuthInitReqVO {

    @Schema(description = "付方编号（回收企业 / 子商户）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "付方编号不能为空")
    private String outVendorId;

    @Schema(description = "工行入参 siteType")
    private String siteType;

    @Schema(description = "工行入参 userType")
    private String userType;

}
