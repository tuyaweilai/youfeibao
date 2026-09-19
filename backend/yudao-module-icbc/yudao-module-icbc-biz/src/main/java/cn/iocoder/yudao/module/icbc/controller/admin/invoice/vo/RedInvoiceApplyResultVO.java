package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 红字冲销开票响应 VO
 */
@Schema(description = "管理后台 - 红字冲销开票响应 VO")
@Data
public class RedInvoiceApplyResultVO {

    @Schema(description = "是否成功", example = "true")
    private Boolean success;

    @Schema(description = "是否重复发起（同一蓝票已有生效中的红冲）", example = "false")
    private Boolean duplicate;

    @Schema(description = "红冲流水号", example = "RED202312010001")
    private String redOffsetNo;

    @Schema(description = "原蓝字合作方订单ID", example = "ACQ202312010001")
    private String partnerOrderId;

    @Schema(description = "红冲原因", example = "01")
    private String reason;

    @Schema(description = "红冲金额", example = "1000.00")
    private BigDecimal amount;

    @Schema(description = "红冲状态", example = "1")
    private Integer redOffsetStatus;

    @Schema(description = "红冲状态名称", example = "申请中")
    private String redOffsetStatusName;

    @Schema(description = "下一步动作")
    private String nextAction;

    @Schema(description = "工行红字确认单页面（自动提交表单 HTML）")
    private String confirmPageHtml;

    @Schema(description = "结果说明", example = "已取得红字确认单页面")
    private String message;
}
