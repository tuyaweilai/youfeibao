package cn.iocoder.yudao.module.icbc.controller.admin.trace.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 关联单据查询导出的一行（#55 T17，AC6）。
 *
 * <p>导出与在线查看走同一套脱敏判断：没有 {@code icbc:trace:sensitive:view} 时，
 * 身份证 / 手机号 / 银行卡列是脱敏值；导出动作本身另记一条操作日志。
 */
@Schema(description = "管理后台 - 关联单据查询导出行")
@Data
@ExcelIgnoreUnannotated
public class TraceExportRespVO {

    @Schema(description = "收购单号")
    @ExcelProperty("收购单号")
    private String acquisitionNo;

    @Schema(description = "交接批次号")
    @ExcelProperty("交接批次号")
    private String handoverBatchNo;

    @Schema(description = "交易时间")
    @ExcelProperty("交易时间")
    private LocalDateTime tradeTime;

    @Schema(description = "出售者姓名")
    @ExcelProperty("出售者姓名")
    private String sellerName;

    @Schema(description = "出售者身份证件号码（按岗位权限脱敏）")
    @ExcelProperty("身份证件号码")
    private String sellerIdCard;

    @Schema(description = "出售者手机号（按岗位权限脱敏）")
    @ExcelProperty("手机号")
    private String sellerMobile;

    @Schema(description = "出售者银行卡（按岗位权限脱敏，默认只给尾号）")
    @ExcelProperty("银行卡")
    private String sellerBankCard;

    @Schema(description = "车牌号")
    @ExcelProperty("车牌号")
    private String plateNo;

    @Schema(description = "品类")
    @ExcelProperty("品类")
    private String categoryName;

    @Schema(description = "计量单位")
    @ExcelProperty("计量单位")
    private String unit;

    @Schema(description = "实物量（接收量优先，无则净重）")
    @ExcelProperty("实物量")
    private BigDecimal physicalWeight;

    @Schema(description = "结算重量（唯一计价基准）")
    @ExcelProperty("结算重量")
    private BigDecimal settlementWeight;

    @Schema(description = "已过账入库量")
    @ExcelProperty("已入库量")
    private BigDecimal stockedWeight;

    @Schema(description = "入库量与结算量的差额")
    @ExcelProperty("入库-结算差额")
    private BigDecimal stockInSettlementDiff;

    @Schema(description = "采购安排：采购订单号，未关联时为「直接收购」")
    @ExcelProperty("采购安排")
    private String purchaseArrangement;

    @Schema(description = "结算单号")
    @ExcelProperty("结算单号")
    private String settlementNo;

    @Schema(description = "结算确认状态")
    @ExcelProperty("结算确认状态")
    private String settlementStatusName;

    @Schema(description = "付款状态")
    @ExcelProperty("付款状态")
    private String paymentStatusName;

    @Schema(description = "发票号码")
    @ExcelProperty("发票号码")
    private String invoiceNo;

    @Schema(description = "差异 / 缺失关联说明")
    @ExcelProperty("差异说明")
    private String differenceNote;

}
