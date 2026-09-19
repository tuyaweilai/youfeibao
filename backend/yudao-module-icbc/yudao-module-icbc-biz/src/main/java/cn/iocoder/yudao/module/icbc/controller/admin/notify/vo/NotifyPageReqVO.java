package cn.iocoder.yudao.module.icbc.controller.admin.notify.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 出售者触达记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NotifyPageReqVO extends PageParam {

    @Schema(description = "触达类型：SETTLEMENT_PENDING-结算单待确认，PAYMENT_EXCEPTION-付款异常，INVOICE_ISSUED-发票已开出")
    private String bizType;

    @Schema(description = "发送状态：0-未发送（开关关闭），1-未发送（未留手机号），2-未发送（未配置入口），3-已发送，4-发送失败")
    private Integer status;

    @Schema(description = "出售者姓名")
    private String sellerName;

}
