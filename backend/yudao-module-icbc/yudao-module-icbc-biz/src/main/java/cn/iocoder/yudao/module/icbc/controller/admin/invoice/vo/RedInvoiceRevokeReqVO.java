package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

/**
 * 红字冲销确认单撤销请求 VO
 */
@Schema(description = "管理后台 - 红字冲销确认单撤销请求 VO")
@Data
public class RedInvoiceRevokeReqVO {

    @Schema(description = "红冲流水号", requiredMode = Schema.RequiredMode.REQUIRED, example = "RED202312010001")
    @NotEmpty(message = "红冲流水号不能为空")
    @Size(max = 35, message = "红冲流水号长度不能超过35个字符")
    private String redOffsetNo;
}
