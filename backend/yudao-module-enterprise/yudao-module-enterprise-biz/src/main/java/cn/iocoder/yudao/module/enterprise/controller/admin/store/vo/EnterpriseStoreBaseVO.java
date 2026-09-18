package cn.iocoder.yudao.module.enterprise.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 企业门店 Base VO，提供给添加、修改、详细的子 VO 使用
 *
 * @author 芋道源码
 */
@Data
public class EnterpriseStoreBaseVO {

    @Schema(description = "企业ID", required = true, example = "1024")
    @NotNull(message = "企业ID不能为空")
    private Long enterpriseId;

    @Schema(description = "上级门店ID", example = "0")
    private Long parentId;

    @Schema(description = "门店名称", required = true, example = "总店")
    @NotBlank(message = "门店名称不能为空")
    @Size(max = 100, message = "门店名称长度不能超过 100 个字符")
    private String name;

    @Schema(description = "门店编码", example = "S001")
    @Size(max = 50, message = "门店编码长度不能超过 50 个字符")
    private String storeCode;

    @Schema(description = "门店地址-省编码", example = "110000")
    private String addressProvinceCode;

    @Schema(description = "门店地址-市编码", example = "110100")
    private String addressCityCode;

    @Schema(description = "门店地址-区编码", example = "110105")
    private String addressDistrictCode;

    @Schema(description = "门店地址-详细地址", example = "北京市朝阳区XX路XX号")
    private String addressDetail;

    @Schema(description = "门店联系人姓名", example = "张三")
    private String contactName;

    @Schema(description = "门店联系人电话", example = "13800138000")
    private String contactPhone;

    @Schema(description = "门店状态", example = "0")
    private Integer status;
} 