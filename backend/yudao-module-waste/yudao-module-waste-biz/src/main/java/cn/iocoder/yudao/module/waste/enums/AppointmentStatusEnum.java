package cn.iocoder.yudao.module.waste.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;

import java.util.Arrays;

/**
 * 预约状态枚举
 *
 * @author 芋道源码
 */
public enum AppointmentStatusEnum implements IntArrayValuable {

    PENDING(0, "待处理"),
    WAITING_RECYCLER_CONFIRM(1, "待回收方确认"),
    CONFIRMED(2, "已确认"),
    REJECTED(3, "已拒绝"),
    ORDER_GENERATED(4, "已生成订单"),
    CANCELLED(5, "已取消");

    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(AppointmentStatusEnum::getStatus).toArray();

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    AppointmentStatusEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }

    @Override
    public int[] array() {
        return ARRAY;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static AppointmentStatusEnum valueOf(Integer status) {
        return Arrays.stream(values())
                .filter(statusEnum -> statusEnum.getStatus().equals(status))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING || this == WAITING_RECYCLER_CONFIRM;
    }

    /**
     * 判断是否可以确认
     */
    public boolean canConfirm() {
        return this == WAITING_RECYCLER_CONFIRM;
    }

    /**
     * 判断是否可以拒绝
     */
    public boolean canReject() {
        return this == WAITING_RECYCLER_CONFIRM;
    }

    /**
     * 判断是否可以生成订单
     */
    public boolean canGenerateOrder() {
        return this == CONFIRMED;
    }

} 