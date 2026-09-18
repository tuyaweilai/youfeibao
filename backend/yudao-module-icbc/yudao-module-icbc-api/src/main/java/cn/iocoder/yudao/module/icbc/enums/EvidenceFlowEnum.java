package cn.iocoder.yudao.module.icbc.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 五流合一中的「流」。内容由税总 5 号公告第十七条定义，五类的齐备程度是本平台的
 * 交付指标（见 CONTEXT「五流合一」）。顺序即展示与齐备率统计的顺序。
 */
public enum EvidenceFlowEnum {

    /** 合同流：收购合同或协议（框架收购协议 + 该笔收购单） */
    CONTRACT("CONTRACT", "合同流"),
    /** 货物流：货物过磅单，以及运输发票或凭证 */
    GOODS("GOODS", "货物流"),
    /** 资金流：向出售者支付货款的转账支付记录 */
    CAPITAL("CAPITAL", "资金流"),
    /** 发票流：反向开具的发票，及其红冲与下载 */
    INVOICE("INVOICE", "发票流"),
    /** 信息流：收购台账 */
    INFO("INFO", "信息流");

    private final String code;
    private final String name;

    EvidenceFlowEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    /**
     * 五流的固定顺序。齐备率与证据链展示都按这个顺序，保证同一张票每次展开一致。
     */
    public static List<EvidenceFlowEnum> ordered() {
        return Arrays.asList(values());
    }

    public static Optional<EvidenceFlowEnum> ofCode(String code) {
        return Arrays.stream(values())
                .filter(flow -> flow.code.equals(code))
                .findFirst();
    }

}
