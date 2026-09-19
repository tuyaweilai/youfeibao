package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 预约到站状态（ADR 0020）。
 *
 * <p>预约**不是订单**：没有「企业接受 / 拒绝」，只有「到场」与「未到场」。
 * 它不占额度、不产生开票、不进五流，也不参与任何统计口径。
 */
public enum AppointmentStatusEnum {

    /** 待到站：预约已提交，等待他到场 */
    PENDING(0, "待到站"),
    /** 已到场：收货员在现场确认人来了（通常随后就建收购单） */
    ARRIVED(1, "已到场"),
    /** 未到场：过了预计时间人没来，由收货员标记 */
    NO_SHOW(2, "未到场"),
    /** 已取消：出售者本人取消 */
    CANCELLED(9, "已取消");

    private final Integer status;
    private final String name;

    AppointmentStatusEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static Optional<AppointmentStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

}
