package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 交接批次的来源方式（#50 T12）。
 *
 * <p>它回答「货是怎么来的」，与「有没有预约、有没有采购订单」是两件事（ADR 0020）：
 * 预约只是他事先说了一句要来，采购订单只是内部执行依据，两者都**可空**，
 * 不构成建批次的必要条件。
 */
public enum HandoverSourceTypeEnum {

    /** 预约到站：他事先在自然人端声明过要来的时间与场站 */
    APPOINTMENT("APPOINTMENT", "预约到站"),
    /** 直接到场：临时上门，没有预约也没有采购订单也能收 */
    WALK_IN("WALK_IN", "直接到场"),
    /** 上门回收：企业到对方所在地收货（此时场地由上门地址描述） */
    ON_SITE("ON_SITE", "上门回收");

    private final String type;
    private final String name;

    HandoverSourceTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static Optional<HandoverSourceTypeEnum> ofType(String type) {
        return Arrays.stream(values()).filter(item -> item.type.equals(type)).findFirst();
    }

}
