package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 开票信息（预查询结果）
 *
 * 一张票同时挂着五条状态线：自然人确认、预开票、支付、缴税、上传；
 * 红冲时另有 {@link #redOffsetStatus}。平台按这些状态线收敛，不把它们压成一个布尔值。
 */
@Data
@Builder
public class InvoiceInfo {

    private String outOrderId;
    private String outUserId;
    private String outVendorId;
    private String outInvoiceId;
    private String outRedOffsetId;
    /**
     * 自然人确认状态：00 未确认，01 自然人确认完成，02 全部确认完成
     */
    private String confirmStatus;
    /**
     * 支付状态：见工行字典
     */
    private String payStatus;
    /**
     * 预开票状态：00 初始，01 预开票中，02 预开票成功，03 预开票失败，04 预开票取消
     */
    private String invoiceStatus;
    /**
     * 上传状态：00 未上传，01 处理中，02 已受理，03 上传中，04 上传成功，05 上传失败
     */
    private String uploadStatus;
    /**
     * 缴税状态：00 初始，…，04 缴税成功，05 缴税失败，97/98 异常，99 无需缴税
     */
    private String taxStatus;
    /**
     * 红字确认单状态：00 初始，…，10 撤销成功，11 撤销失败
     */
    private String redOffsetStatus;
    private String invoiceCode;
    /**
     * 发票号码（工行通知里 {@code invoiceCode} 是发票号码；预查询未单列，由适配层回填同一值）
     */
    private String invoiceNo;
    private String invoiceDate;
    private String taxAmount;
    /**
     * 实缴税额（工行 {@code taxRealAmount}），缴税成功后用于出具缴税凭证
     */
    private String taxRealAmount;
    /**
     * 缴税时间（工行 {@code tradeTime}），格式 yyyy-MM-dd HH:mm:ss
     */
    private String tradeTime;
    /**
     * 税费缴纳方式：0-自然人自行办理，1-企业委托扣缴
     */
    private String taxPaymentMethod;
    /**
     * 补缴税费标志
     */
    private String supplementaryTax;
    /**
     * 应征凭证序号（取征收信息明细里的 {@code voucherNum}），缴税凭证的编号来源
     */
    private String taxVoucherNo;
    private String payAmount;
    private String actuallyReceivedAmount;
    private String icbcOrderId;
    private String jOrderId;
    private String serialNo;
    /**
     * 发票明细
     */
    private List<InvoiceDetail> invoiceDetail;
    /**
     * 红冲明细
     */
    private List<RedOffsetDetail> redOffsetDetail;

    /**
     * 征收信息明细（通知类型 03 且预开票成功、以及缴税通知里可能带）
     */
    private List<LevyItem> levyItems;

    /**
     * 发票明细
     */
    @Data
    @Builder
    public static class InvoiceDetail {

        private String detailNumber;
        private String serviceName;
        private String quantity;
        private String specsModel;
        private String amount;
        private String taxAmount;
        private String taxInclusiveAmount;

    }

    /**
     * 征收信息明细
     */
    @Data
    @Builder
    public static class LevyItem {

        private String levyItemCode;
        private String levyItemName;
        private String levyGradeCode;
        private String levyGradeName;
        private String taxBasis;
        private String taxRate;
        private String taxPayable;
        /**
         * 应征凭证序号
         */
        private String voucherNum;
        private String taxStartDate;
        private String taxEndDate;

    }

    /**
     * 红冲明细
     */
    @Data
    @Builder
    public static class RedOffsetDetail {

        private String detailNumber;
        private String blueDetailNumber;
        private String projectName;
        private String quantity;
        private String pricePerUnit;
        private String redOffsetAmount;
        private String redOffsetTaxAmount;
        private String redOffsetTaxInclusiveAmount;

    }

}
