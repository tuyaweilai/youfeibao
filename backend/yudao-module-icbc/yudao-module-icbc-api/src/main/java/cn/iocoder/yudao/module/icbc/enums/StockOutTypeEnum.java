package cn.iocoder.yudao.module.icbc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * 非销售出库类型（#54 T16，ADR 0025）。
 *
 * <p>三种都不挂客户：报损（货真的没了）、退货出库（退回给交易对方）、内部领用（本企业内部消耗 / 转移）。
 * 销售出库不在一期范围内（ADR 0004 不做交易撮合、正向开票不做）。
 *
 * <p>本枚举只表达业务语义；到 ERP 库存流水业务类型（{@code ErpStockRecordBizTypeEnum}）的映射在
 * {@code icbc-biz} 侧——{@code icbc-api} 不依赖 {@code erp-api}。
 */
@Getter
@AllArgsConstructor
public enum StockOutTypeEnum {

    SCRAP(10, "报损"),
    RETURN(20, "退货出库"),
    INTERNAL_USE(30, "内部领用"),
    ;

    private final Integer type;
    private final String name;

    public static Optional<StockOutTypeEnum> ofType(Integer type) {
        return Arrays.stream(values())
                .filter(item -> Objects.equals(item.type, type))
                .findFirst();
    }

    public static String nameOf(Integer type) {
        return ofType(type).map(StockOutTypeEnum::getName).orElse("");
    }

}
