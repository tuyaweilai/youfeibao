package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 自然人确认状态。
 *
 * <p>预下单返回自然人确认页面后，出售者在这一步确认开票信息。工行预查询 / 通知里的
 * {@code confirmStatus}：{@code 00} 未确认 / {@code 01} 自然人确认完成 / {@code 02} 全部确认完成。
 * 这是「首次开票须征得同意」这一法定义务在系统里的落点（见 CONTEXT.md「自然人确认」）。
 */
public enum InvoiceConfirmStatusEnum {

    NOT_CONFIRMED(0, "00", "未确认"),
    NATURAL_PERSON_CONFIRMED(1, "01", "自然人确认完成"),
    ALL_CONFIRMED(2, "02", "全部确认完成");

    private final Integer status;
    private final String code;
    private final String name;

    InvoiceConfirmStatusEnum(Integer status, String code, String name) {
        this.status = status;
        this.code = code;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 工行状态码转平台状态值；未识别返回 {@link #NOT_CONFIRMED}。
     */
    public static Integer toStatus(String code) {
        return ofCode(code).map(InvoiceConfirmStatusEnum::getStatus).orElse(NOT_CONFIRMED.getStatus());
    }

    public static Optional<InvoiceConfirmStatusEnum> ofCode(String code) {
        return Arrays.stream(values()).filter(item -> item.code.equals(code)).findFirst();
    }
}
