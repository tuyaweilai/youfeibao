package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 出售者触达记录的发送状态（#36）。
 *
 * <p>「没发出去」也要落库并说明原因，这样运营能看到是开关关着、没留手机号、还是通道拒绝，
 * 而不是一片空白。落库同时充当**幂等键**：同一业务事件只发一次，不重复轰炸。
 */
public enum SellerNotifyStatusEnum {

    /** 未发送：短信开关（平台或租户）关闭 */
    SKIPPED_DISABLED(0, "未发送（开关关闭）"),
    /** 未发送：出售者没有留手机号 */
    SKIPPED_NO_MOBILE(1, "未发送（未留手机号）"),
    /** 未发送：没有可用的自然人端入口地址，拼不出链接 */
    SKIPPED_NO_LINK(2, "未发送（未配置自然人端入口）"),
    /** 已发送：已交给短信通道 */
    SENT(3, "已发送"),
    /** 发送失败：通道 / 模板异常，原因见 errorMsg */
    FAILED(4, "发送失败");

    private final Integer status;
    private final String name;

    SellerNotifyStatusEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(SellerNotifyStatusEnum::getName).orElse("未知");
    }

    public static Optional<SellerNotifyStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

}
