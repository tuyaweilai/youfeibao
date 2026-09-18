package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工行反向开票预下单响应 VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 工行反向开票预下单响应 VO")
@Data
public class InvoicePreOrderRespVO {

    @Schema(description = "返回码", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer returnCode;

    @Schema(description = "返回码说明", requiredMode = Schema.RequiredMode.REQUIRED, example = "成功")
    private String returnMsg;

    @Schema(description = "工行页面跳转URL", example = "https://gw.open.icbc.com.cn/ui/jft/ui/invoice/pre/order/V1?...")
    private String redirectUrl;

    @Schema(description = "订单号（我方生成）", example = "ORD202312010001")
    private String orderNo;

    @Schema(description = "合作方订单ID", example = "2018040908")
    private String partnerOrderId;

} 