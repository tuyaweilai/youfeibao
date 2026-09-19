package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 红字发票查询响应 VO
 */
@Schema(description = "管理后台 - 红字发票查询响应 VO")
@Data
public class RedInvoiceQueryRespVO {

    @Schema(description = "红冲流水号", example = "RED202312010001")
    private String redOffsetNo;

    @Schema(description = "原蓝字合作方订单ID", example = "ACQ202312010001")
    private String partnerOrderId;

    @Schema(description = "蓝票开票订单编号", example = "1024")
    private Long invoiceOrderId;

    @Schema(description = "来源收购单编号", example = "2048")
    private Long acquisitionId;

    @Schema(description = "红冲原因", example = "01")
    private String reason;

    @Schema(description = "红冲原因名称", example = "开票有误")
    private String reasonName;

    @Schema(description = "红冲金额", example = "1000.00")
    private BigDecimal amount;

    @Schema(description = "红冲税额", example = "10.00")
    private BigDecimal taxAmount;

    @Schema(description = "红冲状态", example = "7")
    private Integer redOffsetStatus;

    @Schema(description = "红冲状态名称", example = "红冲成功")
    private String redOffsetStatusName;

    @Schema(description = "工行原始红字确认单状态码", example = "07")
    private String redOffsetStatusCode;

    @Schema(description = "红冲发票号", example = "87654321")
    private String redInvoiceNo;

    @Schema(description = "红票开具日期", example = "2023-12-02 14:30:00")
    private LocalDateTime redInvoiceDate;

    @Schema(description = "撤销结果（10 撤销成功 / 11 撤销失败）", example = "10")
    private String revokeStatus;

    @Schema(description = "撤销时间")
    private LocalDateTime revokeTime;

    @Schema(description = "下一步动作")
    private String nextAction;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
