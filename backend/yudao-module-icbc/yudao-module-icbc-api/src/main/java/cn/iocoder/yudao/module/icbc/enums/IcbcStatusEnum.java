package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工商银行反向开票模块状态枚举
 */
public class IcbcStatusEnum {

    /**
     * 审核状态枚举
     */
    @Getter
    @AllArgsConstructor
    public enum AuditStatus {
        PENDING(0, "待审核"),
        APPROVED(1, "审核通过"),
        REJECTED(2, "审核拒绝");

        private final Integer status;
        private final String name;
    }

    /**
     * 订单状态枚举
     */
    @Getter
    @AllArgsConstructor
    public enum OrderStatus {
        PENDING_CONFIRM(0, "待确认"),
        CONFIRMED(1, "已确认"),
        PAID(2, "已支付"),
        INVOICED(3, "已开票"),
        COMPLETED(4, "已完成"),
        CANCELLED(9, "已取消");

        private final Integer status;
        private final String name;
    }

    /**
     * 开票状态枚举
     */
    @Getter
    @AllArgsConstructor
    public enum InvoiceStatus {
        NOT_INVOICED(0, "未开票"),
        INVOICING(1, "开票中"),
        INVOICED(2, "开票成功"),
        INVOICE_FAILED(3, "开票失败");

        private final Integer status;
        private final String name;
    }

    /**
     * 支付状态枚举
     */
    @Getter
    @AllArgsConstructor
    public enum PaymentStatus {
        NOT_PAID(0, "未支付"),
        PAYING(1, "支付中"),
        PAID(2, "支付成功"),
        PAY_FAILED(3, "支付失败");

        private final Integer status;
        private final String name;
    }

    /**
     * 缴税状态枚举
     */
    @Getter
    @AllArgsConstructor
    public enum TaxStatus {
        NOT_TAXED(0, "未缴税"),
        TAXING(1, "缴税中"),
        TAXED(2, "缴税成功"),
        TAX_FAILED(3, "缴税失败");

        private final Integer status;
        private final String name;
    }

    /**
     * 发票类型枚举
     */
    @Getter
    @AllArgsConstructor
    public enum InvoiceType {
        ORDINARY(1, "增值税普通发票"),
        SPECIAL(2, "增值税专用发票");

        private final Integer type;
        private final String name;
    }

    /**
     * 业务类型枚举
     */
    @Getter
    @AllArgsConstructor
    public enum BusinessType {
        AGRICULTURAL("AGRICULTURAL", "农产品收购"),
        SCRAP("SCRAP", "报废产品收购"),
        RECYCLE("RECYCLE", "再生资源");

        private final String type;
        private final String name;
    }

    /**
     * 红冲状态枚举
     */
    @Getter
    @AllArgsConstructor
    public enum RedStatus {
        APPLYING(0, "申请中"),
        SUCCESS(1, "红冲成功"),
        FAILED(2, "红冲失败");

        private final Integer status;
        private final String name;
    }

    /**
     * 回调通知类型枚举
     */
    @Getter
    @AllArgsConstructor
    public enum NotifyType {
        PAYEE_AUDIT("PAYEE_AUDIT", "收方审核"),
        INVOICE_STATUS("INVOICE_STATUS", "发票状态"),
        PAYMENT_RESULT("PAYMENT_RESULT", "支付结果");

        private final String type;
        private final String name;
    }

    /**
     * 处理状态枚举
     */
    @Getter
    @AllArgsConstructor
    public enum ProcessStatus {
        PENDING(0, "待处理"),
        SUCCESS(1, "处理成功"),
        FAILED(2, "处理失败");

        private final Integer status;
        private final String name;
    }
} 