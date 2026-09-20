package cn.iocoder.yudao.module.icbc.enums;

import java.util.Optional;

/**
 * 关联单据追溯里每个阶段的状态（#55 T17）。
 *
 * <p>五个取值把「缺」与「坏」分开：{@link #NOT_STARTED} 是还没开始，
 * {@link #NOT_APPLICABLE} 是这一环本来就不需要（如「直接收购」没有采购订单），
 * {@link #EXCEPTION} 才是真出了问题。前端据此用不同颜色区分，不再靠有没有编号去猜。
 */
public enum TraceStageStatusEnum {

    NOT_STARTED(0, "未开始"),
    IN_PROGRESS(1, "处理中"),
    COMPLETED(2, "已完成"),
    EXCEPTION(3, "异常"),
    NOT_APPLICABLE(4, "无需该环节");

    private final Integer status;
    private final String name;

    TraceStageStatusEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static Optional<TraceStageStatusEnum> ofStatus(Integer status) {
        return java.util.Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(TraceStageStatusEnum::getName).orElse(null);
    }

}
