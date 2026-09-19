package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工行反向开票预查询响应 VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 工行反向开票预查询响应 VO")
@Data
public class InvoiceQueryRespVO {

    @Schema(description = "返回码", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer returnCode;

    @Schema(description = "返回码说明", requiredMode = Schema.RequiredMode.REQUIRED, example = "成功")
    private String returnMsg;

    @Schema(description = "订单号（我方生成）", example = "ORD202312010001")
    private String orderNo;

    @Schema(description = "合作方订单ID", example = "2018040908")
    private String partnerOrderId;

    @Schema(description = "来源收购单编号", example = "1024")
    private Long acquisitionId;

    @Schema(description = "订单状态：0-待确认，1-已确认，2-已支付，3-已开票，4-已完成，9-已取消", example = "2")
    private Integer orderStatus;

    @Schema(description = "开票状态：0-未开票，1-开票中，2-已开票，3-开票失败", example = "2")
    private Integer invoiceStatus;

    @Schema(description = "开票状态名称", example = "已开票")
    private String invoiceStatusName;

    @Schema(description = "支付状态：0-未支付，1-支付中，2-支付成功，3-支付失败", example = "2")
    private Integer paymentStatus;

    @Schema(description = "缴税状态：0-未缴税，1-缴税中，2-缴税成功，3-缴税失败，4-金额不一致，5-未知异常，6-无需缴税", example = "2")
    private Integer taxStatus;

    @Schema(description = "缴税状态名称", example = "缴税成功")
    private String taxStatusName;

    @Schema(description = "发票上传状态：0-未上传，1-处理中，2-已受理，3-上传中，4-上传成功，5-上传失败", example = "4")
    private Integer uploadStatus;

    @Schema(description = "发票上传状态名称", example = "上传成功")
    private String uploadStatusName;

    @Schema(description = "自然人确认状态：0-未确认，1-自然人确认完成，2-全部确认完成", example = "1")
    private Integer confirmStatus;

    @Schema(description = "预开票状态：0-初始，1-预开票中，2-预开票成功，3-预开票失败，4-预开票取消", example = "2")
    private Integer preInvoiceStatus;

    @Schema(description = "发票号码", example = "12345678")
    private String invoiceNo;

    @Schema(description = "发票代码", example = "144031909110")
    private String invoiceCode;

    @Schema(description = "开票日期", example = "2023-12-01 10:30:00")
    private LocalDateTime invoiceDate;

    @Schema(description = "发票金额", example = "1000.00")
    private BigDecimal invoiceAmount;

    @Schema(description = "税额", example = "130.00")
    private BigDecimal taxAmount;

    @Schema(description = "实缴税额", example = "130.00")
    private BigDecimal taxRealAmount;

    @Schema(description = "缴税时间", example = "2023-12-01 10:35:00")
    private LocalDateTime taxTime;

    @Schema(description = "税费缴纳方式：0-自然人自行办理，1-企业委托扣缴", example = "1")
    private String taxPaymentMethod;

    @Schema(description = "税费缴纳方式名称", example = "企业委托扣缴")
    private String taxPaymentMethodName;

    @Schema(description = "应征凭证序号（缴税凭证编号）", example = "2026120100001234")
    private String taxVoucherNo;

    @Schema(description = "失败 / 异常 / 进行中时给用户的下一步动作；全部正常终态时为空",
            example = "缴税失败，请财务在「查状态」确认后重新发起缴税")
    private String nextAction;

    @Schema(description = "红冲流水号", example = "RED202312010001")
    private String redSerialNo;

    @Schema(description = "红票发票号码", example = "87654321")
    private String redInvoiceNo;

    @Schema(description = "红票开票日期", example = "2023-12-02 14:30:00")
    private LocalDateTime redInvoiceDate;

} 