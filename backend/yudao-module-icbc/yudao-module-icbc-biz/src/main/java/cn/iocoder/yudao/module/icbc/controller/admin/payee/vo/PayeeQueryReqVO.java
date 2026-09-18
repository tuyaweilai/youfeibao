package cn.iocoder.yudao.module.icbc.controller.admin.payee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "工行收方查询接口 Request VO")
@Data
public class PayeeQueryReqVO {

    @Schema(description = "收方账号", example = "6222021234567890123")
    private String receiverAccount;

    @Schema(description = "证件号码", example = "110101199001011234")
    private String idNo;

} 