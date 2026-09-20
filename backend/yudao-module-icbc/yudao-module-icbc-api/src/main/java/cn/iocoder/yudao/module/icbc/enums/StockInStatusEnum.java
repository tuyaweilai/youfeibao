package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * 入库单状态（#52 T14）。
 *
 * <p>本枚举是「只有过账的入库才增加正式库存」这条不变量（规格 #38 实现决策第 9 条）的落点：
 * 入库单先落为 {@link #PENDING}（待过账），此时只是把人选定的仓库 / 库位 / 批次与实际入库量记下来，
 * **不动库存**；过账（{@link #POSTED}）时才经 {@code StockApi} 写库存流水并增量余额。
 * 作废（{@link #CANCELLED}）已过账的单据时按相反方向冲销，已过账但未作废的才计入「累计入库」。
 */
public enum StockInStatusEnum {

    /** 待过账：单据已建，尚未写库存 */
    PENDING(0, "待过账"),
    /** 已过账：已写库存流水并增量余额 */
    POSTED(1, "已过账"),
    /** 已作废：不再计入累计入库；已过账的先冲销库存 */
    CANCELLED(2, "已作废");

    private final Integer status;
    private final String name;

    StockInStatusEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public static Optional<StockInStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values()).filter(item -> item.status.equals(status)).findFirst();
    }

}
