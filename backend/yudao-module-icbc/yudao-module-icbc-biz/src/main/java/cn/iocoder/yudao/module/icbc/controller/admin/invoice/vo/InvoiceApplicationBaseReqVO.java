package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 开票申请公共入参。
 *
 * <p>开票员只选「哪些收购单要开票」和「开专票还是普票」，其余字段（出售者、回收企业、
 * 商品明细、税率、税收分类编码）全部从已登记的收购单与档案里推导，不需要手打工行报文。
 */
@Schema(description = "管理后台 - 开票申请公共入参")
@Data
public class InvoiceApplicationBaseReqVO {

    @Schema(description = "发票类型：01-增值税专用发票，02-普通发票", example = "02")
    @Pattern(regexp = "^(01|02)$", message = "发票类型只能是01或02")
    private String invoiceType = "02";

    @Schema(description = "应税行为发生地（省级税务机关代码，如 110000）", requiredMode = Schema.RequiredMode.REQUIRED, example = "110000")
    @NotEmpty(message = "应税行为发生地不能为空")
    @Size(max = 11, message = "应税行为发生地长度不能超过11个字符")
    private String areaCode;

    @Schema(description = "开票人姓名（须与工行税务登记的开票员为同一实名主体）", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotEmpty(message = "开票人不能为空")
    @Size(max = 200, message = "开票人长度不能超过200个字符")
    private String drawerName;

    @Schema(description = "开票人证件类型：111-身份证", example = "111")
    @Pattern(regexp = "^(111)$", message = "开票人证件类型目前仅支持身份证（111）")
    private String drawerCardType = "111";

    @Schema(description = "开票人证件号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "110101199001011234")
    @NotEmpty(message = "开票人证件号码不能为空")
    @Size(max = 30, message = "开票人证件号码长度不能超过30个字符")
    private String drawerCardNumber;

    @Schema(description = "返回页面地址前缀（工行页面跳回本平台的地址，如 https://platform.example.com）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "https://platform.example.com")
    @NotEmpty(message = "返回页面地址前缀不能为空")
    @Size(max = 150, message = "返回页面地址前缀长度不能超过150个字符（工行返回地址上限 200，平台会追加固定路径）")
    private String jumpUrlBase;

    @Schema(description = "mac地址", example = "00:00:00:00:00:00")
    @Size(max = 17, message = "mac地址长度不能超过17个字符")
    private String mac = "00:00:00:00:00:00";

    @Schema(description = "备注", example = "特殊备注信息")
    @Size(max = 140, message = "备注长度不能超过140个字符")
    private String notes;
}
