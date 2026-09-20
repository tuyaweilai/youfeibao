package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * 库存作业单据状态（#54 T16）。
 *
 * <p>非销售出库 / 跨仓调拨 / 盘点调整 / 期初导入四类单据共用一套状态：先登记（待过账，不动库存），
 * 过账才写库存流水；作废已过账的单会按相反方向冲销。与 {@link StockInStatusEnum} 同形，
 * 但归属不同（入库是收购单派生的单向动作，本枚举是仓管自主发起的库存作业）。
 */
@Getter
@AllArgsConstructor
public enum StockOpsStatusEnum {

    PENDING(0, "待过账"),
    POSTED(1, "已过账"),
    CANCELLED(2, "已作废"),
    ;

    private final Integer status;
    private final String name;

    public static Optional<StockOpsStatusEnum> ofStatus(Integer status) {
        return Arrays.stream(values())
                .filter(item -> Objects.equals(item.status, status))
                .findFirst();
    }

    public static String nameOf(Integer status) {
        return ofStatus(status).map(StockOpsStatusEnum::getName).orElse("");
    }

}
