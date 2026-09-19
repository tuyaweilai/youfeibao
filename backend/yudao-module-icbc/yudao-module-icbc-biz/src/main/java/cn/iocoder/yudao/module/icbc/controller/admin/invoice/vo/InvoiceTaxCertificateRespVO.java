package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 代办税费缴税凭证（可打印）。
 *
 * <p>对应税总 5 号公告第十一条的代办税费义务：回收企业在次月申报期内为出售者代办增值税及附加税费、
 * 个人所得税的申报与缴纳。缴税成功后本凭证把「哪张票、应缴多少、实缴多少、什么时候缴、凭证序号」
 * 固化下来，供财务与税务核查对齐。
 */
@Schema(description = "管理后台 - 代办税费缴税凭证 VO")
@Data
@ExcelIgnoreUnannotated
public class InvoiceTaxCertificateRespVO {

    @Schema(description = "凭证编号", example = "TAXCERT-ACQ20261201001")
    @ExcelProperty("凭证编号")
    private String certificateNo;

    @Schema(description = "合作方订单号（收购单号）", example = "ACQ20261201001")
    @ExcelProperty("业务单号")
    private String partnerOrderId;

    @Schema(description = "开票订单号", example = "INV17645472000001234")
    @ExcelProperty("开票订单号")
    private String orderNo;

    @Schema(description = "收购单号", example = "ACQ20261201001")
    @ExcelProperty("收购单号")
    private String acquisitionNo;

    @Schema(description = "扣缴义务人（回收企业）名称", example = "北京再生资源回收有限公司")
    @ExcelProperty("扣缴义务人")
    private String payerName;

    @Schema(description = "扣缴义务人纳税人识别号", example = "91110105MA01XXXXXX")
    @ExcelProperty("纳税人识别号")
    private String payerTaxNo;

    @Schema(description = "出售者姓名", example = "张三")
    @ExcelProperty("出售者姓名")
    private String sellerName;

    @Schema(description = "出售者身份证号（脱敏）", example = "1101**********1234")
    @ExcelProperty("出售者身份证号")
    private String sellerIdCardNo;

    @Schema(description = "发票号码", example = "12345678")
    @ExcelProperty("发票号码")
    private String invoiceNo;

    @Schema(description = "发票代码", example = "144031909110")
    @ExcelProperty("发票代码")
    private String invoiceCode;

    @Schema(description = "开票日期", example = "2026-12-01 10:30:00")
    @ExcelProperty("开票日期")
    private String invoiceDate;

    @Schema(description = "发票金额（元）", example = "1000.00")
    @ExcelProperty("发票金额(元)")
    private BigDecimal invoiceAmount;

    @Schema(description = "应缴税额（元）", example = "10.00")
    @ExcelProperty("应缴税额(元)")
    private BigDecimal taxAmount;

    @Schema(description = "实缴税额（元）", example = "10.00")
    @ExcelProperty("实缴税额(元)")
    private BigDecimal taxRealAmount;

    @Schema(description = "缴税时间", example = "2026-12-01 10:35:00")
    @ExcelProperty("缴税时间")
    private String taxTime;

    @Schema(description = "税费缴纳方式", example = "企业委托扣缴")
    @ExcelProperty("缴纳方式")
    private String taxPaymentMethodName;

    @Schema(description = "应征凭证序号", example = "2026120100001234")
    @ExcelProperty("应征凭证序号")
    private String taxVoucherNo;

    @Schema(description = "缴税状态名称", example = "缴税成功")
    @ExcelProperty("缴税状态")
    private String taxStatusName;

    @Schema(description = "凭证出具时间", example = "2026-12-01 10:35:00")
    @ExcelProperty("出具时间")
    private String issuedTime;

}
