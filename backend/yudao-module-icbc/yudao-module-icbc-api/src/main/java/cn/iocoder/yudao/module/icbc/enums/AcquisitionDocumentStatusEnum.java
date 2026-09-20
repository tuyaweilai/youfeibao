package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 收购单的要件状态（V6 #73，ADR 0030 第 4 条）。
 *
 * <p>上门提货时司机可能遇到「对方没带身份证」或「没带银行卡」。按 ADR 0030：**缺要件允许先收货**，
 * 交接与收购事实照记，状态为**待补档**；付款与开票被门禁拦住，补档后放行。
 *
 * <p>「待补档」的收购单**不进开票申请、不进台账口径、不计入额度**——额度台账派生自票据事实，
 * 开票申请被门禁挡住，自然也就不会占额度。补档动作（{@code complete-documents}）留办理人与时间。
 *
 * <p>历史数据（字段为空）视为**已齐**：不因新增字段把老单据变成走不通的。
 */
public enum AcquisitionDocumentStatusEnum {

    /** 已齐：身份证与银行卡都在，可以正常进入付款与开票链路 */
    COMPLETE("COMPLETE", "已齐"),
    /** 待补档：缺身份证或银行卡；事实照记，付款与开票被门禁拦住 */
    PENDING("PENDING", "待补档");

    private final String status;
    private final String name;

    AcquisitionDocumentStatusEnum(String status, String name) {
        this.status = status;
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static Optional<AcquisitionDocumentStatusEnum> ofStatus(String status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    /**
     * 状态名；为空（历史数据）时按「已齐」显示——不把老单据标成待补档。
     */
    public static String nameOf(String status) {
        if (status == null) {
            return COMPLETE.getName();
        }
        return ofStatus(status).map(AcquisitionDocumentStatusEnum::getName).orElse("");
    }

}
