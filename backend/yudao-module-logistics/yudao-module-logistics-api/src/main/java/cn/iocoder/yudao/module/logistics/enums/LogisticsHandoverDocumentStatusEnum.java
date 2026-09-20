package cn.iocoder.yudao.module.logistics.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 交接登记的要件状态（V6 #73，ADR 0030 第 4 条）。
 *
 * <p>司机在上门现场会遇到「身份证或银行卡没带」的真实情况。按 ADR 0030：**缺要件允许先收货**——
 * 交接与收购事实照记，状态为**待补档**；付款与开票被门禁拦住，补档后放行。
 *
 * <p>「待补档」的单据**不进开票申请、不进台账口径、不计入额度**，所以这条状态必须一路从交接登记
 * 继承到 icbc 侧的交接批次与收购单上，门禁才有判据。
 */
public enum LogisticsHandoverDocumentStatusEnum {

    /** 已齐：身份证与银行卡都在，收购后可正常进入付款与开票链路 */
    COMPLETE("COMPLETE", "已齐"),
    /** 待补档：缺身份证或银行卡；事实照记，付款与开票被门禁拦住 */
    PENDING("PENDING", "待补档");

    private final String status;
    private final String name;

    LogisticsHandoverDocumentStatusEnum(String status, String name) {
        this.status = status;
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static Optional<LogisticsHandoverDocumentStatusEnum> ofStatus(String status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static String nameOf(String status) {
        return ofStatus(status).map(LogisticsHandoverDocumentStatusEnum::getName).orElse("");
    }

}
