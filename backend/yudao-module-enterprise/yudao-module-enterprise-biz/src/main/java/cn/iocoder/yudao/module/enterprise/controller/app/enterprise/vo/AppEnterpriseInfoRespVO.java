package cn.iocoder.yudao.module.enterprise.controller.app.enterprise.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "APP - 企业信息 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppEnterpriseInfoRespVO extends AppEnterpriseInfoSimpleRespVO {

    @Schema(description = "企业类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer enterpriseType;

    @Schema(description = "企业类型名称", example = "产废企业")
    private String enterpriseTypeName;

    @Schema(description = "企业状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;
    
    @Schema(description = "企业状态名称", example = "入驻待审核")
    private String statusName;

    @Schema(description = "法定代表人姓名", example = "张三")
    private String legalPersonName;

    @Schema(description = "注册资本(万元)", example = "1000")
    private BigDecimal registeredCapital;

    @Schema(description = "成立日期", example = "2022-01-01")
    private LocalDate establishmentDate;

    @Schema(description = "经营范围", example = "互联网软件开发")
    private String businessScope;

    @Schema(description = "注册地址-省编码", example = "110000")
    private String registeredAddressProvinceCode;

    @Schema(description = "注册地址-市编码", example = "110100")
    private String registeredAddressCityCode;

    @Schema(description = "注册地址-区编码", example = "110101")
    private String registeredAddressDistrictCode;

    @Schema(description = "注册地址-详细地址", example = "朝阳区XX大厦")
    private String registeredAddressDetail;

    @Schema(description = "企业联系人姓名", example = "李四")
    private String contactName;

    @Schema(description = "企业联系人电话", example = "13866668888")
    private String contactPhone;

    @Schema(description = "营业执照附件", example = "https://www.iocoder.cn/xxx.jpg")
    private String businessLicenseFile;

    @Schema(description = "最新审核备注", example = "资料不完整，请补充")
    private String auditRemarks;
    
    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
} 