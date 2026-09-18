package cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 企业信息 Base VO，提供给添加、修改、详细的子 VO 使用
 *
 * @author 芋道源码
 */
@Data
public class EnterpriseInfoBaseVO {

    @Schema(description = "企业名称", required = true, example = "芋道源码")
    @NotBlank(message = "企业名称不能为空")
    @Size(max = 100, message = "企业名称长度不能超过 100 个字符")
    private String name;

    @Schema(description = "统一社会信用代码", required = true, example = "91110105MA01RUYX8Q")
    @NotBlank(message = "统一社会信用代码不能为空")
    @Size(min = 18, max = 18, message = "统一社会信用代码长度必须为 18 个字符")
    private String creditCode;

    @Schema(description = "企业类型", required = true, example = "1")
    @NotNull(message = "企业类型不能为空")
    private Integer enterpriseType;

    @Schema(description = "法定代表人姓名", required = true, example = "芋道")
    @NotBlank(message = "法定代表人姓名不能为空")
    @Size(max = 64, message = "法定代表人姓名长度不能超过 64 个字符")
    private String legalPersonName;

    @Schema(description = "法定代表人身份证号", example = "110101199001011234")
    @Size(min = 18, max = 18, message = "法定代表人身份证号长度必须为 18 个字符")
    private String legalPersonIdCardNo;

    @Schema(description = "注册资本(万元)", example = "1000")
    private BigDecimal registeredCapital;

    @Schema(description = "成立日期", example = "2020-01-01")
    private LocalDate establishmentDate;

    @Schema(description = "经营范围", example = "技术开发、技术咨询、技术服务")
    private String businessScope;

    @Schema(description = "注册地址-省编码", example = "110000")
    private String registeredAddressProvinceCode;

    @Schema(description = "注册地址-市编码", example = "110100")
    private String registeredAddressCityCode;

    @Schema(description = "注册地址-区编码", example = "110105")
    private String registeredAddressDistrictCode;

    @Schema(description = "注册地址-详细地址", example = "北京市朝阳区XX路XX号")
    private String registeredAddressDetail;

    @Schema(description = "企业联系人姓名", example = "小王")
    private String contactName;

    @Schema(description = "企业联系人电话", example = "13800138000")
    private String contactPhone;

    @Schema(description = "营业执照附件", example = "https://example.com/licenses/xxx.jpg")
    private String businessLicenseFile;
} 